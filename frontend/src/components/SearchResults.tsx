import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Header, Footer } from './HomePage';

// Use full URL to avoid proxy issues
const API_BASE_URL = 'http://localhost:8080';

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

const SearchResults: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  const [step, setStep] = useState(2); // Start at Step 2 (List)
  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [bookedSeats, setBookedSeats] = useState<string[]>([]);
  const [selectedSeats, setSelectedSeats] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [bookingId, setBookingId] = useState<number | null>(null);

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
    if (storedUserId) return Number(storedUserId);
    let uid = sessionStorage.getItem('temp_user_id');
    if (!uid) {
      uid = Math.floor(Math.random() * 10000).toString();
      sessionStorage.setItem('temp_user_id', uid);
    }
    return Number(uid);
  };
  const userId = getUserId();

  // Fetch trips on mount based on URL params
  useEffect(() => {
    const fetchTrips = async () => {
      const origin = searchParams.get('origin');
      const destination = searchParams.get('destination');
      const date = searchParams.get('date');

      if (origin && destination && date) {
        setLoading(true);
        try {
          const response = await axios.get(`${API_BASE_URL}/trips/search`, {
            params: { origin, destination, date }
          });
          setTrips(response.data);
          if (response.data.length === 0) {
            toast.info("Không tìm thấy chuyến đi nào phù hợp.");
          }
        } catch (error) {
          console.error(error);
          toast.error("Lỗi khi tìm kiếm chuyến đi.");
        } finally {
          setLoading(false);
        }
      }
    };
    fetchTrips();
  }, [searchParams]);

  // Handle browser close / reload (Release seats)
  useEffect(() => {
    const handleBeforeUnload = () => {
      const seatsToRelease = selectedSeatsRef.current;
      const trip = selectedTripRef.current;
      if (seatsToRelease.length > 0 && trip) {
        const payload = { tripId: trip.id, seatNumbers: seatsToRelease, userId: userId };
        const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
        navigator.sendBeacon(`${API_BASE_URL}/booking/release-batch`, blob);
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [userId]);

  const selectTrip = async (trip: Trip) => {
    setSelectedTrip(trip);
    setLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/booking/trip/${trip.id}/seats`);
      setBookedSeats(response.data || []);
      setStep(3);
      
      // Polling
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
        await axios.post(`${API_BASE_URL}/booking/release`, { tripId: selectedTrip?.id, seatNumber: seat, userId: userId });
        setSelectedSeats(selectedSeats.filter(s => s !== seat));
      } catch (error) { console.error(error); }
    } else {
      if (selectedSeats.length >= 5) { toast.warning("Tối đa 5 ghế."); return; }
      try {
        await axios.post(`${API_BASE_URL}/booking/hold`, { tripId: selectedTrip?.id, seatNumber: seat, userId: userId });
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
    const storedUserId = localStorage.getItem('userId');
    if (!storedUserId) { toast.error("Vui lòng đăng nhập để đặt vé!"); return; }
    
    setLoading(true);
    const bookingRequest: BookingRequest = {
      tripId: selectedTrip.id,
      userId: Number(storedUserId),
      seats: selectedSeats,
      busId: 1, stationId: 1
    };

    try {
      const response = await axios.post(`${API_BASE_URL}/booking`, bookingRequest);
      toast.success(`Đặt vé thành công! Mã vé: ${response.data.id}`);
      setBookingId(response.data.id);
      setStep(4);
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
      const response = await axios.post(`${API_BASE_URL}/payment/vn-pay`, { amount, bookingId });
      if (response.data.code === "00" || response.data.code === "ok") {
        const url = response.data.data || response.data.paymentUrl;
        if (url) window.location.href = url;
        else toast.error("Không tìm thấy link thanh toán.");
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

  // --- Render Helpers ---
  const renderSeatGrid = () => {
    const rows = ['A', 'B', 'C', 'D', 'E'];
    return (
      <div className="bg-white p-6 rounded-xl shadow-inner border border-gray-200 max-w-md mx-auto">
        <div className="w-full bg-gray-200 text-gray-500 text-center py-2 rounded mb-8 text-sm font-semibold uppercase tracking-wider">Khoang lái</div>
        <div className="flex justify-between gap-8">
          <div className="grid grid-cols-2 gap-3">{rows.map(row => [1, 2].map(col => renderSingleSeat(`${row}${col}`)))}</div>
          <div className="flex items-center justify-center text-gray-300 text-xs font-vertical writing-mode-vertical">LỐI ĐI</div>
          <div className="grid grid-cols-2 gap-3">{rows.map(row => [3, 4].map(col => renderSingleSeat(`${row}${col}`)))}</div>
        </div>
      </div>
    );
  };

  const renderSingleSeat = (seatNum: string) => {
    const isBooked = bookedSeats.includes(seatNum) && !selectedSeats.includes(seatNum);
    const isSelected = selectedSeats.includes(seatNum);
    return (
      <button key={seatNum} disabled={isBooked} onClick={() => toggleSeat(seatNum)}
        className={`w-10 h-10 rounded-lg text-xs font-bold transition-all duration-200 flex items-center justify-center shadow-sm ${isBooked ? 'bg-gray-300 text-gray-500 cursor-not-allowed' : isSelected ? 'bg-orange-500 text-white transform scale-105 ring-2 ring-orange-300' : 'bg-blue-50 text-blue-600 border border-blue-200 hover:bg-blue-100'}`}>
        {seatNum}
      </button>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 font-sans">
      <Header />
      <main className="container mx-auto px-4 py-8 max-w-5xl">
        {/* Step 2: Trip List */}
        {step === 2 && (
          <div>
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
                    <div className="flex justify-between text-sm text-gray-500"><span>{trip.origin}</span><span>{trip.destination}</span></div>
                    <div className="mt-3 flex items-center gap-2">
                      <span className="bg-blue-100 text-blue-700 text-xs px-2 py-1 rounded font-semibold">{trip.busName}</span>
                      <span className="bg-green-100 text-green-700 text-xs px-2 py-1 rounded font-semibold">Ghế ngồi</span>
                    </div>
                  </div>
                  <div className="text-right min-w-[150px]">
                    {/* Fix toLocaleString */}
                    <div className="text-2xl font-bold text-orange-600 mb-2">{(trip.price || 0).toLocaleString()}đ</div>
                    <button onClick={() => selectTrip(trip)} className="w-full bg-blue-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-blue-700 transition shadow-sm">Chọn chuyến</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Step 3: Seat Selection */}
        {step === 3 && selectedTrip && (
          <div className="flex flex-col lg:flex-row gap-8">
            <div className="flex-1 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <button onClick={() => setStep(2)} className="mb-4 text-blue-600 font-medium hover:underline">&larr; Chọn chuyến khác</button>
              <h3 className="text-lg font-bold text-gray-800 mb-6 text-center">Sơ đồ ghế</h3>
              {renderSeatGrid()}
              <div className="flex justify-center gap-6 mt-8 text-sm">
                <div className="flex items-center gap-2"><div className="w-5 h-5 bg-blue-50 border border-blue-200 rounded"></div><span>Trống</span></div>
                <div className="flex items-center gap-2"><div className="w-5 h-5 bg-orange-500 rounded"></div><span>Đang chọn</span></div>
                <div className="flex items-center gap-2"><div className="w-5 h-5 bg-gray-300 rounded"></div><span>Đã đặt/Giữ</span></div>
              </div>
            </div>
            <div className="w-full lg:w-96">
              <div className="bg-white p-6 rounded-xl shadow-lg border border-blue-100 sticky top-24">
                <h3 className="text-lg font-bold text-blue-900 mb-4 border-b pb-2">Thông tin đặt vé</h3>
                <div className="space-y-3 mb-6">
                  <div className="flex justify-between"><span className="text-gray-500">Tuyến đường:</span><span className="font-medium">{selectedTrip.origin} - {selectedTrip.destination}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Giờ chạy:</span><span className="font-medium">{selectedTrip.departureTime}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Số lượng ghế:</span><span className="font-medium">{selectedSeats.length}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Ghế đã chọn:</span><span className="font-bold text-orange-600">{selectedSeats.join(', ') || '---'}</span></div>
                </div>
                <div className="border-t pt-4 mb-6">
                  {/* Fix toLocaleString */}
                  <div className="flex justify-between items-center"><span className="text-gray-600 font-medium">Tổng cộng:</span><span className="text-2xl font-bold text-blue-600">{(selectedSeats.length * (selectedTrip.price || 0)).toLocaleString()}đ</span></div>
                </div>
                <button onClick={confirmBooking} disabled={selectedSeats.length === 0 || loading} className={`w-full py-3 rounded-lg font-bold text-white transition shadow-md ${selectedSeats.length === 0 ? 'bg-gray-300 cursor-not-allowed' : 'bg-orange-500 hover:bg-orange-600'}`}>{loading ? 'Đang xử lý...' : 'Xác nhận đặt vé'}</button>
              </div>
            </div>
          </div>
        )}

        {/* Step 4: Payment */}
        {step === 4 && selectedTrip && (
          <div className="max-w-2xl mx-auto bg-white p-8 rounded-2xl shadow-lg border border-gray-100 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Đặt vé thành công!</h2>
            <p className="text-gray-600 mb-8">Mã đơn hàng: <span className="font-bold text-blue-600">#{bookingId}</span></p>
            <div className="bg-gray-50 p-6 rounded-xl mb-8 text-left">
              <h3 className="font-bold text-gray-800 mb-4 border-b pb-2">Chi tiết thanh toán</h3>
              <div className="flex justify-between mb-2"><span className="text-gray-600">Tuyến xe:</span><span className="font-medium">{selectedTrip.origin} - {selectedTrip.destination}</span></div>
              <div className="flex justify-between mb-2"><span className="text-gray-600">Ghế đã chọn:</span><span className="font-medium">{selectedSeats.join(', ')}</span></div>
              {/* Fix toLocaleString */}
              <div className="flex justify-between pt-2 border-t mt-2"><span className="font-bold text-gray-800">Tổng tiền:</span><span className="font-bold text-orange-600 text-xl">{(selectedSeats.length * (selectedTrip.price || 0)).toLocaleString()}đ</span></div>
            </div>
            <div className="space-y-3">
              <button onClick={handlePayment} disabled={loading} className="w-full bg-blue-600 text-white py-3 rounded-lg font-bold hover:bg-blue-700 transition shadow-md flex items-center justify-center gap-2">{loading ? 'Đang chuyển hướng...' : 'Thanh toán ngay qua VNPay'}</button>
              <button onClick={() => navigate('/')} className="w-full bg-white text-gray-600 py-3 rounded-lg font-medium border border-gray-300 hover:bg-gray-50 transition">Về trang chủ</button>
            </div>
            <p className="text-xs text-gray-500 mt-6">Lưu ý: Vé sẽ tự động hủy nếu không thanh toán trong vòng 15 phút.</p>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
};

export default SearchResults;
