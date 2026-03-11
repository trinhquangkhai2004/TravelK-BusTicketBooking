package com.khaiquang.integration;

import com.khaiquang.dto.request.BookingRequestDto;
import com.khaiquang.entity.BookingTrip;
import com.khaiquang.entity.Trip;
import com.khaiquang.entity.User;
import com.khaiquang.repository.BookingRepository;
import com.khaiquang.repository.TicketRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock
    private  BookingRepository bookingRepository;
    @Mock
    private  TicketRepository ticketRepository;
    @Mock
    private  TripRepository tripRepository;
    @Mock
    private  UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @InjectMocks
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setup(){
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private BookingRequestDto mockBookingRequest(Long tripId, Long userId, List<String> seats){
        BookingRequestDto bookingRequestDto = mock(BookingRequestDto.class);
        when(bookingRequestDto.getTripId()).thenReturn(tripId);
        when(bookingRequestDto.getUserId()).thenReturn(userId);
        when(bookingRequestDto.getSeats()).thenReturn(seats);
        return bookingRequestDto;

    }

    private Trip mockTrip(Long tripId){
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(tripId);
        return trip;
    }

    private User mockUser(Long userId){
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUserName()).thenReturn("testUser");
        when(user.getPhoneNumber()).thenReturn("1234567890");
        return user;
    }

    private void createBooking_success_shouldSaveBooking_andReleaseSeat(){
        Long tripId = 1L;
        Long userId = 10L;
        List<String> seats = new ArrayList<>(Arrays.asList("A1", "A2"));
        BookingRequestDto bookingRequestDto = mockBookingRequest(tripId, userId, seats);
        Trip trip = mockTrip(tripId);
        User user = mockUser(userId);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(valueOperations.setIfAbsent(anyString(), eq(String.valueOf(userId)), eq(10L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(ticketRepository.findByTripIdAndSeatNumberIn(tripId, seats)).thenReturn(Collections.emptyList());

        BookingTrip bookingTrip = new BookingTrip();
        
    }

}
