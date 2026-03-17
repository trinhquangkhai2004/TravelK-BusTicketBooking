import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { toast } from 'react-toastify';
import { Users, Trash2, Shield } from 'lucide-react';

interface User {
  userId: number;
  userName: string;
  email: string;
  phoneNumber: string;
  roles: { roleName: string }[];
}

const UserManager: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);

  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/user/all', { headers });
      setUsers(response.data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa người dùng này?")) return;
    
    const prevUsers = [...users];
    setUsers(users.filter(u => u.userId !== id));

    try {
      await axios.delete(`/api/user/${id}`, { headers });
      toast.success("Xóa người dùng thành công!");
    } catch (error) {
      console.error(error);
      setUsers(prevUsers);
      toast.error("Lỗi xóa người dùng.");
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <Users className="w-8 h-8 text-blue-600" />
            Quản lý Người dùng
        </h3>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b border-gray-100 text-gray-500 text-sm">
            <tr>
              <th className="p-4">ID</th>
              <th className="p-4">Tên đăng nhập</th>
              <th className="p-4">Email</th>
              <th className="p-4">SĐT</th>
              <th className="p-4">Vai trò</th>
              <th className="p-4">Hành động</th>
            </tr>
          </thead>
          <tbody className="text-sm">
            {users.length === 0 ? (
              <tr>
                <td colSpan={6} className="p-8 text-center text-gray-500">
                  Chưa có người dùng nào.
                </td>
              </tr>
            ) : (
              users.map(user => (
                <tr key={user.userId} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="p-4">#{user.userId}</td>
                  <td className="p-4 font-bold">{user.userName}</td>
                  <td className="p-4">{user.email}</td>
                  <td className="p-4">{user.phoneNumber}</td>
                  <td className="p-4">
                    {user.roles.map(r => (
                        <span key={r.roleName} className={`inline-block px-2 py-1 rounded text-xs font-bold mr-1 ${
                            r.roleName === 'ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
                        }`}>
                            {r.roleName}
                        </span>
                    ))}
                  </td>
                  <td className="p-4 flex gap-2">
                    <button 
                        onClick={() => handleDelete(user.userId)}
                        className="text-red-600 hover:bg-red-50 p-2 rounded"
                        title="Xóa người dùng"
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

export default UserManager;
