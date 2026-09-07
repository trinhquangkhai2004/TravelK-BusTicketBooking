package com.khaiquang.service.impl;

import com.khaiquang.dto.request.BookingRequestDto;
import com.khaiquang.dto.response.BookingResponseDto;
import com.khaiquang.entity.BookingTrip;
import com.khaiquang.entity.Ticket;
import com.khaiquang.entity.Trip;
import com.khaiquang.entity.User;
import com.khaiquang.exception.BusAPIException;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.exception.SeatUnavailableException;
import com.khaiquang.repository.BookingRepository;
import com.khaiquang.repository.TicketRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.security.CustomUserDetail;
import com.khaiquang.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        // userId luôn lấy từ token, không nhận từ client
        Long currentUserId = getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("user", "id", currentUserId));

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
                    // Trả lại toàn bộ lock đã giữ được trước đó để không khoá nhầm ghế
                    releaseLocks(successfullyLockedKeys, userId);
                    throw new SeatUnavailableException("Ghế " + seatNum + " đang có người khác giữ hoặc thao tác!");
                }
            }
        }

        // finally đảm bảo lock luôn được trả lại ở MỌI nhánh thoát (kể cả nhánh ghế đã bán),
        // và chỉ được trả lại SAU khi transaction kết thúc - xem scheduleReleaseLocks.
        try {
            List<Ticket> existingTickets = ticketRepository.findByTripIdAndSeatNumberIn(trip.getId(), requestedSeats);

            if (!existingTickets.isEmpty()) {
                List<String> soldSeats = existingTickets.stream().map(Ticket::getSeatNumber).collect(Collectors.toList());
                throw new SeatUnavailableException("Rất tiếc, các ghế trên đã đươc bán: " + String.join(", ", soldSeats));
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
            try {
                ticketRepository.saveAll(tickets);
                // Ép ghi xuống DB ngay để bắt vi phạm unique (trip_id, seat_number) tại đây,
                // thay vì để nó nổ lúc commit và biến thành lỗi 500.
                ticketRepository.flush();
            } catch (DataIntegrityViolationException ex) {
                throw new SeatUnavailableException(
                        "Rất tiếc, các ghế trên vừa có người khác đặt: " + String.join(", ", requestedSeats));
            }

            return new BookingResponseDto(
                    savedBooking.getId(),
                    "PENDING",
                    user.getId(),
                    trip.getId(),
                    user.getUserName(),
                    user.getPhoneNumber()
            );
        } finally {
            scheduleReleaseLocks(successfullyLockedKeys, userId);
        }

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
    public void holdSeat(Long tripId, String seatNumber) {
        Long userId = getCurrentUserId();

        if (ticketRepository.existsByTripIdAndSeatNumber(tripId, seatNumber)) {
            throw new SeatUnavailableException("Ghế đã được bán.");
        }

        String key = generateHoldKey(tripId, seatNumber);
        
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId.toString(), HOLD_TIMEOUT, TimeUnit.MINUTES);
                
        if (Boolean.FALSE.equals(success)) {
            Object currentHolder = redisTemplate.opsForValue().get(key);
            if (currentHolder != null && currentHolder.toString().equals(userId.toString())) {
                return;
            }
            throw new SeatUnavailableException("Ghế đang được người khác giữ.");
        }
    }

    @Override
    public void releaseSeat(Long tripId, String seatNumber) {
        Long userId = getCurrentUserId();
        String key = generateHoldKey(tripId, seatNumber);
        releaseLocks(Collections.singletonList(key), userId.toString());
    }

    @Override
    public void releaseSeats(Long tripId, List<String> seatNumbers) {
        Long userId = getCurrentUserId();
        List<String> keys = seatNumbers.stream()
                .map(seatNumber -> generateHoldKey(tripId, seatNumber))
                .collect(Collectors.toList());
        releaseLocks(keys, userId.toString());
    }

    private String generateHoldKey(Long tripId, String seatNumber) {
        return HOLD_KEY_PREFIX + tripId + ":seat:" + seatNumber;
    }

    // Hoãn việc trả lock tới SAU khi transaction kết thúc (commit hoặc rollback).
    // Nếu trả lock trước lúc commit, request khác chiếm được lock rồi ghi vé trong khi
    // transaction hiện tại chưa commit -> cửa sổ race rộng bằng thời gian commit.
    // Dùng afterCompletion (thay vì afterCommit) để lock cũng được trả khi rollback, tránh rò lock 10 phút.
    private void scheduleReleaseLocks(List<String> keys, String userId) {
        if (keys.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            List<String> keysToRelease = new ArrayList<>(keys);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseLocks(keysToRelease, userId);
                }
            });
        } else {
            // Không có transaction đang chạy (ví dụ gọi trực tiếp service): trả lock ngay
            releaseLocks(keys, userId);
        }
    }

    // Chỉ xoá lock nếu chính user này đang giữ (compare-and-delete)
    private void releaseLocks(List<String> keys, String userId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
        for (String lockKey : keys) {
            redisTemplate.execute(script, Collections.singletonList(lockKey), userId);
        }
    }

    // Lấy userId từ principal đã xác thực, không bao giờ từ input của client
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetail userDetail)) {
            throw new BusAPIException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để thực hiện thao tác này!");
        }
        return userDetail.getId();
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
