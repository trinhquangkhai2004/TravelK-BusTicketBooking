import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

const PaymentResult: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'success' | 'failed' | 'loading'>('loading');
  const [bookingId, setBookingId] = useState('');
  const [amount, setAmount] = useState('');

  useEffect(() => {
    const responseCode = searchParams.get('vnp_ResponseCode');
    const txnRef = searchParams.get('vnp_TxnRef');
    const amountParam = searchParams.get('vnp_Amount');
    
    if (txnRef) setBookingId(txnRef);
    if (amountParam) setAmount((Number(amountParam) / 100).toLocaleString());

    if (responseCode === '00') {
      setStatus('success');
    } else if (responseCode) {
      setStatus('failed');
    } else {
      setStatus('failed');
    }
  }, [searchParams]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4 font-sans">
      <div className="bg-white p-8 rounded-3xl shadow-2xl max-w-md w-full text-center border border-gray-100 transform transition-all hover:scale-105 duration-300">
        {status === 'success' ? (
          <>
            <div className="w-24 h-24 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6 animate-bounce">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h2 className="text-3xl font-extrabold text-gray-900 mb-2">Thanh toán thành công!</h2>
            <p className="text-gray-500 mb-6">Giao dịch của bạn đã được hoàn tất.</p>
            
            <div className="bg-gray-50 rounded-xl p-4 mb-8 text-left space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-500">Mã đơn hàng</span>
                <span className="font-bold text-gray-800">#{bookingId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Số tiền</span>
                <span className="font-bold text-green-600">{amount} VND</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Thời gian</span>
                <span className="font-medium text-gray-800">{new Date().toLocaleString()}</span>
              </div>
            </div>

            <button 
              onClick={() => navigate('/')}
              className="w-full bg-gradient-to-r from-blue-600 to-blue-700 text-white py-3.5 rounded-xl font-bold hover:from-blue-700 hover:to-blue-800 transition shadow-lg hover:shadow-xl transform active:scale-95"
            >
              Về trang chủ
            </button>
          </>
        ) : (
          <>
            <div className="w-24 h-24 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6 animate-pulse">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <h2 className="text-3xl font-extrabold text-gray-900 mb-2">Thanh toán thất bại</h2>
            <p className="text-gray-500 mb-8">
              Giao dịch bị hủy hoặc có lỗi xảy ra. Vui lòng thử lại sau.
            </p>
            <button 
              onClick={() => navigate('/')}
              className="w-full bg-gray-800 text-white py-3.5 rounded-xl font-bold hover:bg-gray-900 transition shadow-lg hover:shadow-xl transform active:scale-95"
            >
              Quay về trang chủ
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default PaymentResult;
