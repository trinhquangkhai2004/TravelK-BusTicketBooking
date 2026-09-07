package com.khaiquang.exception;

/**
 * Ném ra khi ghế đang bị người khác giữ, đã được bán, hoặc vi phạm ràng buộc
 * unique (trip_id, seat_number) ở tầng DB. Đây là lỗi tranh chấp tài nguyên
 * nên GlobalExceptionHandler map sang HTTP 409 Conflict, không phải 500.
 */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
