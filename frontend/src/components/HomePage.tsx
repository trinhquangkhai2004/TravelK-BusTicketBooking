import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Facebook, Twitter, Youtube, Instagram, 
  CheckCircle, Headphones, Ticket, DollarSign, // FeatureBar Icons (Safe names)
  MapPin, Calendar, ArrowRightLeft, Plus, Circle // SearchForm Icons
} from 'lucide-react';

// --- Types ---
interface RouteCardProps {
  image: string;
  title: string;
  price: string;
}

// --- Mock Data ---
const POPULAR_ROUTES: RouteCardProps[] = [
  {
    image: "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=800&auto=format&fit=crop",
    title: "Hà Nội đi Sa Pa",
    price: "Từ 250k VNĐ"
  },
  {
    image: "https://images.unsplash.com/photo-1558509804-07d077c59501?q=80&w=800&auto=format&fit=crop",
    title: "Sài Gòn đi Đà Lạt",
    price: "Từ 300k VNĐ"
  },
  {
    image: "https://images.unsplash.com/photo-1565158102910-381414e21c3a?q=80&w=800&auto=format&fit=crop",
    title: "Đà Nẵng đi Huế",
    price: "Từ 150k VNĐ"
  }
];

// --- Components ---

export const Header = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem('token'));
  const username = localStorage.getItem('username');
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    setIsLoggedIn(false);
    navigate('/');
  };

  return (
    <header className="flex items-center justify-between px-8 py-4 bg-white shadow-sm sticky top-0 z-50">
      <Link to="/" className="flex items-center gap-1 no-underline">
        <span className="text-3xl font-bold text-red-600 font-sans">TravelK</span>
      </Link>

      <nav className="hidden md:flex gap-8 font-medium text-gray-700">
        <Link to="/" className="text-red-500 font-semibold hover:text-red-600 transition">Trang chủ</Link>
        <Link to="/search" className="hover:text-red-500 transition">Tuyến đường</Link>
        <Link to="/my-tickets" className="hover:text-red-500 transition">Vé của tôi</Link>
        <Link to="/support" className="hover:text-red-500 transition">Hỗ trợ</Link>
      </nav>

      {isLoggedIn ? (
        <div className="flex items-center gap-4">
          <span className="text-sm font-medium text-gray-700">Xin chào, <span className="text-red-600 font-bold">{username}</span></span>
          <button onClick={handleLogout} className="text-sm text-gray-500 hover:text-red-500 font-medium">
            Đăng xuất
          </button>
        </div>
      ) : (
        <Link to="/auth">
          <button className="bg-red-500 hover:bg-red-600 text-white px-6 py-2 rounded font-medium transition">
            Đăng nhập
          </button>
        </Link>
      )}
    </header>
  );
};

const SearchForm = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useState({
    origin: 'Hà Nội', // Default value for better UI preview
    destination: 'Hồ Chí Minh',
    date: new Date().toISOString().split('T')[0]
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchParams({ ...searchParams, [e.target.name]: e.target.value });
  };

  const handleSearch = () => {
    if (searchParams.origin && searchParams.destination && searchParams.date) {
      navigate(`/search?origin=${searchParams.origin}&destination=${searchParams.destination}&date=${searchParams.date}`);
    } else {
      alert("Vui lòng nhập đầy đủ thông tin tìm kiếm!");
    }
  };

  const swapLocations = () => {
    setSearchParams(prev => ({
      ...prev,
      origin: prev.destination,
      destination: prev.origin
    }));
  };

  return (
    <div className="flex flex-col md:flex-row items-stretch md:items-center gap-4 w-full max-w-[1100px] mx-auto font-sans">
      {/* --- Khối chứa các ô nhập liệu (Màu trắng) --- */}
      <div className="bg-white rounded-xl shadow-lg flex-grow flex flex-col md:flex-row md:items-center py-3 px-2 text-gray-700 font-medium">

        {/* Ô 1: Nơi xuất phát */}
        <div className="flex-1 flex items-center gap-3 px-3 py-2 md:py-0 cursor-pointer hover:bg-gray-50 transition rounded-lg group relative">
           <Circle className="text-blue-500 w-5 h-5 fill-current group-hover:scale-110 transition" /> 
           <div className="flex flex-col w-full">
              <span className="text-xs text-gray-400 font-normal mb-0.5">Nơi xuất phát</span>
              <input 
                type="text" 
                name="origin"
                value={searchParams.origin}
                onChange={handleChange}
                className="text-lg font-bold text-gray-800 bg-transparent outline-none w-full placeholder-gray-300"
                placeholder="Chọn điểm đi"
              />
           </div>
        </div>

        {/* Vách ngăn & Nút đảo chiều */}
        <div className="hidden md:flex h-12 border-r border-gray-200 relative mx-2 items-center justify-center">
           <button 
             onClick={swapLocations}
             className="absolute bg-gray-50 border border-gray-100 p-1.5 rounded-full hover:bg-gray-200 transition z-10 hover:rotate-180 duration-300"
           >
              <ArrowRightLeft className="w-4 h-4 text-gray-500" />
           </button>
        </div>
        <div className="md:hidden h-px w-full bg-gray-100 my-2 relative flex justify-center">
            <button 
              onClick={swapLocations}
              className="absolute top-[-12px] bg-gray-50 border border-gray-100 p-1.5 rounded-full z-10"
            >
              <ArrowRightLeft className="w-4 h-4 text-gray-500 rotate-90" />
           </button>
        </div>

        {/* Ô 2: Nơi đến */}
         <div className="flex-1 flex items-center gap-3 px-3 py-2 md:py-0 cursor-pointer hover:bg-gray-50 transition rounded-lg md:pl-6 group">
           <MapPin className="text-red-500 w-6 h-6 fill-current group-hover:scale-110 transition" />
           <div className="flex flex-col w-full">
              <span className="text-xs text-gray-400 font-normal mb-0.5">Nơi đến</span>
              <input 
                type="text" 
                name="destination"
                value={searchParams.destination}
                onChange={handleChange}
                className="text-lg font-bold text-gray-800 bg-transparent outline-none w-full placeholder-gray-300"
                placeholder="Chọn điểm đến"
              />
           </div>
        </div>

        {/* Vách ngăn */}
        <div className="hidden md:block h-12 border-r border-gray-200 mx-2"></div>
        <div className="md:hidden h-px w-full bg-gray-100 my-2"></div>

        {/* Ô 3: Ngày đi */}
        <div className="flex-1 flex items-center gap-3 px-3 py-2 md:py-0 cursor-pointer hover:bg-gray-50 transition rounded-lg group">
           <Calendar className="text-blue-500 w-6 h-6 group-hover:scale-110 transition" />
           <div className="flex flex-col w-full">
              <span className="text-xs text-gray-400 font-normal mb-0.5">Ngày đi</span>
              <input 
                type="date" 
                name="date"
                value={searchParams.date}
                onChange={handleChange}
                className="text-lg font-bold text-gray-800 bg-transparent outline-none w-full cursor-pointer"
              />
           </div>
        </div>

         {/* Vách ngăn */}
        <div className="hidden md:block h-12 border-r border-gray-200 mx-2"></div>
        <div className="md:hidden h-px w-full bg-gray-100 my-2"></div>

        {/* Ô 4: Thêm ngày về (Mock) */}
        <div className="flex items-center gap-2 px-4 py-3 md:py-0 cursor-pointer hover:bg-blue-50 rounded-lg transition text-blue-600 font-semibold whitespace-nowrap group">
           <Plus className="w-5 h-5 group-hover:rotate-90 transition" />
           <span>Thêm ngày về</span>
        </div>

      </div>

      {/* --- Nút Tìm kiếm (Màu vàng) --- */}
      <button 
        onClick={handleSearch}
        className="bg-[#FFC700] hover:bg-[#e5b300] text-black text-xl font-bold py-4 px-10 rounded-xl shadow-md transition duration-300 whitespace-nowrap active:scale-95"
      >
        Tìm kiếm
      </button>
    </div>
  );
};

const FeatureBar = () => {
  return (
    <div className="absolute bottom-0 left-0 w-full bg-black/70 backdrop-blur-sm py-3 border-t border-white/10 z-20">
      <div className="max-w-6xl mx-auto px-4 flex flex-wrap justify-center md:justify-between items-center gap-4 text-yellow-400">
        <div className="flex items-center gap-2 hover:text-yellow-300 transition-colors cursor-default">
          <CheckCircle className="w-5 h-5 fill-yellow-400 text-black" />
          <span className="font-bold text-sm md:text-base">Chắc chắn có chỗ</span>
        </div>
        <div className="flex items-center gap-2 hover:text-yellow-300 transition-colors cursor-default">
          <Headphones className="w-5 h-5" />
          <span className="font-bold text-sm md:text-base">Hỗ trợ 24/7</span>
        </div>
        <div className="flex items-center gap-2 hover:text-yellow-300 transition-colors cursor-default">
          <Ticket className="w-5 h-5" />
          <span className="font-bold text-sm md:text-base">Nhiều ưu đãi</span>
        </div>
        <div className="flex items-center gap-2 hover:text-yellow-300 transition-colors cursor-default">
          <DollarSign className="w-5 h-5" />
          <span className="font-bold text-sm md:text-base">Thanh toán đa dạng</span>
        </div>
      </div>
    </div>
  );
};

const Hero = () => {
  return (
    <div className="relative h-[550px] flex items-center justify-center overflow-hidden">
      {/* Background Image Area */}
      <div className="absolute inset-0 z-0">
        <img 
          src="https://images.unsplash.com/photo-1570125909232-eb2be79a1c74?q=80&w=2000&auto=format&fit=crop" 
          alt="Bus Travel" 
          className="w-full h-full object-cover brightness-[0.65] scale-105"
        />
      </div>
      
      <FeatureBar />
      
      {/* Search Form Container */}
      <div className="w-full px-4 z-30 relative -mt-10">
        <SearchForm />
      </div>
    </div>
  );
};

const RouteCard = ({ image, title, price }: RouteCardProps) => {
  return (
    <div className="bg-white rounded-2xl shadow-md overflow-hidden hover:shadow-xl transition-shadow duration-300 flex flex-col items-center pb-6">
      <div className="w-full h-48 overflow-hidden">
        <img src={image} alt={title} className="w-full h-full object-cover hover:scale-105 transition-transform duration-500" />
      </div>
      <h3 className="text-xl font-bold text-gray-800 mt-4 mb-1">{title}</h3>
      <p className="text-gray-600 font-medium mb-4">{price}</p>
      <button className="border-2 border-red-800 text-red-800 hover:bg-red-800 hover:text-white px-8 py-1.5 rounded-full font-bold transition duration-300">
        Đặt vé
      </button>
    </div>
  );
};

const PopularRoutes = () => {
  return (
    <section className="py-16 px-4 max-w-6xl mx-auto">
      <h2 className="text-3xl font-bold text-gray-800 mb-8 text-center md:text-left pl-2">
        Tuyến đường phổ biến
      </h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {POPULAR_ROUTES.map((route, index) => (
          <RouteCard key={index} {...route} />
        ))}
      </div>
    </section>
  );
};

export const Footer = () => {
  return (
    <footer className="bg-gray-200 py-6 px-8">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-4">
        <div className="flex gap-6 text-gray-700 font-medium text-sm">
          <Link to="/about" className="hover:underline">Giới thiệu</Link>
          <Link to="/terms" className="hover:underline">Điều khoản sử dụng</Link>
          <Link to="/contact" className="hover:underline">Liên hệ</Link>
        </div>
        <div className="flex gap-4 text-gray-700">
          <Facebook size={20} className="cursor-pointer hover:text-blue-600" />
          <span className="font-bold text-lg leading-none cursor-pointer">𝕏</span>
          <Youtube size={20} className="cursor-pointer hover:text-red-600" />
          <Instagram size={20} className="cursor-pointer hover:text-pink-600" />
        </div>
      </div>
    </footer>
  );
};

const HomePage = () => {
  return (
    <div className="min-h-screen bg-gray-50 font-sans">
      <Header />
      <Hero />
      <PopularRoutes />
      <div className="h-10"></div>
      <Footer />
    </div>
  );
};

export default HomePage;
