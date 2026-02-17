package com.khaiquang.service;

import com.khaiquang.dto.response.StatisticDto;

public interface StatisticService {
    StatisticDto getStatistics();
    void incrementUserCount();
    void incrementTripCount();
    void incrementTicketSold(int count);
    void incrementRevenue(long amount);
}
