package com.khaiquang.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VnPayRequest {
    private long amount;
    private Long bookingId;
    private String bankCode;
}
