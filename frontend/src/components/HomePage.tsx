import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Facebook, Twitter, Youtube, Instagram, 
  CheckCircle, Headphones, Ticket, DollarSign, 
  MapPin, Calendar, ArrowRightLeft, Plus, Circle 
} from 'lucide-react';

// --- Types ---
interface RouteCardProps {
  image: string;
  title: string;
  price: string;
  discount?: string;
}

// --- Mock Data (Using LoremFlickr for reliability) ---
const POPULAR_ROUTES: RouteCardProps[] = [
  {
    image: "https://loremflickr.com/800/600/sapa,mountain/all",
    title: "Hà Nội đi Sa Pa",
    price: "Từ 250k VNĐ",
    discount: "-15%"
  },
  {
    image: "https://loremflickr.com/800/600/dalat,flower/all",
    title: "Sài Gòn đi Đà Lạt",
    price: "Từ 300k VNĐ",
    discount: "Hot"
  },
  {
    image: "https://loremflickr.com/800/600/hoian,lantern/all",
    title: "Đà Nẵng đi Hội An",
    price: "Từ 150k VNĐ"
  },
  {
    image: "https://loremflickr.com/800/600/nhatrang,beach/all",
    title: "Sài Gòn đi Nha Trang",
    price: "Từ 280k VNĐ"
  },
  {
    image: "https://loremflickr.com/800/600/vungtau,sea/all",
    title: "Sài Gòn đi Vũng Tàu",
    price: "Từ 180k VNĐ"
  },
  {
    image: "https://loremflickr.com/800/600/hue,citadel/all",
    title: "Hà Nội đi Huế",
    price: "Từ 350k VNĐ"
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
      <Link to="/" className="flex items-center gap-2 no-underline group">
        <div className="bg-red-600 text-white p-1.5 rounded-lg transform group-hover:rotate-12 transition duration-300">
            <Ticket size={24} />
        </div>
        <span className="text-2xl font-extrabold text-gray-800 font-sans tracking-tight">Travel<span className="text-red-600">K</span></span>
      </Link>

      <nav className="hidden md:flex gap-8 font-medium text-gray-600">
        <Link to="/" className="text-red-600 font-bold hover:text-red-700 transition">Trang chủ</Link>
        <Link to="/search" className="hover:text-red-600 transition">Tuyến đường</Link>
        <Link to="/my-tickets" className="hover:text-red-600 transition">Vé của tôi</Link>
        <Link to="/support" className="hover:text-red-600 transition">Hỗ trợ</Link>
      </nav>

      {isLoggedIn ? (
        <div className="flex items-center gap-4">
          <span className="text-sm font-medium text-gray-700">Xin chào, <span className="text-red-600 font-bold">{username}</span></span>
          <button onClick={handleLogout} className="text-sm text-gray-500 hover:text-red-500 font-medium border border-gray-200 px-3 py-1.5 rounded-full hover:bg-red-50 transition">
            Đăng xuất
          </button>
        </div>
      ) : (
        <Link to="/auth">
          <button className="bg-red-600 hover:bg-red-700 text-white px-6 py-2.5 rounded-full font-bold transition shadow-md hover:shadow-lg transform hover:-translate-y-0.5">
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
    origin: 'Hà Nội',
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
      <div className="bg-white rounded-xl shadow-2xl flex-grow flex flex-col md:flex-row md:items-center py-3 px-2 text-gray-700 font-medium border border-gray-100">

        {/* Ô 1: Nơi xuất phát */}
        <div className="flex-1 flex items-center gap-3 px-4 py-2 md:py-0 cursor-pointer hover:bg-gray-50 transition rounded-lg group relative border-b md:border-b-0 border-gray-100 pb-3 md:pb-0">
           <Circle className="text-blue-600 w-5 h-5 fill-current group-hover:scale-110 transition" /> 
           <div className="flex flex-col w-full">
              <span className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-0.5">Nơi xuất phát</span>
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
             className="absolute bg-white border border-gray-200 p-2 rounded-full hover:bg-gray-50 transition z-10 hover:rotate-180 duration-300 shadow-sm text-blue-600"
           >
              <ArrowRightLeft className="w-4 h-4" />
           </button>
        </div>

        {/* Ô 2: Nơi đến */}
         <div className="flex-1 flex items-center gap-3 px-4 py-2 md:py-0 cursor-pointer hover:bg-gray-50 transition rounded-lg md:pl-6 group border-b md:border-b-0 border-gray-100 pb-3 md:pb-0">
           <MapPin className="text-red-600 w-6 h-6 fill-current group-hover:scale-110 transition" />
           <div className="flex flex-col w-full">
              <span className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-0.5">Nơi đến</span>
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

        <div className="hidden md:block h-12 border-r border-gray-200 mx-2"></div>

        {/* Ô 3: Ngày đi */}
        <div className="flex-1 flex items-center gap-3 px-4 py-2 md:py-0 cursor-pointer hover:bg-gray-50 transition rounded-lg group">
           <Calendar className="text-blue-600 w-6 h-6 group-hover:scale-110 transition" />
           <div className="flex flex-col w-full">
              <span className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-0.5">Ngày đi</span>
              <input 
                type="date" 
                name="date"
                value={searchParams.date}
                onChange={handleChange}
                className="text-lg font-bold text-gray-800 bg-transparent outline-none w-full cursor-pointer"
              />
           </div>
        </div>

         <div className="hidden md:block h-12 border-r border-gray-200 mx-2"></div>

        {/* Ô 4: Thêm ngày về */}
        <div className="flex items-center gap-2 px-4 py-3 md:py-0 cursor-pointer hover:bg-blue-50 rounded-lg transition text-blue-600 font-bold whitespace-nowrap group">
           <Plus className="w-5 h-5 group-hover:rotate-90 transition" />
           <span>Thêm ngày về</span>
        </div>

      </div>

      <button 
        onClick={handleSearch}
        className="bg-[#FFC700] hover:bg-[#e5b300] text-black text-xl font-extrabold py-4 px-10 rounded-xl shadow-xl transition duration-300 whitespace-nowrap active:scale-95 transform hover:-translate-y-1"
      >
        TÌM KIẾM
      </button>
    </div>
  );
};

const FeatureBar = () => {
  return (
    <div className="absolute bottom-0 left-0 w-full bg-black/60 backdrop-blur-md py-4 border-t border-white/10 z-20">
      <div className="max-w-6xl mx-auto px-4 flex flex-wrap justify-center md:justify-between items-center gap-6 text-white">
        <div className="flex items-center gap-3 hover:text-yellow-400 transition-colors cursor-default group">
          <CheckCircle className="w-6 h-6 text-yellow-400 group-hover:scale-110 transition" />
          <span className="font-bold text-sm md:text-base">Chắc chắn có chỗ</span>
        </div>
        <div className="flex items-center gap-3 hover:text-yellow-400 transition-colors cursor-default group">
          <Headphones className="w-6 h-6 text-yellow-400 group-hover:scale-110 transition" />
          <span className="font-bold text-sm md:text-base">Hỗ trợ 24/7</span>
        </div>
        <div className="flex items-center gap-3 hover:text-yellow-400 transition-colors cursor-default group">
          <Ticket className="w-6 h-6 text-yellow-400 group-hover:scale-110 transition" />
          <span className="font-bold text-sm md:text-base">Nhiều ưu đãi</span>
        </div>
        <div className="flex items-center gap-3 hover:text-yellow-400 transition-colors cursor-default group">
          <DollarSign className="w-6 h-6 text-yellow-400 group-hover:scale-110 transition" />
          <span className="font-bold text-sm md:text-base">Thanh toán đa dạng</span>
        </div>
      </div>
    </div>
  );
};

const Hero = () => {
  return (
    <div className="relative h-[600px] flex items-center justify-center overflow-hidden">
      {/* Background Image Area */}
      <div className="absolute inset-0 z-0">
        <img 
          src="https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?q=80&w=2000&auto=format&fit=crop" 
          alt="Bus Travel" 
          className="w-full h-full object-cover brightness-[0.7] scale-105 animate-slow-zoom"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-black/30"></div>
      </div>
      
      <div className="z-30 text-center text-white mb-20 px-4">
        <h1 className="text-4xl md:text-6xl font-extrabold mb-4 drop-shadow-lg">
          Vivu khắp Việt Nam cùng <span className="text-[#FFC700]">TravelK</span>
        </h1>
        <p className="text-lg md:text-xl font-medium text-gray-200 mb-8 max-w-2xl mx-auto drop-shadow-md">
          Đặt vé xe khách trực tuyến giá rẻ, uy tín, chất lượng cao. Hơn 5000+ tuyến đường đang chờ bạn khám phá.
        </p>
      </div>

      <FeatureBar />
      
      {/* Search Form Container */}
      <div className="absolute bottom-24 w-full px-4 z-30">
        <SearchForm />
      </div>
    </div>
  );
};

const RouteCard = ({ image, title, price, discount }: RouteCardProps) => {
  return (
    <div className="bg-white rounded-2xl shadow-md overflow-hidden hover:shadow-2xl transition-all duration-300 flex flex-col group cursor-pointer border border-gray-100">
      <div className="w-full h-56 overflow-hidden relative">
        <img src={image} alt={title} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
        {discount && (
          <div className="absolute top-3 right-3 bg-red-600 text-white text-xs font-bold px-3 py-1 rounded-full shadow-md">
            {discount}
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
      </div>
      <div className="p-5">
        <h3 className="text-xl font-bold text-gray-800 mb-2 group-hover:text-blue-600 transition-colors">{title}</h3>
        <div className="flex justify-between items-center">
            <p className="text-gray-500 text-sm">Giá vé chỉ từ</p>
            <p className="text-red-600 font-extrabold text-lg">{price}</p>
        </div>
        <button className="w-full mt-4 border-2 border-blue-600 text-blue-600 hover:bg-blue-600 hover:text-white py-2 rounded-lg font-bold transition duration-300">
            Đặt ngay
        </button>
      </div>
    </div>
  );
};

const PopularRoutes = () => {
  return (
    <section className="py-20 px-4 max-w-7xl mx-auto">
      <div className="flex justify-between items-end mb-10">
        <div>
            <h2 className="text-3xl md:text-4xl font-extrabold text-gray-800 mb-2">
                Tuyến đường phổ biến
            </h2>
            <p className="text-gray-500">Các điểm đến được yêu thích nhất trong tháng này</p>
        </div>
        <a href="/search" className="hidden md:block text-blue-600 font-bold hover:underline">Xem tất cả &rarr;</a>
      </div>
      
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-8">
        {POPULAR_ROUTES.map((route, index) => (
          <RouteCard key={index} {...route} />
        ))}
      </div>
      
      <div className="mt-10 text-center md:hidden">
        <a href="/search" className="text-blue-600 font-bold hover:underline">Xem tất cả &rarr;</a>
      </div>
    </section>
  );
};

export const Footer = () => {
  return (
    <footer className="bg-gray-900 text-white py-12 px-8 border-t-4 border-red-600">
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
        <div>
            <h3 className="text-2xl font-bold mb-4">Travel<span className="text-red-600">K</span></h3>
            <p className="text-gray-400 text-sm leading-relaxed">
                Hệ thống đặt vé xe khách trực tuyến hàng đầu Việt Nam. Kết nối hàng triệu hành khách với hàng ngàn nhà xe uy tín.
            </p>
        </div>
        <div>
            <h4 className="font-bold text-lg mb-4">Về chúng tôi</h4>
            <ul className="space-y-2 text-gray-400 text-sm">
                <li><Link to="/about" className="hover:text-white">Giới thiệu</Link></li>
                <li><Link to="/terms" className="hover:text-white">Điều khoản sử dụng</Link></li>
                <li><Link to="/privacy" className="hover:text-white">Chính sách bảo mật</Link></li>
            </ul>
        </div>
        <div>
            <h4 className="font-bold text-lg mb-4">Hỗ trợ</h4>
            <ul className="space-y-2 text-gray-400 text-sm">
                <li><Link to="/support" className="hover:text-white">Trung tâm trợ giúp</Link></li>
                <li><Link to="/contact" className="hover:text-white">Liên hệ</Link></li>
                <li><Link to="/recruitment" className="hover:text-white">Tuyển dụng</Link></li>
            </ul>
        </div>
        <div>
            <h4 className="font-bold text-lg mb-4">Kết nối</h4>
            <div className="flex gap-4">
                <Facebook className="cursor-pointer hover:text-blue-500 transition" />
                <Youtube className="cursor-pointer hover:text-red-600 transition" />
                <Instagram className="cursor-pointer hover:text-pink-500 transition" />
                <Twitter className="cursor-pointer hover:text-blue-400 transition" />
            </div>
        </div>
      </div>
      <div className="border-t border-gray-800 pt-8 text-center text-gray-500 text-sm">
        &copy; 2026 TravelK. All rights reserved.
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
