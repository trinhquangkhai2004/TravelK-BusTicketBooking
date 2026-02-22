import React from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Bus, Map, Ticket, Users, LogOut, MapPin } from 'lucide-react';

const AdminLayout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    navigate('/auth');
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <div className="min-h-screen bg-gray-100 font-sans flex">
      {/* Sidebar */}
      <aside className="w-64 bg-white shadow-lg fixed h-full z-10">
        <div className="p-6 border-b border-gray-100">
          <h2 className="text-2xl font-extrabold text-gray-800 tracking-tight">
            Travel<span className="text-red-600">K</span> <span className="text-sm font-medium text-gray-500">Admin</span>
          </h2>
        </div>
        
        <nav className="p-4 space-y-2">
          <Link to="/admin" className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${isActive('/admin') ? 'bg-blue-50 text-blue-600 font-bold' : 'text-gray-600 hover:bg-gray-50'}`}>
            <LayoutDashboard size={20} /> Dashboard
          </Link>
          
          <Link to="/admin/stations" className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${isActive('/admin/stations') ? 'bg-blue-50 text-blue-600 font-bold' : 'text-gray-600 hover:bg-gray-50'}`}>
            <MapPin size={20} /> Quản lý Bến xe
          </Link>

          <Link to="/admin/buses" className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${isActive('/admin/buses') ? 'bg-blue-50 text-blue-600 font-bold' : 'text-gray-600 hover:bg-gray-50'}`}>
            <Bus size={20} /> Quản lý Xe
          </Link>
          
          <Link to="/admin/trips" className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${isActive('/admin/trips') ? 'bg-blue-50 text-blue-600 font-bold' : 'text-gray-600 hover:bg-gray-50'}`}>
            <Map size={20} /> Quản lý Chuyến đi
          </Link>
          
          <Link to="/admin/bookings" className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${isActive('/admin/bookings') ? 'bg-blue-50 text-blue-600 font-bold' : 'text-gray-600 hover:bg-gray-50'}`}>
            <Ticket size={20} /> Quản lý Đặt vé
          </Link>
          
          <Link to="/admin/users" className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${isActive('/admin/users') ? 'bg-blue-50 text-blue-600 font-bold' : 'text-gray-600 hover:bg-gray-50'}`}>
            <Users size={20} /> Quản lý Người dùng
          </Link>
        </nav>

        <div className="absolute bottom-0 w-full p-4 border-t border-gray-100">
          <button onClick={handleLogout} className="flex items-center gap-3 px-4 py-3 w-full text-red-600 hover:bg-red-50 rounded-lg transition-colors font-medium">
            <LogOut size={20} /> Đăng xuất
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 ml-64 p-8">
        <Outlet />
      </main>
    </div>
  );
};

export default AdminLayout;
