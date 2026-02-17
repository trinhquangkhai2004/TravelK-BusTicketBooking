package com.khaiquang.service;

import com.khaiquang.entity.Seat;

import java.util.List;

public interface SeatService {
    List<Seat> getAllSeatsByBusId(Long id);
}
