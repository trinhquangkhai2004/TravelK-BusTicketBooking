<!DOCTYPE html>
<html>
<head>
    <title>Xác nhận đặt vé</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .container { padding: 20px; border: 1px solid #ddd; border-radius: 5px; max-width: 600px; margin: 0 auto; }
        .header { background-color: #f8f9fa; padding: 10px; text-align: center; border-bottom: 1px solid #ddd; }
        .content { padding: 20px; }
        .footer { text-align: center; font-size: 12px; color: #888; margin-top: 20px; }
        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>Xác nhận đặt vé thành công</h2>
        </div>
        <div class="content">
            <p>Xin chào <strong>${customerName}</strong>,</p>
            <p>Cảm ơn bạn đã đặt vé tại TravelK. Dưới đây là thông tin vé của bạn:</p>
            
            <table>
                <tr>
                    <th>Mã vé:</th>
                    <td>${ticketCode}</td>
                </tr>
                <tr>
                    <th>Chuyến đi:</th>
                    <td>${tripName}</td>
                </tr>
                <tr>
                    <th>Ngày khởi hành:</th>
                    <td>${departureDate} - ${departureTime}</td>
                </tr>
                <tr>
                    <th>Nơi đi:</th>
                    <td>${origin}</td>
                </tr>
                <tr>
                    <th>Nơi đến:</th>
                    <td>${destination}</td>
                </tr>
                <tr>
                    <th>Xe:</th>
                    <td>${busNumber} (${busType})</td>
                </tr>
                <tr>
                    <th>Ghế:</th>
                    <td><strong>${seat}</strong></td>
                </tr>
                <tr>
                    <th>Tổng tiền:</th>
                    <td>${totalPrice}</td>
                </tr>
                <tr>
                    <th>Trạng thái:</th>
                    <td style="color: green;">${paymentStatus}</td>
                </tr>
            </table>
            
            <p>Vui lòng có mặt tại bến xe trước 30 phút để làm thủ tục.</p>
        </div>
        <div class="footer">
            <p>&copy; 2026 TravelK Bus Booking System</p>
        </div>
    </div>
</body>
</html>
