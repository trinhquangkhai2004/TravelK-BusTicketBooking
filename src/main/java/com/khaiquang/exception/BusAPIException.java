package com.khaiquang.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BusAPIException extends RuntimeException {
    private HttpStatus httpStatus;
    private String message;
}
