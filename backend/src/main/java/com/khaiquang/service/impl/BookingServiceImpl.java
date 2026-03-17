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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String HOLD_KEY_PREFIX = "hold:trip:";
    private static final long HOLD_TIMEOUT = 10; // 10 phút

    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "   return redis.call('del', KEYS[1]) " +
                    "else " +
                    "   return 0 " +

                    "end";
    @Transactional
    @Override
    public BookingResponseDto createBooking(BookingRequestDto bookingRequestDto) {
        Trip trip = tripRepository.findById(bookingRequestDto.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("trip", "id", bookingRequestDto.getTripId()));

        User user = userRepository.findById(bookingRequestDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user", "id", bookingRequestDto.getUserId()));

        List<String> requestedSeats = bookingRequestDto.getSeats();
        Collections.sort(requestedSeats);


        List<String> successfullyLockedKeys = new ArrayList<>();
        String userId = user.getId().toString();

        // Lấy Lock nhiều ghế
        
        for (String seatNum : requestedSeats) {
            String holdKey = generateHoldKey(trip.getId(), seatNum);
            Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(holdKey, userId, HOLD_TIMEOUT, TimeUnit.MINUTES);

            if (Boolean.TRUE.equals(isLocked)) {
                successfullyLockedKeys.add(holdKey);
            } else {
                Object currentHolder = redisTemplate.opsForValue().get(holdKey);
                if (currentHolder != null && currentHolder.toString().equals(userId)) {
                    successfullyLockedKeys.add(holdKey);
                } else {
                    throw new RuntimeException("Ghế " + seatNum + " đang có người khác giữ hoặc thao tác!");
                }
            }
        }

        List<Ticket> existingTickets = ticketRepository.findByTripIdAndSeatNumberIn(trip.getId(), requestedSeats);

        if (!existingTickets.isEmpty()) {
            List<String> soldSeats = existingTickets.stream().map(Ticket::getSeatNumber).collect(Collectors.toList());
            throw new RuntimeException("Rất tiếc, các ghế trên đã đươc bán: " + String.join(", ", soldSeats));
        }


        BookingTrip bookingTrip = new BookingTrip();
        bookingTrip.setTrip(trip);
        bookingTrip.setUser(user);
        bookingTrip.setStatus("PENDING");
        BookingTrip savedBooking = bookingRepository.save(bookingTrip);

        List<Ticket> tickets = requestedSeats.stream().map(seatNum ->
                Ticket.builder()
                        .seatNumber(seatNum)
                        .price(trip.getPrice())
                        .bookingTrip(savedBooking)
                        .trip(trip)
                        .build())
                .collect(Collectors.toList());
        ticketRepository.saveAll(tickets);
        
        // Xóa Lock sau khi lưu thành công
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
        for(String lockKey : successfullyLockedKeys){
            redisTemplate.execute(script, Collections.singletonList(lockKey), userId);
        }

        return new BookingResponseDto(
                savedBooking.getId(),
                "PENDING",
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
        
        // Sửa tên phương thức
        ticketRepository.deleteByBookingTripId(bookingId);
        bookingRepository.delete(booking);
    }

    @Override
    public List<String> getAllBookedSeats(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
             throw new ResourceNotFoundException("Trip", "id", tripId);
        }

        return ticketRepository.findByTripId(tripId).stream()
                .map(Ticket::getSeatNumber)
                .collect(Collectors.toList());
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
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
        redisTemplate.execute(script, Collections.singletonList(key), userId.toString());
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
    
    @Override
    public List<BookingResponseDto> getBookingsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return bookingRepository.findByUserId(userId).stream()
                .map(booking -> new BookingResponseDto(
                        booking.getId(),
                        booking.getStatus(),
                        booking.getUser().getId(),
                        booking.getTrip().getId(),
                        booking.getUser().getUserName(),
                        booking.getUser().getPhoneNumber()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BookingResponseDto> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(booking -> new BookingResponseDto(
                        booking.getId(),
                        booking.getStatus(),
                        booking.getUser().getId(),
                        booking.getTrip().getId(),
                        booking.getUser().getUserName(),
                        booking.getUser().getPhoneNumber()
                ))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCancelUnpaidBookings() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);
        
        List<BookingTrip> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore("PENDING", expirationTime);
        
        if (!expiredBookings.isEmpty()) {
            List<Long> expiredBookingIds = expiredBookings.stream()
                            .map(BookingTrip::getId).toList();

            ticketRepository.deleteByBookingTripIdIn(expiredBookingIds);
            bookingRepository.updateStatusByIds("CANCELLED", expiredBookingIds);
            System.out.println("Auto-cancelled booking ID: " + expiredBookingIds.size());
        }
    }
}
