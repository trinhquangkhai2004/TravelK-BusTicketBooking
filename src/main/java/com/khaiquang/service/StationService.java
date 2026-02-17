package com.khaiquang.service;

import com.khaiquang.dto.request.StationRequestDto;
import com.khaiquang.dto.request.StationUpdateRequest;
import com.khaiquang.dto.response.StationAndTripResponseDto;
import com.khaiquang.dto.response.StationResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface StationService {
    StationResponseDto createStation(StationRequestDto stationRequestDto);
    StationResponseDto updateStation(StationUpdateRequest dto, Long id);
    List<StationResponseDto> getAllStations();
    StationResponseDto getStationById(Long id);
    void deleteStation(Long id);
}
