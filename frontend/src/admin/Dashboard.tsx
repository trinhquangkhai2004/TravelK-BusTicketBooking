import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';

interface StatisticDto {
  userCount: number;
  totalTicketSold: number;
  totalTrips: number;
  revenue: number;
}

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<StatisticDto>({
    userCount: 0,
    totalTicketSold: 0,
    totalTrips: 0,
    revenue: 0
  });

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const token = localStorage.getItem('token');
        const response = await axios.get('/api/admin/statistics', {
            headers: { Authorization: `Bearer ${token}` }
        });
        const data = response.data;
        setStats({
            userCount: data.userCount || 0,
            totalTicketSold: data.totalTicketSold || 0,
            totalTrips: data.totalTrips || 0,
            revenue: data.revenue || 0
        });
      } catch (error) {
        console.error("Failed to fetch statistics", error);
      }
    };

    fetchStats();
  }, []);

  return (
    <div>
      <h3 className="text-2xl font-bold text-gray-800 mb-6">Tổng quan</h3>
      
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <p className="text-gray-500 text-sm font-medium">Tổng doanh thu</p>
          {/* Fix lỗi toLocaleString khi revenue null */}
          <h4 className="text-2xl font-bold text-gray-800 mt-2">{(stats.revenue || 0).toLocaleString()}đ</h4>
          <span className="text-green-500 text-xs font-medium">Cập nhật realtime</span>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <p className="text-gray-500 text-sm font-medium">Vé đã bán</p>
          <h4 className="text-2xl font-bold text-gray-800 mt-2">{stats.totalTicketSold}</h4>
          <span className="text-green-500 text-xs font-medium">Cập nhật realtime</span>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <p className="text-gray-500 text-sm font-medium">Chuyến xe</p>
          <h4 className="text-2xl font-bold text-gray-800 mt-2">{stats.totalTrips}</h4>
          <span className="text-gray-400 text-xs font-medium">Đang hoạt động</span>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <p className="text-gray-500 text-sm font-medium">Người dùng</p>
          <h4 className="text-2xl font-bold text-gray-800 mt-2">{stats.userCount}</h4>
          <span className="text-green-500 text-xs font-medium">Tổng số thành viên</span>
        </div>
      </div>

      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <h4 className="text-lg font-bold text-gray-800 mb-4">Giao dịch gần đây</h4>
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-gray-100 text-gray-500 text-sm">
                <th className="py-3 font-medium">Mã đơn</th>
                <th className="py-3 font-medium">Khách hàng</th>
                <th className="py-3 font-medium">Chuyến đi</th>
                <th className="py-3 font-medium">Số tiền</th>
                <th className="py-3 font-medium">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="text-sm">
              <tr className="border-b border-gray-50 hover:bg-gray-50">
                <td className="py-3 text-blue-600 font-medium">#BK001</td>
                <td className="py-3">Nguyễn Văn A</td>
                <td className="py-3">Hà Nội - Sapa</td>
                <td className="py-3">500.000đ</td>
                <td className="py-3"><span className="bg-green-100 text-green-700 px-2 py-1 rounded-full text-xs font-medium">Thành công</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
