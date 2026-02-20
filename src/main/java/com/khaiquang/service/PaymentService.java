package com.khaiquang.service;

import com.khaiquang.dto.request.VnPayRequest;
import com.khaiquang.dto.response.VnPayResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public interface PaymentService {
    VnPayResponse createOrder(HttpServletRequest request, long amount, String bankCode, Long bookingId);
    int returnOrder(HttpServletRequest request);
}
