package com.khaiquang.service;

import com.khaiquang.dto.request.BusRequestDto;
import com.khaiquang.dto.response.BusResponseDto;

import java.util.List;

public interface BusService {
    BusResponseDto createBus(BusRequestDto busRequestDto, Long stationId);
    BusResponseDto updateBus(BusRequestDto busRequestDto, Long busId);
    void deleteBus(Long busId);
    List<BusResponseDto> getAllBusByStationId(Long stationId);
    List<BusResponseDto> getAllBuses();
}
