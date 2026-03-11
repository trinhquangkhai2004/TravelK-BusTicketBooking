package com.khaiquang.service;

import com.khaiquang.dto.request.BookingRequestDto;
import com.khaiquang.dto.response.BookingResponseDto;
import com.khaiquang.entity.BookingTrip;
import com.khaiquang.entity.Ticket;
import com.khaiquang.entity.Trip;
import com.khaiquang.entity.User;
import com.khaiquang.repository.BookingRepository;
import com.khaiquang.repository.TicketRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

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

    // ===== Helpers (stub tối thiểu để không bị UnnecessaryStubbing) =====

    private BookingRequestDto mockRequest(long tripId, long userId, List<String> seats) {
        BookingRequestDto dto = mock(BookingRequestDto.class);
        when(dto.getTripId()).thenReturn(tripId);
        when(dto.getUserId()).thenReturn(userId);
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

        List<String> seats = new ArrayList<>(Arrays.asList("A2", "A1")); // service sẽ sort
        BookingRequestDto dto = mockRequest(tripId, userId, seats);

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

        BookingRequestDto dto = mockRequest(tripId, userId, new ArrayList<>(List.of("A1")));
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

        BookingRequestDto dto = mockRequest(tripId, userId, new ArrayList<>(List.of("A1")));
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

    // ==============================================================
    // holdSeat()
    // ==============================================================

    @Test
    void holdSeat_whenAlreadySold_shouldThrow() {
        long tripId = 1L;
        long userId = 10L;
        String seat = "A1";

        // Arrange: case này throw trước khi gọi Redis -> KHÔNG stub redisTemplate.opsForValue()
        when(ticketRepository.existsByTripIdAndSeatNumber(tripId, seat)).thenReturn(true);

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> bookingService.holdSeat(tripId, seat, userId));
        assertTrue(ex.getMessage().contains("đã được bán"));

        verify(valueOperations, never()).setIfAbsent(anyString(), any(), anyLong(), any());
    }

    @Test
    void holdSeat_whenOtherHolds_shouldThrow() {
        long tripId = 1L;
        long userId = 10L;
        String seat = "A1";

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(ticketRepository.existsByTripIdAndSeatNumber(tripId, seat)).thenReturn(false);

        when(valueOperations.setIfAbsent(eq("hold:trip:1:seat:A1"), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        when(valueOperations.get("hold:trip:1:seat:A1")).thenReturn("999");

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> bookingService.holdSeat(tripId, seat, userId));
        assertTrue(ex.getMessage().toLowerCase().contains("người khác"));
    }

    // ==============================================================
    // autoCancelUnpaidBookings()
    // ==============================================================

    @Test
    void autoCancelUnpaidBookings_whenExpired_shouldDeleteTickets_andUpdateStatus() {
        List<Long> expiredIds = List.of(1L, 2L, 3L);

        when(bookingRepository.findByStatusAndCreatedAtBefore(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(expiredIds);

        bookingService.autoCancelUnpaidBookings();

        verify(ticketRepository).deleteByBookingTripIdIn(expiredIds);
        verify(bookingRepository).updateStatusByIds("CANCELLED", expiredIds);
    }
}