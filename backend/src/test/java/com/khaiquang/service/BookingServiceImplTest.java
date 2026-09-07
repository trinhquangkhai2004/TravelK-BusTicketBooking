package com.khaiquang.service;

import com.khaiquang.dto.request.BookingRequestDto;
import com.khaiquang.dto.response.BookingResponseDto;
import com.khaiquang.entity.BookingTrip;
import com.khaiquang.entity.Ticket;
import com.khaiquang.entity.Trip;
import com.khaiquang.entity.User;
import com.khaiquang.exception.BusAPIException;
import com.khaiquang.exception.SeatUnavailableException;
import com.khaiquang.repository.BookingRepository;
import com.khaiquang.repository.TicketRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.security.CustomUserDetail;
import com.khaiquang.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock BookingRepository bookingRepository;
    @Mock TicketRepository ticketRepository;
    @Mock TripRepository tripRepository;
    @Mock UserRepository userRepository;

    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;

    @InjectMocks BookingServiceImpl bookingService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ===== Helpers (stub tối thiểu để không bị UnnecessaryStubbing) =====

    /** userId giờ lấy từ principal đã xác thực chứ không phải từ request. */
    private void authenticateAs(long userId) {
        CustomUserDetail principal = new CustomUserDetail(userId, "khai", "secret", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private BookingRequestDto mockRequest(long tripId, List<String> seats) {
        BookingRequestDto dto = mock(BookingRequestDto.class);
        when(dto.getTripId()).thenReturn(tripId);
        when(dto.getSeats()).thenReturn(seats);
        return dto;
    }

    private Trip mockTripBasic(long id) {
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(id);
        return trip;
    }

    private Trip mockTripWithPrice(long id, BigDecimal price) {
        Trip trip = mockTripBasic(id);
        when(trip.getPrice()).thenReturn(price);
        return trip;
    }

    private User mockUserBasic(long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private User mockUserWithProfile(long id, String username, String phone) {
        User user = mockUserBasic(id);
        when(user.getUserName()).thenReturn(username);
        when(user.getPhoneNumber()).thenReturn(phone);
        return user;
    }

    private static List<Ticket> toList(Iterable<Ticket> it) {
        List<Ticket> out = new ArrayList<>();
        if (it != null) it.forEach(out::add);
        return out;
    }

    // ==============================================================
    // createBooking()
    // ==============================================================

    @Test
    void createBooking_success_shouldSaveBookingTickets_andReleaseLocks() {
        long tripId = 1L;
        long userId = 10L;

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations); // stub Redis ONLY in tests that need it

        authenticateAs(userId);

        List<String> seats = new ArrayList<>(Arrays.asList("A2", "A1")); // service sẽ sort
        BookingRequestDto dto = mockRequest(tripId, seats);

        BigDecimal price = BigDecimal.valueOf(100_000);
        Trip trip = mockTripWithPrice(tripId, price);
        User user = mockUserWithProfile(userId, "khai", "0123456789");

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(valueOperations.setIfAbsent(anyString(), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        when(ticketRepository.findByTripIdAndSeatNumberIn(eq(tripId), anyList()))
                .thenReturn(Collections.emptyList());

        BookingTrip saved = new BookingTrip();
        saved.setId(999L);
        when(bookingRepository.save(any(BookingTrip.class))).thenReturn(saved);

        // Act
        BookingResponseDto res = bookingService.createBooking(dto);

        // Assert - booking saved đúng
        verify(bookingRepository).save(argThat(b ->
                b.getTrip() == trip &&
                        b.getUser() == user &&
                        "PENDING".equals(b.getStatus())
        ));

        // Assert - tickets saved đúng
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Iterable> ticketCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(ticketRepository).saveAll(ticketCaptor.capture());

        @SuppressWarnings("unchecked")
        List<Ticket> savedTickets = toList((Iterable<Ticket>) ticketCaptor.getValue());

        assertEquals(2, savedTickets.size());
        assertTrue(savedTickets.stream().allMatch(t ->
                t.getTrip() == trip &&
                        t.getBookingTrip() == saved &&
                        price.compareTo(t.getPrice()) == 0
        ));

        // Assert - release lock 2 lần (A1 & A2)
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> keyCaptor = ArgumentCaptor.forClass(List.class);

        verify(redisTemplate, times(2))
                .execute(any(DefaultRedisScript.class), keyCaptor.capture(), eq(String.valueOf(userId)));

        List<String> allKeys = new ArrayList<>();
        for (List keysList : keyCaptor.getAllValues()) {
            for (Object k : keysList) allKeys.add(String.valueOf(k));
        }

        assertTrue(allKeys.contains("hold:trip:1:seat:A1"));
        assertTrue(allKeys.contains("hold:trip:1:seat:A2"));

        // Assert - response (tuỳ BookingResponseDto của bạn)
        assertEquals(999L, res.getId());
        assertEquals("PENDING", res.getStatus());
        assertEquals(userId, res.getUserId());
        assertEquals(tripId, res.getTripId());
    }

    @Test
    void createBooking_whenSeatLockedByOther_shouldThrow_andNotSaveAnything() {
        long tripId = 1L;
        long userId = 10L;

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authenticateAs(userId);

        BookingRequestDto dto = mockRequest(tripId, new ArrayList<>(List.of("A1")));
        Trip trip = mockTripBasic(tripId);
        User user = mockUserBasic(userId);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(valueOperations.setIfAbsent(anyString(), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        when(valueOperations.get("hold:trip:1:seat:A1")).thenReturn("999");

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> bookingService.createBooking(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("ghế"));

        verify(bookingRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());
        // Không release lock vì chưa lock được
        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    void createBooking_whenSeatAlreadySold_shouldThrow_andNotSaveAnything() {
        long tripId = 1L;
        long userId = 10L;

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authenticateAs(userId);

        BookingRequestDto dto = mockRequest(tripId, new ArrayList<>(List.of("A1")));
        Trip trip = mockTripBasic(tripId);
        User user = mockUserBasic(userId);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(valueOperations.setIfAbsent(anyString(), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        Ticket sold = mock(Ticket.class);
        when(sold.getSeatNumber()).thenReturn("A1");

        when(ticketRepository.findByTripIdAndSeatNumberIn(eq(tripId), anyList()))
                .thenReturn(List.of(sold));

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> bookingService.createBooking(dto));

        // code bạn có typo "đươc" nên mình check cả 2
        String msg = ex.getMessage();
        assertTrue(msg.contains("đã đươc bán") || msg.contains("đã được bán"));

        verify(bookingRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());
    }

    @Test
    void createBooking_whenLockFailsPartway_shouldReleaseLocksAlreadyAcquired() {
        long tripId = 1L;
        long userId = 10L;

        // Arrange: A1 lock được, A2 bị người khác giữ
        authenticateAs(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        BookingRequestDto dto = mockRequest(tripId, new ArrayList<>(Arrays.asList("A1", "A2")));
        Trip trip = mockTripBasic(tripId);
        User user = mockUserBasic(userId);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(valueOperations.setIfAbsent(eq("hold:trip:1:seat:A1"), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(eq("hold:trip:1:seat:A2"), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(false);
        when(valueOperations.get("hold:trip:1:seat:A2")).thenReturn("999");

        // Act + Assert
        assertThrows(RuntimeException.class, () -> bookingService.createBooking(dto));

        verify(bookingRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());

        // Lock của A1 phải được trả lại, không để rò rỉ 10 phút
        verify(redisTemplate, times(1)).execute(
                any(DefaultRedisScript.class),
                eq(List.of("hold:trip:1:seat:A1")),
                eq(String.valueOf(userId)));
    }

    /**
     * Lock Redis chỉ được trả SAU khi transaction kết thúc. Nếu trả trước lúc commit,
     * request khác chiếm được lock rồi ghi vé trong lúc transaction này chưa commit -> double booking.
     */
    @Test
    void createBooking_whenTransactionActive_shouldReleaseLocksOnlyAfterTransactionCompletes() {
        long tripId = 1L;
        long userId = 10L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authenticateAs(userId);

        BookingRequestDto dto = mockRequest(tripId, new ArrayList<>(Arrays.asList("A2", "A1")));
        Trip trip = mockTripWithPrice(tripId, BigDecimal.valueOf(100_000));
        User user = mockUserWithProfile(userId, "khai", "0123456789");

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(valueOperations.setIfAbsent(anyString(), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(ticketRepository.findByTripIdAndSeatNumberIn(eq(tripId), anyList()))
                .thenReturn(Collections.emptyList());

        BookingTrip saved = new BookingTrip();
        saved.setId(999L);
        when(bookingRepository.save(any(BookingTrip.class))).thenReturn(saved);

        TransactionSynchronizationManager.initSynchronization();
        try {
            bookingService.createBooking(dto);

            // Transaction chưa kết thúc -> tuyệt đối chưa được trả lock
            verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any());

            List<TransactionSynchronization> syncs =
                    new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
            assertEquals(1, syncs.size(), "Phải đăng ký đúng 1 synchronization để trả lock");

            // Mô phỏng transaction commit xong
            syncs.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

            verify(redisTemplate, times(2))
                    .execute(any(DefaultRedisScript.class), anyList(), eq(String.valueOf(userId)));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** Nhánh "ghế đã được bán" trước đây throw mà không trả lock -> rò lock 10 phút. */
    @Test
    void createBooking_whenSeatAlreadySold_shouldStillReleaseAcquiredLocks() {
        long tripId = 1L;
        long userId = 10L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authenticateAs(userId);

        BookingRequestDto dto = mockRequest(tripId, new ArrayList<>(List.of("A1")));
        Trip trip = mockTripBasic(tripId);
        User user = mockUserBasic(userId);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(valueOperations.setIfAbsent(anyString(), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);

        Ticket sold = mock(Ticket.class);
        when(sold.getSeatNumber()).thenReturn("A1");
        when(ticketRepository.findByTripIdAndSeatNumberIn(eq(tripId), anyList())).thenReturn(List.of(sold));

        assertThrows(SeatUnavailableException.class, () -> bookingService.createBooking(dto));

        verify(redisTemplate, times(1)).execute(
                any(DefaultRedisScript.class),
                eq(List.of("hold:trip:1:seat:A1")),
                eq(String.valueOf(userId)));
    }

    /** Vi phạm unique (trip_id, seat_number) phải thành lỗi tranh chấp (409), không phải 500. */
    @Test
    void createBooking_whenUniqueConstraintViolated_shouldThrowSeatUnavailable() {
        long tripId = 1L;
        long userId = 10L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authenticateAs(userId);

        BookingRequestDto dto = mockRequest(tripId, new ArrayList<>(List.of("A1")));
        Trip trip = mockTripWithPrice(tripId, BigDecimal.valueOf(100_000));
        User user = mockUserBasic(userId);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(valueOperations.setIfAbsent(anyString(), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(ticketRepository.findByTripIdAndSeatNumberIn(eq(tripId), anyList()))
                .thenReturn(Collections.emptyList());

        BookingTrip saved = new BookingTrip();
        saved.setId(999L);
        when(bookingRepository.save(any(BookingTrip.class))).thenReturn(saved);

        // DB chặn ở lần flush: ghế vừa bị người khác ghi trước
        doThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_tickets_trip_seat'"))
                .when(ticketRepository).flush();

        SeatUnavailableException ex =
                assertThrows(SeatUnavailableException.class, () -> bookingService.createBooking(dto));
        assertTrue(ex.getMessage().contains("A1"));

        // Lock vẫn phải được trả lại
        verify(redisTemplate, times(1)).execute(
                any(DefaultRedisScript.class),
                eq(List.of("hold:trip:1:seat:A1")),
                eq(String.valueOf(userId)));
    }

    // ==============================================================
    // holdSeat()
    // ==============================================================

    @Test
    void holdSeat_whenAlreadySold_shouldThrow() {
        long tripId = 1L;
        long userId = 10L;
        String seat = "A1";

        // Arrange: case này throw trước khi gọi Redis -> KHÔNG stub redisTemplate.opsForValue()
        authenticateAs(userId);
        when(ticketRepository.existsByTripIdAndSeatNumber(tripId, seat)).thenReturn(true);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> bookingService.holdSeat(tripId, seat));
        assertTrue(ex.getMessage().contains("đã được bán"));

        verify(valueOperations, never()).setIfAbsent(anyString(), any(), anyLong(), any());
    }

    @Test
    void holdSeat_whenOtherHolds_shouldThrow() {
        long tripId = 1L;
        long userId = 10L;
        String seat = "A1";

        // Arrange
        authenticateAs(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(ticketRepository.existsByTripIdAndSeatNumber(tripId, seat)).thenReturn(false);

        when(valueOperations.setIfAbsent(eq("hold:trip:1:seat:A1"), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        when(valueOperations.get("hold:trip:1:seat:A1")).thenReturn("999");

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> bookingService.holdSeat(tripId, seat));
        assertTrue(ex.getMessage().toLowerCase().contains("người khác"));
    }

    @Test
    void holdSeat_whenNotAuthenticated_shouldThrow() {
        // Không set SecurityContext -> không được phép giữ ghế
        BusAPIException ex = assertThrows(BusAPIException.class, () -> bookingService.holdSeat(1L, "A1"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());

        verifyNoInteractions(ticketRepository, redisTemplate);
    }

    // ==============================================================
    // autoCancelUnpaidBookings()
    // ==============================================================

    @Test
    void autoCancelUnpaidBookings_whenExpired_shouldDeleteTickets_andUpdateStatus() {
        BookingTrip b1 = new BookingTrip();
        b1.setId(1L);

        BookingTrip b2 = new BookingTrip();
        b2.setId(2L);

        BookingTrip b3 = new BookingTrip();
        b3.setId(3L);

        List<BookingTrip> expiredBookings = List.of(b1, b2, b3);
        List<Long> expiredIds = List.of(1L, 2L, 3L);

        when(bookingRepository.findByStatusAndCreatedAtBefore(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        bookingService.autoCancelUnpaidBookings();

        verify(ticketRepository).deleteByBookingTripIdIn(expiredIds);
        verify(bookingRepository).updateStatusByIds("CANCELLED", expiredIds);
    }
}