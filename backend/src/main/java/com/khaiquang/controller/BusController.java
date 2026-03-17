package com.khaiquang.controller;

import com.khaiquang.dto.request.BusRequestDto;
import com.khaiquang.dto.response.BusResponseDto;
import com.khaiquang.service.BusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
public class BusController {
    private final BusService busService;

    @PostMapping("/station/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusResponseDto> createBus(@PathVariable Long id, @Valid @RequestBody BusRequestDto busRequestDto) {
        return new ResponseEntity<>(busService.createBus(busRequestDto, id), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusResponseDto> updateBus(@PathVariable Long id ,@Valid @RequestBody BusRequestDto busRequestDto) {
        return  new ResponseEntity<>(busService.updateBus(busRequestDto, id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteBus(@PathVariable Long id){
            busService.deleteBus(id);
            return new ResponseEntity<>("Deleted Bus!",HttpStatus.OK);
        }

    @GetMapping("/station/{id}")
    public ResponseEntity<List<BusResponseDto>> getAllBusesByStationId(@PathVariable Long id) {
        return new ResponseEntity<>(busService.getAllBusByStationId(id),  HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<BusResponseDto>> getAllBuses() {
        return new ResponseEntity<>(busService.getAllBuses(), HttpStatus.OK);
    }
}
