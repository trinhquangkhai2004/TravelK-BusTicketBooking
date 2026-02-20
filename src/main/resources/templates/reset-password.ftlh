<!DOCTYPE html>
<html>
<head>
    <title>Đặt lại mật khẩu</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .container { padding: 20px; border: 1px solid #ddd; border-radius: 5px; max-width: 600px; margin: 0 auto; }
        .header { background-color: #f8f9fa; padding: 10px; text-align: center; border-bottom: 1px solid #ddd; }
        .content { padding: 20px; text-align: center; }
        .btn { display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
        .footer { text-align: center; font-size: 12px; color: #888; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>Yêu cầu đặt lại mật khẩu</h2>
        </div>
        <div class="content">
            <p>Xin chào <strong>${userName}</strong>,</p>
            <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
            <p>Vui lòng nhấn vào nút bên dưới để đặt lại mật khẩu (Link có hiệu lực trong 15 phút):</p>
            
            <a href="${resetLink}" class="btn">Đặt lại mật khẩu</a>
            
            <p style="margin-top: 20px; font-size: 13px; color: #666;">Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>
        </div>
        <div class="footer">
            <p>&copy; 2026 TravelK Bus Booking System</p>
        </div>
    </div>
</body>
</html>
