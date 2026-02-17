package com.khaiquang.controller;

import com.khaiquang.dto.request.TripDto;
import com.khaiquang.dto.response.TripResponseDto;
import com.khaiquang.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {
    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponseDto> createTrip(@RequestBody TripDto tripDto) {
        return new ResponseEntity<>(tripService.addTrip(tripDto), HttpStatus.CREATED);
    }

    @GetMapping("/bus/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TripResponseDto>> getALlTripByBusId(@PathVariable Long id) {
        return new ResponseEntity<>(tripService.getALlTripByBusId(id), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponseDto> updateTrip(@PathVariable Long tripId, @RequestBody TripDto tripDto){
        return new ResponseEntity<>(tripService.updateTrip(tripId, tripDto), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteTrip(@PathVariable Long tripId) {
        tripService.deleteTrip(tripId);
        return new ResponseEntity<>("Deleted successfully", HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TripResponseDto>> searchTrips(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new ResponseEntity<>(tripService.searchTrips(origin, destination, date), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<TripResponseDto>> getAllTrips() {
        return new ResponseEntity<>(tripService.getAllTrips(), HttpStatus.OK);
    }
}
