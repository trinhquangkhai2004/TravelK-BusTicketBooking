package com.khaiquang.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VnPayResponse {
    public String code;
    public String message;
    public String paymentUrl;
}
