package com.khaiquang.service.impl;

import com.khaiquang.config.VnPayConfig;
import com.khaiquang.dto.message.EmailContent;
import com.khaiquang.dto.message.OrderEvent;
import com.khaiquang.dto.response.VnPayResponse;
import com.khaiquang.entity.BookingTrip;
import com.khaiquang.entity.Ticket;
import com.khaiquang.repository.BookingRepository;
import com.khaiquang.repository.TicketRepository;
import com.khaiquang.service.PaymentService;
import com.khaiquang.service.StatisticService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final VnPayConfig vnPayConfig;
    private final BookingRepository bookingRepository;
    private final StatisticService statisticService;
    private final TicketRepository ticketRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.binding.routing.key}")
    private String routingKey;

    @Override
    public VnPayResponse createOrder(HttpServletRequest request, long amount, String bankCode, Long bookingId) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        
        vnp_Params.put("vnp_CurrCode", "VND");

        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }

        String vnp_TxnRef = String.valueOf(bookingId);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan ve xe don hang #" + vnp_TxnRef);
        
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnPayConfig.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;

        return VnPayResponse.builder()
                .code("ok")
                .message("success")
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public int returnOrder(HttpServletRequest request){
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        String signValue = hashAllFields(fields);

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                String txnRef = request.getParameter("vnp_TxnRef");
                String amountStr = request.getParameter("vnp_Amount");
                long amount = Long.parseLong(amountStr) / 100;

                statisticService.incrementRevenue(amount);
                
                try {
                    Long bookingId = Long.parseLong(txnRef);
                    BookingTrip booking = bookingRepository.findById(bookingId).orElse(null);
                    
                    if (booking != null) {
                        booking.setStatus("PAID");
                        bookingRepository.save(booking);
                        
                        List<Ticket> tickets = ticketRepository.findByTripId(booking.getTrip().getId());
                        List<String> seats = tickets.stream()
                                .filter(t -> t.getBookingTrip().getId().equals(bookingId))
                                .map(Ticket::getSeatNumber)
                                .collect(Collectors.toList());
                        
                        statisticService.incrementTicketSold(seats.size());

                        OrderEvent event = new OrderEvent();
                        event.setStatus("PAID");
                        event.setMessage("Payment successful");
                        
                        EmailContent content = new EmailContent();
                        content.setDesEmail(booking.getUser().getEmail());
                        content.setCustomerName(booking.getUser().getUserName());
                        content.setTickerCode("TICKET-" + bookingId);
                        content.setBookingDate(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                        content.setTripName(booking.getTrip().getOrigin() + " - " + booking.getTrip().getDestination());
                        content.setDepartureDate(booking.getTrip().getDepartureDate().toString());
                        content.setDepartureTime(booking.getTrip().getDepartureTime().toString());
                        content.setOrigin(booking.getTrip().getOrigin());
                        content.setDestination(booking.getTrip().getDestination());
                        content.setBusNumber(booking.getTrip().getBus().getNumber());
                        content.setBusType(booking.getTrip().getBus().getBusType());
                        content.setSeats(seats);
                        content.setTotalPrice(String.format("%,d VND", amount));
                        content.setPaymentStatus("Đã thanh toán");
                        
                        event.setContent(content);
                        
                        rabbitTemplate.convertAndSend(exchange, routingKey, event);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Booking ID in TxnRef: " + txnRef);
                }

                return 1; 
            } else {
                return 0; 
            }
        } else {
            return -1; 
        }
    }

    private String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                try {
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return vnPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), sb.toString());
    }
}
