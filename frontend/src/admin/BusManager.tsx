import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';
import { Plus, Edit, Trash2, X, Bus as BusIcon } from 'lucide-react';

interface Bus {
  id: number;
  busNumber: string; 
  number: string; 
  busType: string;
  seats: number;
  stationName: string;
}

interface Station {
  stationId: number;
  stationName: string;
}

const BusManager: React.FC = () => {
  const [buses, setBuses] = useState<Bus[]>([]);
  const [stations, setStations] = useState<Station[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    busNumber: '',
    busType: '',
    seats: '',
    stationId: ''
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

      const stationRes = await axios.get('/station', { headers });
      setStations(stationRes.data);
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
    
    if (!formData.stationId) {
        toast.error("Vui lòng chọn trạm quản lý xe!");
        return;
    }

    const payload = {
        number: formData.busNumber, 
        busType: formData.busType,
        seats: Number(formData.seats)
    };
    
    try {
      await axios.post(`/buses/station/${formData.stationId}`, payload, { headers });
      toast.success("Thêm xe thành công!");
      setShowModal(false);
      fetchData(); 
      setFormData({ busNumber: '', busType: '', seats: '', stationId: '' });
    } catch (error: any) {
      console.error(error);
      toast.error(error.response?.data?.message || "Lỗi thêm xe.");
    }
  };

  const handleDelete = async (id: number) => {
      if(!window.confirm("Bạn có chắc chắn muốn xóa xe này?")) return;
      try {
          await axios.delete(`/buses/${id}`, { headers });
          toast.success("Xóa xe thành công!");
          setBuses(prev => prev.filter(bus => bus.id !== id));
      } catch (error: any) {
          console.error(error);
          const message = error.response?.data?.message || "Không thể xóa xe (có thể do đang có chuyến đi";
          toast.error(message);
      }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <BusIcon className="w-8 h-8 text-blue-600" />
            Quản lý Xe (Bus)
        </h3>
        <button 
          onClick={() => setShowModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700"
        >
          <Plus className="w-5 h-5" /> Thêm xe mới
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 text-sm">
            <tr>
              <th className="p-4">ID</th>
              <th className="p-4">Biển số</th>
              <th className="p-4">Loại xe</th>
              <th className="p-4">Số ghế</th>
              <th className="p-4">Trạm quản lý</th>
              <th className="p-4">Hành động</th>
            </tr>
          </thead>
          <tbody className="text-sm">
            {buses.length === 0 ? (
              <tr>
                <td colSpan={6} className="p-8 text-center text-gray-500">
                  Chưa có dữ liệu xe.
                </td>
              </tr>
            ) : (
              buses.map(bus => (
                <tr key={bus.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="p-4">#{bus.id}</td>
                  <td className="p-4 font-bold">{bus.number || bus.busNumber}</td>
                  <td className="p-4">{bus.busType}</td>
                  <td className="p-4">{bus.seats}</td>
                  <td className="p-4 text-blue-600">{bus.stationName || 'N/A'}</td>
                  <td className="p-4 flex gap-2">
                    <button className="text-blue-600 hover:bg-blue-50 p-2 rounded"><Edit className="w-4 h-4" /></button>
                    <button onClick={() => handleDelete(bus.id)} className="text-red-600 hover:bg-red-50 p-2 rounded"><Trash2 className="w-4 h-4" /></button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
            <div className="flex justify-between items-center mb-6">
              <h4 className="text-xl font-bold text-gray-800">Thêm xe mới</h4>
              <button onClick={() => setShowModal(false)} className="text-gray-500 hover:text-gray-700">
                <X className="w-6 h-6" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Biển số xe</label>
                <input 
                  type="text" 
                  name="busNumber" 
                  value={formData.busNumber} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  placeholder="Ví dụ: 29A-12345"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Loại xe</label>
                <input 
                  type="text" 
                  name="busType" 
                  value={formData.busType} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  placeholder="Ví dụ: Giường nằm, Limousine"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Số ghế</label>
                <input 
                  type="number" 
                  name="seats" 
                  value={formData.seats} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  placeholder="Ví dụ: 34"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Trạm quản lý</label>
                <select 
                  name="stationId" 
                  value={formData.stationId} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2"
                  required
                >
                  <option value="">Chọn trạm</option>
                  {stations.map(s => (
                    <option key={s.stationId} value={s.stationId}>
                      {s.stationName}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-3 mt-6">
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
                  Lưu lại
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default BusManager;
