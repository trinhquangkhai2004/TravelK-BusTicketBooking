package com.khaiquang.dto.message;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EmailContent {
    private String desEmail;
    private String customerName;
    private String tickerCode;
    private String bookingDate;
    private String tripName;
    private String departureDate;
    private String departureTime;
    private String origin;
    private String destination;
    private String busNumber;
    private String busType;
    private List<String> seats;
    private String totalPrice;
    private String paymentStatus;
}
