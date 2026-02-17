package com.khaiquang.service.impl;

import com.khaiquang.dto.request.BookingRequestDto;
import com.khaiquang.dto.response.BookingResponseDto;
import com.khaiquang.entity.BookingTrip;
import com.khaiquang.entity.Ticket;
import com.khaiquang.entity.Trip;
import com.khaiquang.entity.User;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.repository.BookingRepository;
import com.khaiquang.repository.TicketRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String HOLD_KEY_PREFIX = "hold:trip:";
    private static final long HOLD_TIMEOUT = 1; // 5 phút

    @Override
    public BookingResponseDto createBooking(BookingRequestDto bookingRequestDto) {
        Trip trip = tripRepository.findById(bookingRequestDto.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("trip", "id", bookingRequestDto.getTripId()));

        User user = userRepository.findById(bookingRequestDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user", "id", bookingRequestDto.getUserId()));

        List<String> requestedSeats = bookingRequestDto.getSeats();
        
        for (String seatNum : requestedSeats) {
            String holdKey = generateHoldKey(trip.getId(), seatNum);
            Object holderId = redisTemplate.opsForValue().get(holdKey);
            
            if (holderId != null && !holderId.toString().equals(user.getId().toString())) {
                throw new RuntimeException("Ghế " + seatNum + " đang được giữ bởi người khác.");
            }
            
            if (holderId == null) {
                 if (ticketRepository.existsByTripIdAndSeatNumber(trip.getId(), seatNum)) {
                    throw new RuntimeException("Ghế " + seatNum + " đã bị bán.");
                }
            }
        }

        BookingTrip bookingTrip = new BookingTrip();
        bookingTrip.setTrip(trip);
        bookingTrip.setUser(user);
        bookingTrip.setStatus("PENDING");
        BookingTrip savedBooking = bookingRepository.save(bookingTrip);

        List<Ticket> tickets = new ArrayList<>();
        BigDecimal ticketPrice = trip.getPrice();
        
        for (String seatNum : requestedSeats) {
            Ticket ticket = Ticket.builder()
                    .seatNumber(seatNum)
                    .price(ticketPrice)
                    .bookingTrip(savedBooking)
                    .trip(trip)
                    .build();
            tickets.add(ticket);
        }
        ticketRepository.saveAll(tickets);
        
        for (String seatNum : requestedSeats) {
            redisTemplate.delete(generateHoldKey(trip.getId(), seatNum));
        }

        return new BookingResponseDto(
                savedBooking.getId(),
                "PENDING", // Trả về trạng thái Pending
                user.getId(),
                trip.getId(),
                user.getUserName(),
                user.getPhoneNumber()
        );
    }

    @Override
    public void deleteBooking(Long bookingId) {
        BookingTrip booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        
        ticketRepository.deleteByBookingTripId(bookingId);
        bookingRepository.delete(booking);
    }

    @Override
    public List<String> getAllBookedSeats(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
             throw new ResourceNotFoundException("Trip", "id", tripId);
        }
        
        List<String> bookedSeats = ticketRepository.findByTripId(tripId).stream()
                .map(Ticket::getSeatNumber)
                .collect(Collectors.toList());
                
        String pattern = HOLD_KEY_PREFIX + tripId + ":seat:*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null) {
            for (String key : keys) {
                String[] parts = key.split(":");
                String seatNum = parts[parts.length - 1];
                if (!bookedSeats.contains(seatNum)) {
                    bookedSeats.add(seatNum);
                }
            }
        }

        return bookedSeats;
    }

    @Override
    public void holdSeat(Long tripId, String seatNumber, Long userId) {
        if (ticketRepository.existsByTripIdAndSeatNumber(tripId, seatNumber)) {
            throw new RuntimeException("Ghế đã được bán.");
        }

        String key = generateHoldKey(tripId, seatNumber);
        
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId.toString(), HOLD_TIMEOUT, TimeUnit.MINUTES);
                
        if (Boolean.FALSE.equals(success)) {
            Object currentHolder = redisTemplate.opsForValue().get(key);
            if (currentHolder != null && currentHolder.toString().equals(userId.toString())) {
                return;
            }
            throw new RuntimeException("Ghế đang được người khác giữ.");
        }
    }

    @Override
    public void releaseSeat(Long tripId, String seatNumber, Long userId) {
        String key = generateHoldKey(tripId, seatNumber);
        Object currentHolder = redisTemplate.opsForValue().get(key);
        
        if (currentHolder != null && currentHolder.toString().equals(userId.toString())) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public void releaseSeats(Long tripId, List<String> seatNumbers, Long userId) {
        for (String seatNumber : seatNumbers) {
            releaseSeat(tripId, seatNumber, userId);
        }
    }

    private String generateHoldKey(Long tripId, String seatNumber) {
        return HOLD_KEY_PREFIX + tripId + ":seat:" + seatNumber;
    }
    

    @Scheduled(fixedRate = 60000)
    public void autoCancelUnpaidBookings() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(1); // 1 phút
        
        // Cần thêm method findByStatusAndCreatedAtBefore trong Repository
        List<BookingTrip> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore("PENDING", expirationTime);
        
        for (BookingTrip booking : expiredBookings) {
            ticketRepository.deleteByBookingTripId(booking.getId());
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            System.out.println("Auto-cancelled booking ID: " + booking.getId());
        }
    }
}
