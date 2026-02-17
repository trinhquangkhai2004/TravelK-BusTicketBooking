package com.khaiquang.service.impl;

import com.khaiquang.dto.mapper.TripMapper;
import com.khaiquang.dto.request.TripDto;
import com.khaiquang.dto.response.TripResponseDto;
import com.khaiquang.entity.BookingTrip;
import com.khaiquang.entity.Bus;
import com.khaiquang.entity.Station;
import com.khaiquang.entity.Trip;
import com.khaiquang.exception.BusAPIException;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.repository.BookingRepository;
import com.khaiquang.repository.BusRepository;
import com.khaiquang.repository.StationRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.service.StatisticService;
import com.khaiquang.service.TripService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {
    private final TripRepository tripRepository;
    private final TripMapper  tripMapper;
    private final BusRepository busRepository;
    private final StationRepository  stationRepository;
    private final StatisticService statisticService;


    @Override
    public TripResponseDto addTrip(TripDto tripDto) {
        Bus bus = busRepository.findById(tripDto.getBusId()).orElseThrow(() ->
                new ResourceNotFoundException("Bus", "id",  tripDto.getBusId()));
        
        // Tự động tạo trạm nếu chưa có
        Station arrivalStation = getOrCreateStation(tripDto.getArrivalStationName());
        Station departureStation = getOrCreateStation(tripDto.getDepartureStationName());

        if(tripRepository.existsOverLap(
                tripDto.getBusId(),
                tripDto.getDepartureTime(),
                tripDto.getArrivalTime(),
                arrivalStation.getId(),
                departureStation.getId(),
                tripDto.getDepartureDate()
        ))
            throw new BusAPIException(HttpStatus.BAD_REQUEST, "Trip Time already exists");
            
        Trip trip = tripMapper.toEntity(tripDto);
        trip.setBus(bus);
        trip.setDepartureStation(departureStation);
        trip.setArrivalStation(arrivalStation);
        trip.setOrigin(departureStation.getName());
        trip.setDestination(arrivalStation.getName());
        
        Trip savedTrip = tripRepository.save(trip);
        statisticService.incrementTripCount();
        
        return tripMapper.toResponseDto(savedTrip);
    }

    private Station getOrCreateStation(String stationName) {
        return stationRepository.findByName(stationName)
                .orElseGet(() -> {
                    Station newStation = new Station();
                    newStation.setName(stationName);
                    return stationRepository.save(newStation);
                });
    }

    @Override
    public TripResponseDto updateTrip(Long tripId, TripDto tripDto) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() ->
                new ResourceNotFoundException("Trip", "id", tripId));
        if(tripRepository.existsOverLap(
                tripDto.getBusId(),
                tripDto.getDepartureTime(),
                tripDto.getArrivalTime(),
                trip.getDepartureStation().getId(),
                trip.getArrivalStation().getId(),
                tripDto.getDepartureDate()
        ))
            throw new BusAPIException(HttpStatus.BAD_REQUEST, "Bus already exists");
        trip.setArrivalTime(tripDto.getArrivalTime());
        trip.setDepartureTime(tripDto.getDepartureTime());
        trip.setDepartureDate(tripDto.getDepartureDate());
        return tripMapper.toResponseDto(tripRepository.save(trip));
    }

    @Override
    public void deleteTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() ->
                new ResourceNotFoundException("Trip", "id", tripId));
        tripRepository.delete(trip);
    }


    @Override
    public List<TripResponseDto> getALlTripByBusId(Long busId) {
        if(!busRepository.existsById(busId))
            throw new ResourceNotFoundException("Bus", "id", busId);

        return tripRepository.findByBusId(busId)
                .stream()
                .map(tripMapper::toResponseDto).toList();
    }

    @Override
    public List<TripResponseDto> searchTrips(String origin, String destination, LocalDate date) {
        return tripRepository.findByOriginAndDestinationAndDepartureDate(origin, destination, date)
                .stream()
                .map(tripMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripResponseDto> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(tripMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
