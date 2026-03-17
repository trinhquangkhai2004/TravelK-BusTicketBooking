package com.khaiquang.service.impl;

import com.khaiquang.dto.response.StatisticDto;
import com.khaiquang.repository.TicketRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.repository.UserRepository;
import com.khaiquang.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TicketRepository ticketRepository;

    private static final String KEY_USER_COUNT = "stats:userCount";
    private static final String KEY_TRIP_COUNT = "stats:totalTrips";
    private static final String KEY_TICKET_SOLD = "stats:totalTicketSold";
    private static final String KEY_REVENUE = "stats:revenue";

    @Override
    public StatisticDto getStatistics() {
        Integer userCount = (Integer) redisTemplate.opsForValue().get(KEY_USER_COUNT);
        Integer tripCount = (Integer) redisTemplate.opsForValue().get(KEY_TRIP_COUNT);
        Integer ticketSold = (Integer) redisTemplate.opsForValue().get(KEY_TICKET_SOLD);
        Integer revenue = (Integer) redisTemplate.opsForValue().get(KEY_REVENUE);

        // If any key is missing, initialize from DB
        if (userCount == null) {
            userCount = (int) userRepository.count();
            redisTemplate.opsForValue().set(KEY_USER_COUNT, userCount);
        }
        if (tripCount == null) {
            tripCount = (int) tripRepository.count();
            redisTemplate.opsForValue().set(KEY_TRIP_COUNT, tripCount);
        }
        if (ticketSold == null) {
            ticketSold = ticketRepository.countSoldTickets();
            if (ticketSold == null) ticketSold = 0;
            redisTemplate.opsForValue().set(KEY_TICKET_SOLD, ticketSold);
        }
        if (revenue == null) {
            revenue = ticketRepository.calculateRevenue();
            if (revenue == null) revenue = 0;
            redisTemplate.opsForValue().set(KEY_REVENUE, revenue);
        }

        return new StatisticDto(userCount, ticketSold, tripCount, revenue);
    }

    @Override
    public void incrementUserCount() {
        redisTemplate.opsForValue().increment(KEY_USER_COUNT);
    }

    @Override
    public void incrementTripCount() {
        redisTemplate.opsForValue().increment(KEY_TRIP_COUNT);
    }

    @Override
    public void incrementTicketSold(int count) {
        redisTemplate.opsForValue().increment(KEY_TICKET_SOLD, count);
    }

    @Override
    public void incrementRevenue(long amount) {
        redisTemplate.opsForValue().increment(KEY_REVENUE, amount);
    }
}
