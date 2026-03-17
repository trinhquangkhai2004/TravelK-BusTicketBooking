import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';
import { Ticket, Trash2, CheckCircle, XCircle } from 'lucide-react';

interface Booking {
  id: number;
  status: string;
  userId: number;
  tripId: number;
  userName: string;
  phoneNumber: string;
}

const BookingManager: React.FC = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);

  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/booking', { headers });
      setBookings(response.data);
    } catch (error) {
      console.error(error);
      // toast.error("Lỗi tải danh sách vé.");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Bạn có chắc chắn muốn hủy vé này?")) return;
    
    // Optimistic UI
    const prevBookings = [...bookings];
    setBookings(bookings.filter(b => b.id !== id));

    try {
      await axios.delete(`/api/booking/${id}`, { headers });
      toast.success("Hủy vé thành công!");
    } catch (error) {
      console.error(error);
      setBookings(prevBookings);
      toast.error("Lỗi hủy vé.");
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <Ticket className="w-8 h-8 text-blue-600" />
            Quản lý Đặt vé
        </h3>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 text-sm">
            <tr>
              <th className="p-4">ID</th>
              <th className="p-4">Khách hàng</th>
              <th className="p-4">SĐT</th>
              <th className="p-4">Chuyến đi (ID)</th>
              <th className="p-4">Trạng thái</th>
              <th className="p-4">Hành động</th>
            </tr>
          </thead>
          <tbody className="text-sm">
            {bookings.length === 0 ? (
              <tr>
                <td colSpan={6} className="p-8 text-center text-gray-500">
                  Chưa có dữ liệu đặt vé.
                </td>
              </tr>
            ) : (
              bookings.map(booking => (
                <tr key={booking.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="p-4">#{booking.id}</td>
                  <td className="p-4 font-medium">{booking.userName}</td>
                  <td className="p-4">{booking.phoneNumber}</td>
                  <td className="p-4 text-blue-600">#{booking.tripId}</td>
                  <td className="p-4">
                    <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                      booking.status === 'PAID' ? 'bg-green-100 text-green-700' :
                      booking.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                      'bg-red-100 text-red-700'
                    }`}>
                      {booking.status}
                    </span>
                  </td>
                  <td className="p-4 flex gap-2">
                    <button 
                        onClick={() => handleDelete(booking.id)}
                        className="text-red-600 hover:bg-red-50 p-2 rounded"
                        title="Hủy vé"
                    >
                        <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default BookingManager;
