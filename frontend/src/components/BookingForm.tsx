import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';

// --- Icons (SVG) ---
const SearchIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
  </svg>
);
const MapPinIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
  </svg>
);
const CalendarIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
  </svg>
);
const BusIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
  </svg>
);

interface Trip {
  id: number;
  origin: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
  price: number;
  busName: string;
  departureDate: string;
}

interface BookingRequest {
  tripId: number;
  userId: number;
  seats: string[];
  busId: number;
  stationId: number;
}

// Use full URL to avoid proxy issues
const API_BASE_URL = 'http://localhost:8080';

const BookingForm: React.FC = () => {
  const [step, setStep] = useState(1);
  const [searchParams, setSearchParams] = useState({
    origin: '',
    destination: '',
    date: ''
  });
  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [bookedSeats, setBookedSeats] = useState<string[]>([]);
  const [selectedSeats, setSelectedSeats] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [bookingId, setBookingId] = useState<number | null>(null); // Store booking ID for payment
  
  const selectedSeatsRef = useRef(selectedSeats);
  const selectedTripRef = useRef(selectedTrip);
  
  useEffect(() => {
      selectedSeatsRef.current = selectedSeats;
  }, [selectedSeats]);

  useEffect(() => {
      selectedTripRef.current = selectedTrip;
  }, [selectedTrip]);

  const getUserId = () => {
      const storedUserId = localStorage.getItem('userId');
      if (storedUserId) {
          return Number(storedUserId);
      }
      let uid = sessionStorage.getItem('temp_user_id');
      if (!uid) {
          uid = Math.floor(Math.random() * 10000).toString();
          sessionStorage.setItem('temp_user_id', uid);
      }
      return Number(uid);
  };
  
  const userId = getUserId();

  useEffect(() => {
    const handleBeforeUnload = () => {
      const seatsToRelease = selectedSeatsRef.current;
      const trip = selectedTripRef.current;

      if (seatsToRelease.length > 0 && trip) {
        const payload = {
          tripId: trip.id,
          seatNumbers: seatsToRelease,
          userId: userId
        };
        
        if ('keepalive' in new Request('')) {
          fetch(`${API_BASE_URL}/booking/release-batch`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
            keepalive: true,
          });
        } else {
          const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
          navigator.sendBeacon(`${API_BASE_URL}/booking/release-batch`, blob);
        }
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [userId]);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchParams({ ...searchParams, [e.target.name]: e.target.value });
  };

  const searchTrips = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/trips/search`, {
        params: {
          origin: searchParams.origin,
          destination: searchParams.destination,
          date: searchParams.date
        }
      });
      setTrips(response.data);
      if (response.data.length === 0) {
        toast.info("Không tìm thấy chuyến đi nào phù hợp.");
      } else {
        setStep(2);
      }
    } catch (error) {
      console.error(error);
      toast.error("Lỗi khi tìm kiếm chuyến đi.");
    } finally {
      setLoading(false);
    }
  };

  const selectTrip = async (trip: Trip) => {
    setSelectedTrip(trip);
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/booking/trip/${trip.id}/seats`);
      setBookedSeats(response.data || []);
      setStep(3);
      
      const interval = setInterval(async () => {
          if (step !== 3) clearInterval(interval);
          try {
              const res = await axios.get(`${API_BASE_URL}/booking/trip/${trip.id}/seats`);
              const serverBooked = res.data || [];
              const othersBooked = serverBooked.filter((s: string) => !selectedSeatsRef.current.includes(s));
              setBookedSeats(othersBooked);
          } catch (e) {}
      }, 5000);
      
      return () => clearInterval(interval);
      
    } catch (error) {
      console.error(error);
      toast.error("Không thể tải thông tin ghế.");
      setBookedSeats([]); 
      setStep(3); 
    } finally {
      setLoading(false);
    }
  };

  const toggleSeat = async (seat: string) => {
    if (bookedSeats.includes(seat)) return;

    if (selectedSeats.includes(seat)) {
      try {
          await axios.post(`${API_BASE_URL}/booking/release`, {
              tripId: selectedTrip?.id,
              seatNumber: seat,
              userId: userId
          });
          setSelectedSeats(selectedSeats.filter(s => s !== seat));
      } catch (error) {
          console.error("Failed to release seat", error);
      }
    } else {
      if (selectedSeats.length >= 5) {
        toast.warning("Bạn chỉ được chọn tối đa 5 ghế.");
        return;
      }
      
      try {
          await axios.post(`${API_BASE_URL}/booking/hold`, {
              tripId: selectedTrip?.id,
              seatNumber: seat,
              userId: userId
          });
          setSelectedSeats([...selectedSeats, seat]);
      } catch (error: any) {
          toast.error(error.response?.data || "Ghế này vừa có người chọn!");
          if (selectedTrip) {
              const res = await axios.get(`${API_BASE_URL}/booking/trip/${selectedTrip.id}/seats`);
              setBookedSeats(res.data || []);
          }
      }
    }
  };

  const confirmBooking = async () => {
    if (!selectedTrip) return;
    setLoading(true);
    
    const storedUserId = localStorage.getItem('userId');
    if (!storedUserId) {
        toast.error("Vui lòng đăng nhập để đặt vé!");
        setLoading(false);
        return;
    }

    const bookingRequest: BookingRequest = {
      tripId: selectedTrip.id,
      userId: Number(storedUserId),
      seats: selectedSeats,
      busId: 1, 
      stationId: 1 
    };

    try {
      const response = await axios.post(`${API_BASE_URL}/booking`, bookingRequest);
      toast.success(`Đặt vé thành công! Mã vé: ${response.data.id}`);
      setBookingId(response.data.id); // Save booking ID
      setStep(4); // Move to Payment Step
    } catch (error: any) {
      console.error(error);
      toast.error(error.response?.data?.message || "Đặt vé thất bại.");
    } finally {
      setLoading(false);
    }
  };

  const handlePayment = async () => {
      if (!selectedTrip || !bookingId) return;
      setLoading(true);
      try {
          const amount = selectedSeats.length * selectedTrip.price;
          const response = await axios.post(`${API_BASE_URL}/payment/vn-pay`, {
              amount: amount,
              bookingId: bookingId
          });

          if (response.data.code === "00" || response.data.code === "ok") {
              const url = response.data.data || response.data.paymentUrl;
              if (url) {
                  window.location.href = url;
              } else {
                  toast.error("Không tìm thấy link thanh toán.");
              }
          } else {
              toast.error("Lỗi tạo giao dịch thanh toán.");
          }
      } catch (error) {
          console.error(error);
          toast.error("Không thể kết nối cổng thanh toán.");
      } finally {
          setLoading(false);
      }
  };

  const renderSeatGrid = () => {
    const rows = ['A', 'B', 'C', 'D', 'E'];
    const leftCol = [1, 2];
    const rightCol = [3, 4];

    return (
      <div className="bg-white p-6 rounded-xl shadow-inner border border-gray-200 max-w-md mx-auto">
        <div className="w-full bg-gray-200 text-gray-500 text-center py-2 rounded mb-8 text-sm font-semibold uppercase tracking-wider">
          Khoang lái
        </div>
        <div className="flex justify-between gap-8">
          <div className="grid grid-cols-2 gap-3">
            {rows.map(row => (
              leftCol.map(col => {
                const seatNum = `${row}${col}`;
                return renderSingleSeat(seatNum);
              })
            ))}
          </div>
          
          <div className="flex items-center justify-center text-gray-300 text-xs font-vertical writing-mode-vertical">
            LỐI ĐI
          </div>

          <div className="grid grid-cols-2 gap-3">
            {rows.map(row => (
              rightCol.map(col => {
                const seatNum = `${row}${col}`;
                return renderSingleSeat(seatNum);
              })
            ))}
          </div>
        </div>
      </div>
    );
  };

  const renderSingleSeat = (seatNum: string) => {
    const isBooked = bookedSeats.includes(seatNum) && !selectedSeats.includes(seatNum);
    const isSelected = selectedSeats.includes(seatNum);
    
    return (
      <button
        key={seatNum}
        type="button"
        disabled={isBooked}
        onClick={() => toggleSeat(seatNum)}
        className={`
          w-10 h-10 rounded-lg text-xs font-bold transition-all duration-200 flex items-center justify-center shadow-sm
          ${isBooked 
            ? 'bg-gray-300 text-gray-500 cursor-not-allowed' 
            : isSelected 
              ? 'bg-orange-500 text-white transform scale-105 ring-2 ring-orange-300' 
              : 'bg-blue-50 text-blue-600 border border-blue-200 hover:bg-blue-100 hover:border-blue-300'}
        `}
      >
        {seatNum}
      </button>
    );
  };

  return (
    <div className="max-w-5xl mx-auto">
      {step === 1 && (
        <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100">
          <h2 className="text-2xl font-bold text-blue-900 mb-6 text-center">Tìm chuyến xe của bạn</h2>
          <form onSubmit={searchTrips} className="flex flex-col md:flex-row gap-4 items-end">
            <div className="flex-1 w-full">
              <label className="block text-sm font-medium text-gray-700 mb-1">Điểm đi</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <MapPinIcon />
                </div>
                <input
                  type="text"
                  name="origin"
                  placeholder="Hà Nội"
                  value={searchParams.origin}
                  onChange={handleSearchChange}
                  className="pl-10 w-full p-3 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
                  required
                />
              </div>
            </div>

            <div className="hidden md:flex items-center justify-center pb-3 text-gray-400">
              <BusIcon />
            </div>

            <div className="flex-1 w-full">
              <label className="block text-sm font-medium text-gray-700 mb-1">Điểm đến</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <MapPinIcon />
                </div>
                <input
                  type="text"
                  name="destination"
                  placeholder="Hồ Chí Minh"
                  value={searchParams.destination}
                  onChange={handleSearchChange}
                  className="pl-10 w-full p-3 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
                  required
                />
              </div>
            </div>

            <div className="w-full md:w-48">
              <label className="block text-sm font-medium text-gray-700 mb-1">Ngày đi</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <CalendarIcon />
                </div>
                <input
                  type="date"
                  name="date"
                  value={searchParams.date}
                  onChange={handleSearchChange}
                  className="pl-10 w-full p-3 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full md:w-auto bg-orange-500 text-white px-8 py-3 rounded-lg font-bold hover:bg-orange-600 transition shadow-md flex items-center justify-center gap-2"
            >
              {loading ? 'Đang tìm...' : (
                <>
                  <SearchIcon /> Tìm vé
                </>
              )}
            </button>
          </form>
        </div>
      )}

      {step === 2 && (
        <div>
          <button onClick={() => setStep(1)} className="mb-6 flex items-center text-blue-600 font-medium hover:underline">
            &larr; Quay lại tìm kiếm
          </button>
          
          <h3 className="text-xl font-bold text-gray-800 mb-4">Kết quả tìm kiếm: {trips.length} chuyến</h3>
          
          <div className="space-y-4">
            {trips.map(trip => (
              <div key={trip.id} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition duration-200 flex flex-col md:flex-row justify-between items-center gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="text-2xl font-bold text-blue-900">{trip.departureTime.substring(0, 5)}</span>
                    <div className="flex-1 h-px bg-gray-300 relative mx-2 min-w-[50px]">
                      <div className="absolute -top-1.5 right-0 w-3 h-3 bg-gray-300 rounded-full"></div>
                      <div className="absolute -top-1.5 left-0 w-3 h-3 bg-white border-2 border-gray-300 rounded-full"></div>
                    </div>
                    <span className="text-xl font-semibold text-gray-600">{trip.arrivalTime.substring(0, 5)}</span>
                  </div>
                  <div className="flex justify-between text-sm text-gray-500">
                    <span>{trip.origin}</span>
                    <span>{trip.destination}</span>
                  </div>
                  <div className="mt-3 flex items-center gap-2">
                    <span className="bg-blue-100 text-blue-700 text-xs px-2 py-1 rounded font-semibold">{trip.busName}</span>
                    <span className="bg-green-100 text-green-700 text-xs px-2 py-1 rounded font-semibold">Ghế ngồi</span>
                  </div>
                </div>
                
                <div className="text-right min-w-[150px]">
                  <div className="text-2xl font-bold text-orange-600 mb-2">
                    {trip.price.toLocaleString()}đ
                  </div>
                  <button
                    onClick={() => selectTrip(trip)}
                    className="w-full bg-blue-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-blue-700 transition shadow-sm"
                  >
                    Chọn chuyến
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {step === 3 && selectedTrip && (
        <div className="flex flex-col lg:flex-row gap-8">
          <div className="flex-1 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <button onClick={() => setStep(2)} className="mb-4 text-blue-600 font-medium hover:underline">
              &larr; Chọn chuyến khác
            </button>
            <h3 className="text-lg font-bold text-gray-800 mb-6 text-center">Sơ đồ ghế</h3>
            
            {renderSeatGrid()}

            <div className="flex justify-center gap-6 mt-8 text-sm">
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 bg-blue-50 border border-blue-200 rounded"></div>
                <span>Trống</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 bg-orange-500 rounded"></div>
                <span>Đang chọn</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 bg-gray-300 rounded"></div>
                <span>Đã đặt/Giữ</span>
              </div>
            </div>
          </div>

          <div className="w-full lg:w-96">
            <div className="bg-white p-6 rounded-xl shadow-lg border border-blue-100 sticky top-24">
              <h3 className="text-lg font-bold text-blue-900 mb-4 border-b pb-2">Thông tin đặt vé</h3>
              
              <div className="space-y-3 mb-6">
                <div className="flex justify-between">
                  <span className="text-gray-500">Tuyến đường:</span>
                  <span className="font-medium">{selectedTrip.origin} - {selectedTrip.destination}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Giờ chạy:</span>
                  <span className="font-medium">{selectedTrip.departureTime}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Số lượng ghế:</span>
                  <span className="font-medium">{selectedSeats.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Ghế đã chọn:</span>
                  <span className="font-bold text-orange-600">{selectedSeats.join(', ') || '---'}</span>
                </div>
              </div>

              <div className="border-t pt-4 mb-6">
                <div className="flex justify-between items-center">
                  <span className="text-gray-600 font-medium">Tổng cộng:</span>
                  <span className="text-2xl font-bold text-blue-600">
                    {(selectedSeats.length * selectedTrip.price).toLocaleString()}đ
                  </span>
                </div>
              </div>

              <button
                onClick={confirmBooking}
                disabled={selectedSeats.length === 0 || loading}
                className={`w-full py-3 rounded-lg font-bold text-white transition shadow-md ${
                  selectedSeats.length === 0 
                    ? 'bg-gray-300 cursor-not-allowed' 
                    : 'bg-orange-500 hover:bg-orange-600'
                }`}
              >
                {loading ? 'Đang xử lý...' : 'Xác nhận đặt vé'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Step 4: Payment */}
      {step === 4 && selectedTrip && (
        <div className="max-w-2xl mx-auto bg-white p-8 rounded-2xl shadow-lg border border-gray-100 text-center">
          <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Đặt vé thành công!</h2>
          <p className="text-gray-600 mb-8">Mã đơn hàng: <span className="font-bold text-blue-600">#{bookingId}</span></p>
          
          <div className="bg-gray-50 p-6 rounded-xl mb-8 text-left">
            <h3 className="font-bold text-gray-800 mb-4 border-b pb-2">Chi tiết thanh toán</h3>
            <div className="flex justify-between mb-2">
              <span className="text-gray-600">Tuyến xe:</span>
              <span className="font-medium">{selectedTrip.origin} - {selectedTrip.destination}</span>
            </div>
            <div className="flex justify-between mb-2">
              <span className="text-gray-600">Ghế đã chọn:</span>
              <span className="font-medium">{selectedSeats.join(', ')}</span>
            </div>
            <div className="flex justify-between pt-2 border-t mt-2">
              <span className="font-bold text-gray-800">Tổng tiền:</span>
              <span className="font-bold text-orange-600 text-xl">
                {(selectedSeats.length * selectedTrip.price).toLocaleString()}đ
              </span>
            </div>
          </div>

          <div className="space-y-3">
            <button
              onClick={handlePayment}
              disabled={loading}
              className="w-full bg-blue-600 text-white py-3 rounded-lg font-bold hover:bg-blue-700 transition shadow-md flex items-center justify-center gap-2"
            >
              {loading ? 'Đang chuyển hướng...' : 'Thanh toán ngay qua VNPay'}
            </button>
            
            <button
              onClick={() => {
                  setStep(1);
                  setSearchParams({ origin: '', destination: '', date: '' });
                  setSelectedSeats([]);
                  setSelectedTrip(null);
              }}
              className="w-full bg-white text-gray-600 py-3 rounded-lg font-medium border border-gray-300 hover:bg-gray-50 transition"
            >
              Về trang chủ
            </button>
          </div>
          
          <p className="text-xs text-gray-500 mt-6">
            Lưu ý: Vé sẽ tự động hủy nếu không thanh toán trong vòng 15 phút.
          </p>
        </div>
      )}
    </div>
  );
};

export default BookingForm;
