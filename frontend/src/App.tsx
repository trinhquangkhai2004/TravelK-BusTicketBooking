import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useNavigate } from 'react-router-dom';
import HomePage from './components/HomePage';
import SearchResults from './components/SearchResults';
import AuthPage from './components/AuthPage';
import PaymentResult from './components/PaymentResult';
import AdminLayout from './admin/AdminLayout';
import Dashboard from './admin/Dashboard';
import TripManager from './admin/TripManager';
import BusManager from './admin/BusManager';
import ChatBox from './components/ChatBox'; // Import ChatBox
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<HomePage />} />
        <Route path="/search" element={<SearchResults />} />
        <Route path="/auth" element={<AuthPage onLoginSuccess={() => {}} />} />
        <Route path="/booking-success" element={<PaymentResult />} />

        {/* Admin Routes */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="buses" element={<BusManager />} />
          <Route path="trips" element={<TripManager />} />
          <Route path="bookings" element={<div>Quản lý Đặt vé (Coming Soon)</div>} />
          <Route path="users" element={<div>Quản lý Người dùng (Coming Soon)</div>} />
        </Route>
      </Routes>
      
      {/* ChatBox Global */}
      <ChatBox />
      
      <ToastContainer position="top-right" autoClose={3000} />
    </Router>
  );
};

export default App;
