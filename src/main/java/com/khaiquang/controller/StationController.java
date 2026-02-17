package com.khaiquang.controller;

import com.khaiquang.dto.request.StationRequestDto;
import com.khaiquang.dto.request.StationUpdateRequest;
import com.khaiquang.dto.response.StationAndTripResponseDto;
import com.khaiquang.dto.response.StationResponseDto;
import com.khaiquang.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/station")
@RequiredArgsConstructor
public class StationController {
    private final StationService  stationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponseDto> createStation(@RequestBody StationRequestDto stationRequestDto) {
        return  new ResponseEntity<>(stationService.createStation(stationRequestDto), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationResponseDto> updateStation(@RequestBody StationUpdateRequest dto, @PathVariable Long id) {
        return new ResponseEntity<>(stationService.updateStation(dto, id), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StationResponseDto>> getAllStations() {
        return new ResponseEntity<>(stationService.getAllStations(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationResponseDto> getStationById(@PathVariable Long id) {
        return new ResponseEntity<>(stationService.getStationById(id), HttpStatus.OK);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStationById(@PathVariable Long id){
        stationService.deleteStation(id);
        return new ResponseEntity<>("Deleted station successfully!", HttpStatus.OK);
    }

}
