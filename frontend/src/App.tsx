import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useNavigate } from 'react-router-dom';
import HomePage from './components/HomePage';
import SearchResults from './components/SearchResults';
import AuthPage from './components/AuthPage';
import ForgotPasswordPage from './components/ForgotPasswordPage'; // Import
import ResetPasswordPage from './components/ResetPasswordPage'; // Import
import PaymentResult from './components/PaymentResult';
import MyTickets from './components/MyTickets';
import Support from './components/Support';
import AdminLayout from './admin/AdminLayout';
import Dashboard from './admin/Dashboard';
import TripManager from './admin/TripManager';
import BusManager from './admin/BusManager';
import BookingManager from './admin/BookingManager';
import UserManager from './admin/UserManager';
import ChatBox from './components/ChatBox';
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
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/booking-success" element={<PaymentResult />} />
        <Route path="/my-tickets" element={<MyTickets />} />
        <Route path="/support" element={<Support />} />

        {/* Admin Routes */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="buses" element={<BusManager />} />
          <Route path="trips" element={<TripManager />} />
          <Route path="bookings" element={<BookingManager />} />
          <Route path="users" element={<UserManager />} />
        </Route>
      </Routes>
      
      <ChatBox />
      
      <ToastContainer position="top-right" autoClose={3000} />
    </Router>
  );
};

export default App;
