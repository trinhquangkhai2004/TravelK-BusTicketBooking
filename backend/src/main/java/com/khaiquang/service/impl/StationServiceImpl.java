package com.khaiquang.service.impl;

import com.khaiquang.dto.mapper.StationMapper;
import com.khaiquang.dto.mapper.TripMapper;
import com.khaiquang.dto.request.BusUpdateRequest;
import com.khaiquang.dto.request.StationRequestDto;
import com.khaiquang.dto.request.StationUpdateRequest;
import com.khaiquang.dto.response.StationAndTripResponseDto;
import com.khaiquang.dto.response.StationResponseDto;
import com.khaiquang.dto.response.TripResponseDto;
import com.khaiquang.entity.Bus;
import com.khaiquang.entity.Station;
import com.khaiquang.entity.Trip;
import com.khaiquang.exception.ResourceDuplicateException;
import com.khaiquang.exception.ResourceNotFoundException;
import com.khaiquang.repository.BusRepository;
import com.khaiquang.repository.StationRepository;
import com.khaiquang.repository.TripRepository;
import com.khaiquang.service.StationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {
    private final StationRepository stationRepository;
    private final StationMapper  stationMapper;

    @Override
    public StationResponseDto createStation(StationRequestDto stationRequestDto) {
        if(stationRepository.existsByName(stationRequestDto.getName())) {
            throw new ResourceDuplicateException("Station", "name",  stationRequestDto.getName());
        }
        Station station = stationMapper.toEntity(stationRequestDto);
        for(Bus bus : station.getBuses()) {
            bus.setStation(station);
        }
        return stationMapper.toResponse(stationRepository.save(station));
    }

    @Override
    public StationResponseDto updateStation(StationUpdateRequest dto, Long id) {
        Station station = stationRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Station", "id", id));
        station.setName(dto.getName());

        if(dto.getBusList() != null) {
            Map<Long, Bus> currentBus = station.getBuses()
                    .stream()
                    .collect(Collectors.toMap(Bus::getId, e -> e));

            for(BusUpdateRequest busDto : dto.getBusList()) {
                Bus bus = currentBus.get(busDto.getId());
                if(bus == null) {
                    throw new ResourceNotFoundException("Bus", "id", busDto.getId());
                }
                bus.setNumber(String.valueOf(busDto.getNumber()));
                bus.setSeats(busDto.getSeats());
                bus.setStation(station);

            }
        }
        return stationMapper.toResponse(stationRepository.save(station));
    }

    @Override
    public List<StationResponseDto> getAllStations() {
        return stationRepository.findAll().stream().map(stationMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public StationResponseDto getStationById(Long id) {
        return stationMapper.toResponse(stationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Station", "id", id)));
    }

    @Override
    public void deleteStation(Long id) {
        Station station =  stationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Station", "id", id));
        stationRepository.delete(station);
    }
}
