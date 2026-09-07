import React from 'react'
import ReactDOM from 'react-dom/client'
import axios from 'axios'
import App from './App.tsx'
import 'react-toastify/dist/ReactToastify.css';

// Tự động đính kèm JWT vào mọi request của axios.
// Các API cần đăng nhập (vd: /api/booking/**) sẽ bị 403 nếu thiếu header này.
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
