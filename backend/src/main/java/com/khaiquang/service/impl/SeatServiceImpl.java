package com.khaiquang.service.impl;

import com.khaiquang.entity.Bus;
import com.khaiquang.entity.Seat;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.repository.BusRepository;
import com.khaiquang.repository.SeatRepository;
import com.khaiquang.service.SeatService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {
    private final SeatRepository seatRepository;
    private final BusRepository busRepository;


    @Override
    public List<Seat> getAllSeatsByBusId(Long id) {
        Bus bus = busRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Bus", "id", id));
        List<Seat> seats = seatRepository.findByBusId(bus.getId());
        if(seats.isEmpty()){
            throw new ResourceNotFoundException("Seat", "busId", bus.getId());
        }
        return seats;
    }
}
