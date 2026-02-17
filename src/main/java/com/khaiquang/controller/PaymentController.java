package com.khaiquang.controller;

import com.khaiquang.dto.request.VnPayRequest;
import com.khaiquang.dto.response.VnPayResponse;
import com.khaiquang.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/vn-pay")
    public ResponseEntity<?> createPayment(HttpServletRequest request,
                                           @RequestBody VnPayRequest paymentDTO) {
        // Truyền thêm paymentDTO.getBookingId() vào service
        VnPayResponse res = paymentService.createOrder(
                request, 
                paymentDTO.getAmount(), 
                paymentDTO.getBankCode(), 
                paymentDTO.getBookingId()
        );
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/vnpay-return")
    public void returnOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Xử lý logic thanh toán (cập nhật DB, gửi mail...)
        paymentService.returnOrder(request);
        
        // Lấy các tham số từ VNPay để chuyển tiếp về Frontend
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
        String vnp_Amount = request.getParameter("vnp_Amount");
        
        // Chuyển hướng về trang kết quả của Frontend
        String redirectUrl = "http://localhost:3000/booking-success" +
                             "?vnp_TxnRef=" + vnp_TxnRef +
                             "&vnp_ResponseCode=" + vnp_ResponseCode +
                             "&vnp_TransactionNo=" + vnp_TransactionNo +
                             "&vnp_Amount=" + vnp_Amount;
                             
        response.sendRedirect(redirectUrl);
    }
}
