import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';
import { Plus, Edit, Trash2, X } from 'lucide-react';

interface Trip {
  id: number;
  origin: string;
  destination: string;
  departureDate: string;
  departureTime: string;
  arrivalDate: string;
  arrivalTime: string;
  price: number;
  busName: string;
}

interface Bus {
  id: number;
  busNumber: string;
  number: string;
  busType: string;
  seats: number;
}

// Không cần interface Station nữa vì nhập text

const TripManager: React.FC = () => {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [buses, setBuses] = useState<Bus[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);

  // Form State - Đổi sang dùng Name thay vì ID cho trạm
  const [formData, setFormData] = useState({
    departureStationName: '', // Changed from Id to Name
    arrivalStationName: '',   // Changed from Id to Name
    busId: '',
    price: '',
    departureDate: '',
    departureTime: '',
    arrivalDate: '',
    arrivalTime: ''
  });

  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const busRes = await axios.get('/buses', { headers });
      setBuses(busRes.data);

      const tripRes = await axios.get('/trips', { headers });
      setTrips(tripRes.data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.departureStationName || !formData.arrivalStationName || !formData.busId) {
        toast.error("Vui lòng nhập đầy đủ thông tin!");
        return;
    }

    const payload = {
        ...formData,
        busId: Number(formData.busId),
        price: Number(formData.price)
    };

    try {
      await axios.post('/trips', payload, { headers });
      toast.success("Tạo chuyến xe thành công!");
      setShowModal(false);
      fetchData(); 
      setFormData({
        departureStationName: '',
        arrivalStationName: '',
        busId: '',
        price: '',
        departureDate: '',
        departureTime: '',
        arrivalDate: '',
        arrivalTime: ''
      });
    } catch (error: any) {
      console.error(error);
      toast.error(error.response?.data?.message || "Lỗi tạo chuyến xe.");
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-2xl font-bold text-gray-800">Quản lý Chuyến xe</h3>
        <button 
          onClick={() => setShowModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700"
        >
          <Plus className="w-5 h-5" /> Thêm chuyến mới
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 text-sm">
            <tr>
              <th className="p-4">ID</th>
              <th className="p-4">Tuyến đường</th>
              <th className="p-4">Xe</th>
              <th className="p-4">Khởi hành</th>
              <th className="p-4">Giá vé</th>
              <th className="p-4">Hành động</th>
            </tr>
          </thead>
          <tbody className="text-sm">
            {trips.length === 0 ? (
              <tr>
                <td colSpan={6} className="p-8 text-center text-gray-500">
                  Chưa có dữ liệu chuyến xe.
                </td>
              </tr>
            ) : (
              trips.map(trip => (
                <tr key={trip.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="p-4">#{trip.id}</td>
                  <td className="p-4">{trip.origin} - {trip.destination}</td>
                  <td className="p-4">{trip.busName}</td>
                  <td className="p-4">
                    <div>{trip.departureTime}</div>
                    <div className="text-xs text-gray-400">{trip.departureDate}</div>
                  </td>
                  <td className="p-4 font-bold text-blue-600">{trip.price?.toLocaleString()}đ</td>
                  <td className="p-4 flex gap-2">
                    <button className="text-blue-600 hover:bg-blue-50 p-2 rounded"><Edit className="w-4 h-4" /></button>
                    <button className="text-red-600 hover:bg-red-50 p-2 rounded"><Trash2 className="w-4 h-4" /></button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6">
            <div className="flex justify-between items-center mb-6">
              <h4 className="text-xl font-bold text-gray-800">Thêm chuyến xe mới</h4>
              <button onClick={() => setShowModal(false)} className="text-gray-500 hover:text-gray-700">
                <X className="w-6 h-6" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Điểm đi (Nhập tên)</label>
                <input 
                  type="text"
                  name="departureStationName" 
                  value={formData.departureStationName} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  placeholder="Ví dụ: Hà Nội"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Điểm đến (Nhập tên)</label>
                <input 
                  type="text"
                  name="arrivalStationName" 
                  value={formData.arrivalStationName} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  placeholder="Ví dụ: Sapa"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Chọn Xe</label>
                <select 
                  name="busId" 
                  value={formData.busId} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  required
                >
                  <option value="">Chọn xe</option>
                  {buses.map(b => (
                    <option key={b.id} value={b.id}>
                      {b.number || b.busNumber} ({b.busType})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Giá vé</label>
                <input 
                  type="number" 
                  name="price" 
                  value={formData.price} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  placeholder="VNĐ"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Ngày đi</label>
                <input 
                  type="date" 
                  name="departureDate" 
                  value={formData.departureDate} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Giờ đi</label>
                <input 
                  type="time" 
                  name="departureTime" 
                  value={formData.departureTime} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Ngày đến</label>
                <input 
                  type="date" 
                  name="arrivalDate" 
                  value={formData.arrivalDate} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Giờ đến</label>
                <input 
                  type="time" 
                  name="arrivalTime" 
                  value={formData.arrivalTime} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  required
                />
              </div>

              <div className="col-span-2 mt-4 flex justify-end gap-3">
                <button 
                  type="button" 
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
                >
                  Hủy
                </button>
                <button 
                  type="submit"
                  className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium"
                >
                  Tạo chuyến xe
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default TripManager;
