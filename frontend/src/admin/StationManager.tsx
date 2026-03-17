import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';
import { MapPin, Plus, Edit, Trash2, X } from 'lucide-react';

interface Station {
  stationId: number;
  stationName: string;
  address: string;
}

const StationManager: React.FC = () => {
  const [stations, setStations] = useState<Station[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ name: '', address: '' });

  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  useEffect(() => {
    fetchStations();
  }, []);

  const fetchStations = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/station', { headers });
      setStations(response.data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await axios.post('/api/station', formData, { headers });
      toast.success("Thêm trạm thành công!");
      setShowModal(false);
      setFormData({ name: '', address: '' });
      fetchStations();
    } catch (error: any) {
      console.error(error);
      toast.error("Lỗi thêm trạm.");
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa trạm này?")) return;
    try {
      await axios.delete(`/api/station/${id}`, { headers });
      toast.success("Xóa trạm thành công!");
      fetchStations();
    } catch (error) {
      console.error(error);
      toast.error("Không thể xóa trạm (có thể đang được sử dụng).");
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <MapPin className="w-8 h-8 text-blue-600" />
            Quản lý Bến xe
        </h3>
        <button 
          onClick={() => setShowModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700"
        >
          <Plus className="w-5 h-5" /> Thêm bến xe
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 text-sm">
            <tr>
              <th className="p-4">ID</th>
              <th className="p-4">Tên bến xe</th>
              <th className="p-4">Địa chỉ</th>
              <th className="p-4">Hành động</th>
            </tr>
          </thead>
          <tbody className="text-sm">
            {stations.length === 0 ? (
              <tr>
                <td colSpan={4} className="p-8 text-center text-gray-500">
                  Chưa có dữ liệu bến xe.
                </td>
              </tr>
            ) : (
              stations.map(station => (
                <tr key={station.stationId} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="p-4">#{station.stationId}</td>
                  <td className="p-4 font-bold text-gray-800">{station.stationName}</td>
                  <td className="p-4 text-gray-600">{station.address || '---'}</td>
                  <td className="p-4 flex gap-2">
                    <button className="text-blue-600 hover:bg-blue-50 p-2 rounded"><Edit className="w-4 h-4" /></button>
                    <button onClick={() => handleDelete(station.stationId)} className="text-red-600 hover:bg-red-50 p-2 rounded"><Trash2 className="w-4 h-4" /></button>
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
              <h4 className="text-xl font-bold text-gray-800">Thêm bến xe mới</h4>
              <button onClick={() => setShowModal(false)} className="text-gray-500 hover:text-gray-700">
                <X className="w-6 h-6" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Tên bến xe</label>
                <input 
                  type="text"
                  name="name" 
                  value={formData.name} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-blue-500 outline-none"
                  placeholder="Ví dụ: Bến xe Mỹ Đình"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Địa chỉ</label>
                <input 
                  type="text"
                  name="address" 
                  value={formData.address} 
                  onChange={handleInputChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-blue-500 outline-none"
                  placeholder="Ví dụ: 20 Phạm Hùng, Hà Nội"
                  required
                />
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

export default StationManager;
