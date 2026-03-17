import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Header, Footer } from './HomePage';
import { QrCode, Clock, MapPin, Bus } from 'lucide-react';

interface Booking {
  id: number;
  status: string;
  tripId: number;
}

const MyTickets: React.FC = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const userId = localStorage.getItem('userId');

  useEffect(() => {
    if (userId) {
      fetchBookings();
    } else {
      setLoading(false);
    }
  }, [userId]);

  const fetchBookings = async () => {
    try {
      const response = await axios.get(`/api/booking/user/${userId}`);
      setBookings(response.data);
    } catch (error) {
      console.error("Failed to fetch bookings", error);
    } finally {
      setLoading(false);
    }
  };

  if (!userId) {
    return (
      <div className="min-h-screen bg-gray-50 font-sans">
        <Header />
        <div className="container mx-auto px-4 py-16 text-center">
          <h2 className="text-2xl font-bold text-gray-800 mb-4">Bạn chưa đăng nhập</h2>
          <p className="text-gray-600 mb-8">Vui lòng đăng nhập để xem vé của bạn.</p>
          <a href="/auth" className="bg-red-600 text-white px-6 py-3 rounded-full font-bold hover:bg-red-700 transition">
            Đăng nhập ngay
          </a>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 font-sans flex flex-col">
      <Header />
      <main className="flex-grow container mx-auto px-4 py-8 max-w-4xl">
        <h1 className="text-3xl font-bold text-gray-800 mb-8 flex items-center gap-3">
          <TicketIcon className="w-8 h-8 text-red-600" /> Vé của tôi
        </h1>

        {loading ? (
          <div className="text-center py-12">Đang tải...</div>
        ) : bookings.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl shadow-sm text-center border border-gray-100">
            <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <Bus className="w-10 h-10 text-gray-400" />
            </div>
            <h3 className="text-xl font-bold text-gray-800 mb-2">Chưa có vé nào</h3>
            <p className="text-gray-500 mb-8">Bạn chưa đặt chuyến đi nào. Hãy khám phá ngay!</p>
            <a href="/" className="text-red-600 font-bold hover:underline">Tìm chuyến đi &rarr;</a>
          </div>
        ) : (
          <div className="space-y-6">
            {bookings.map((booking) => (
              <div key={booking.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition duration-300">
                <div className="flex flex-col md:flex-row">
                  {/* Left: Ticket Info */}
                  <div className="flex-1 p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold mb-2 ${
                          booking.status === 'PAID' ? 'bg-green-100 text-green-700' : 
                          booking.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 
                          'bg-red-100 text-red-700'
                        }`}>
                          {booking.status === 'PAID' ? 'Đã thanh toán' : booking.status === 'PENDING' ? 'Chờ thanh toán' : 'Đã hủy'}
                        </span>
                        <h3 className="text-xl font-bold text-gray-800">Mã vé: #{booking.id}</h3>
                      </div>
                      <div className="text-right">
                        <div className="text-sm text-gray-500">Ngày đặt</div>
                        <div className="font-medium">Hôm nay</div> 
                      </div>
                    </div>

                    <div className="flex items-center gap-8 mb-6">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                          <span className="font-bold text-lg">Hà Nội</span> {/* Mock Data */}
                        </div>
                        <div className="pl-4 border-l-2 border-dashed border-gray-200 h-8 ml-1.5"></div>
                        <div className="flex items-center gap-2">
                          <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                          <span className="font-bold text-lg">Sapa</span> {/* Mock Data */}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="flex items-center gap-2 text-gray-600 mb-1">
                          <Clock className="w-4 h-4" /> 22:00
                        </div>
                        <div className="flex items-center gap-2 text-gray-600">
                          <Bus className="w-4 h-4" /> Limousine 34
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Right: QR Code */}
                  <div className="bg-gray-50 p-6 flex flex-col items-center justify-center border-t md:border-t-0 md:border-l border-gray-100 min-w-[200px]">
                    <QrCode className="w-24 h-24 text-gray-800 mb-3" />
                    <span className="text-xs text-gray-500 font-mono">SCAN ME</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
};

// Helper Icon
const TicketIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
    <path d="M13 5v2" /><path d="M13 17v2" /><path d="M13 11v2" />
  </svg>
);

export default MyTickets;
