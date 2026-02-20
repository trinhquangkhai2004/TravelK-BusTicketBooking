import React, { useState } from 'react';
import { Header, Footer } from './HomePage';
import { ChevronDown, ChevronUp, Mail, Phone, MapPin, Send } from 'lucide-react';

const FAQS = [
  {
    question: "Làm thế nào để đặt vé xe?",
    answer: "Bạn có thể đặt vé dễ dàng trên trang chủ bằng cách chọn điểm đi, điểm đến và ngày khởi hành. Sau đó chọn chuyến xe phù hợp và thanh toán trực tuyến."
  },
  {
    question: "Tôi có thể hủy vé đã đặt không?",
    answer: "Có, bạn có thể hủy vé trước giờ khởi hành 24h. Vui lòng liên hệ tổng đài hoặc vào mục 'Vé của tôi' để gửi yêu cầu hủy."
  },
  {
    question: "Tôi quên mật khẩu đăng nhập?",
    answer: "Hãy bấm vào 'Quên mật khẩu' tại trang đăng nhập và làm theo hướng dẫn để lấy lại mật khẩu qua email."
  },
  {
    question: "Thanh toán qua VNPay có an toàn không?",
    answer: "Tuyệt đối an toàn. Chúng tôi sử dụng cổng thanh toán VNPay được cấp phép bởi Ngân hàng Nhà nước, mọi thông tin thẻ đều được mã hóa."
  }
];

const Support: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <div className="min-h-screen bg-gray-50 font-sans">
      <Header />
      
      {/* Hero Banner */}
      <div className="bg-blue-600 text-white py-16 text-center">
        <h1 className="text-4xl font-bold mb-4">Trung tâm trợ giúp</h1>
        <p className="text-blue-100 text-lg max-w-2xl mx-auto">Chúng tôi luôn sẵn sàng hỗ trợ bạn 24/7. Tìm câu trả lời nhanh hoặc liên hệ trực tiếp với chúng tôi.</p>
      </div>

      <main className="container mx-auto px-4 py-12 max-w-5xl">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
          
          {/* FAQ Section */}
          <div>
            <h2 className="text-2xl font-bold text-gray-800 mb-6">Câu hỏi thường gặp</h2>
            <div className="space-y-4">
              {FAQS.map((faq, index) => (
                <div key={index} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                  <button 
                    onClick={() => setOpenIndex(openIndex === index ? null : index)}
                    className="w-full flex justify-between items-center p-5 text-left hover:bg-gray-50 transition"
                  >
                    <span className="font-semibold text-gray-700">{faq.question}</span>
                    {openIndex === index ? <ChevronUp className="text-blue-600" /> : <ChevronDown className="text-gray-400" />}
                  </button>
                  {openIndex === index && (
                    <div className="p-5 pt-0 text-gray-600 text-sm leading-relaxed border-t border-gray-50">
                      {faq.answer}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Contact Form */}
          <div>
            <h2 className="text-2xl font-bold text-gray-800 mb-6">Gửi yêu cầu hỗ trợ</h2>
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100">
              <form className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Họ và tên</label>
                  <input type="text" className="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 outline-none" placeholder="Nguyễn Văn A" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                  <input type="email" className="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 outline-none" placeholder="email@example.com" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Nội dung cần hỗ trợ</label>
                  <textarea rows={4} className="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 outline-none" placeholder="Mô tả vấn đề của bạn..."></textarea>
                </div>
                <button className="w-full bg-blue-600 text-white font-bold py-3 rounded-lg hover:bg-blue-700 transition flex items-center justify-center gap-2">
                  <Send size={18} /> Gửi yêu cầu
                </button>
              </form>

              <div className="mt-8 pt-6 border-t border-gray-100 space-y-4">
                <div className="flex items-center gap-3 text-gray-600">
                  <Phone className="text-blue-600" size={20} />
                  <span>Hotline: 1900 1234</span>
                </div>
                <div className="flex items-center gap-3 text-gray-600">
                  <Mail className="text-blue-600" size={20} />
                  <span>Email: support@travelk.com</span>
                </div>
                <div className="flex items-center gap-3 text-gray-600">
                  <MapPin className="text-blue-600" size={20} />
                  <span>VP: Tầng 12, Tòa nhà TravelK, Hà Nội</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </main>
      <Footer />
    </div>
  );
};

export default Support;
