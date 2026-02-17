package com.khaiquang.service.impl;

import com.khaiquang.dto.mapper.BusMapper;
import com.khaiquang.dto.request.BusRequestDto;
import com.khaiquang.dto.response.BusResponseDto;
import com.khaiquang.entity.Bus;
import com.khaiquang.entity.Seat;
import com.khaiquang.entity.Station;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.repository.BusRepository;
import com.khaiquang.repository.StationRepository;
import com.khaiquang.service.BusService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final StationRepository stationRepository;
    private final BusMapper busMapper;

    @Override
    public BusResponseDto createBus(BusRequestDto busRequestDto, Long stationId) {
        Station station =  stationRepository.findById(stationId).orElseThrow(() ->
                new ResourceNotFoundException("Station", "id", stationId));
        Bus bus = busMapper.toBusEntity(busRequestDto);
        int totalSeats = bus.getSeats();

        char[] seatColumns =  {'A','B','C','D'};
        int seatPerRows = seatColumns.length;
        int rows = (int)Math.ceil((double) totalSeats / seatPerRows);
        List<Seat> seats = new ArrayList<>();
        int seatCounter = 0;
        for (int i = 1; i <= rows; i++) {
            for(int j = 0; j < seatPerRows; j++) {
                if(seatCounter >= totalSeats)
                    break;
                String seatName = seatColumns[j] + String.valueOf(i);
                Seat seat = Seat.builder()
                        .name(seatName)
                        .bus(bus)
                        .build();
                seats.add(seat);
                seatCounter++;
            }
        }
        bus.setSeatList(seats);
        bus.setStation(station);
        return busMapper.toBusResponseDto(busRepository.save(bus));
    }

    @Override
    public BusResponseDto updateBus(BusRequestDto busRequestDto, Long busId) {
        Bus bus = busRepository.findById(busId).orElseThrow(() ->
                new ResourceNotFoundException("Bus", "id", busId));
        bus.setNumber(String.valueOf((busRequestDto.getNumber()))); // Fix: getNumber
        bus.setSeats(busRequestDto.getSeats());
        bus.setBusType(busRequestDto.getBusType());
        return busMapper.toBusResponseDto(busRepository.save(bus));
    }

    @Override
    public void deleteBus(Long busId) {
        Bus bus = busRepository.findById(busId).orElseThrow(() ->
                new ResourceNotFoundException("Bus", "id", busId));
        bus.setDeleted(true);
        busRepository.save(bus);
    }

    @Override
    public List<BusResponseDto> getAllBusByStationId(Long stationId) {
        if(!stationRepository.existsById(stationId)){
            throw new ResourceNotFoundException("Station", "id", stationId);
        }
        return busRepository.findByStationIdAndDeletedFalse(stationId)
                .stream()
                .map(busMapper::toBusResponseDto)
                .toList();
    }

    @Override
    public List<BusResponseDto> getAllBuses() {
        return busRepository.findByDeletedFalse()
                .stream()
                .map(busMapper::toBusResponseDto)
                .collect(Collectors.toList());
    }
}
