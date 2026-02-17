package com.khaiquang.dto.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderEvent {
    private String status;
    private String message;
    private EmailContent content;
}
