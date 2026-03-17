package com.khaiquang.service;

import com.khaiquang.dto.request.TripDto;
import com.khaiquang.dto.response.TripResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface TripService {
    TripResponseDto addTrip(TripDto tripDto);
    TripResponseDto updateTrip(Long tripId, TripDto tripDto);
    void deleteTrip(Long tripId);
    List<TripResponseDto> getALlTripByBusId(Long busId);
    List<TripResponseDto> searchTrips(String origin, String destination, LocalDate date);
    List<TripResponseDto> getAllTrips();
}
