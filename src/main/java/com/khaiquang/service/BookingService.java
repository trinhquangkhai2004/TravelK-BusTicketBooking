package com.khaiquang.service;

import com.khaiquang.dto.request.BookingRequestDto;
import com.khaiquang.dto.response.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto bookingRequestDto);
    void deleteBooking(Long bookingId);
    List<String> getAllBookedSeats(Long tripId);

    void holdSeat(Long tripId, String seatNumber, Long userId);
    void releaseSeat(Long tripId, String seatNumber, Long userId);

    // Thêm phương thức nhả nhiều ghế cùng lúc
    void releaseSeats(Long tripId, List<String> seatNumbers, Long userId);
}
