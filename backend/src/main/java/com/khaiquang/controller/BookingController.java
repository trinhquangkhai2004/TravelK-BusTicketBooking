package com.khaiquang.controller;

import com.khaiquang.dto.request.BookingRequestDto;
import com.khaiquang.dto.response.BookingResponseDto;
import com.khaiquang.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/booking")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto bookingRequestDto) {
        BookingResponseDto response = bookingService.createBooking(bookingRequestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới được xóa
    public ResponseEntity<Void> deleteBooking(@PathVariable Long bookingId) {
        bookingService.deleteBooking(bookingId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/trip/{tripId}/seats")
    public ResponseEntity<List<String>> getAllBookedSeats(@PathVariable Long tripId) {
        List<String> seats = bookingService.getAllBookedSeats(tripId);
        return new ResponseEntity<>(seats, HttpStatus.OK);
    }

    @PostMapping("/hold")
    public ResponseEntity<?> holdSeat(@RequestBody Map<String, Object> payload) {
        Long tripId = Long.valueOf(payload.get("tripId").toString());
        String seatNumber = payload.get("seatNumber").toString();

        try {
            bookingService.holdSeat(tripId, seatNumber);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/release")
    public ResponseEntity<?> releaseSeat(@RequestBody Map<String, Object> payload) {
        Long tripId = Long.valueOf(payload.get("tripId").toString());
        String seatNumber = payload.get("seatNumber").toString();

        bookingService.releaseSeat(tripId, seatNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/release-batch")
    public ResponseEntity<?> releaseSeats(@RequestBody Map<String, Object> payload) {
        Long tripId = Long.valueOf(payload.get("tripId").toString());
        List<String> seatNumbers = (List<String>) payload.get("seatNumbers");

        bookingService.releaseSeats(tripId, seatNumbers);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id") // Chỉ xem được booking của chính mình
    public ResponseEntity<List<BookingResponseDto>> getBookingsByUserId(@PathVariable Long userId) {
        return new ResponseEntity<>(bookingService.getBookingsByUserId(userId), HttpStatus.OK);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponseDto>> getAllBookings() {
        return new ResponseEntity<>(bookingService.getAllBookings(), HttpStatus.OK);
    }
}
