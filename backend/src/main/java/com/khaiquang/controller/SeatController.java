package com.khaiquang.controller;

import com.khaiquang.entity.Seat;
import com.khaiquang.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;

    @GetMapping("bus/{busId}")
    public ResponseEntity<List<Seat>> getAllSeatsByBusId(@PathVariable Long busId) {
        return new  ResponseEntity<>(seatService.getAllSeatsByBusId(busId), HttpStatus.OK);
    }
}
