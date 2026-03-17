package com.khaiquang.controller;

import com.khaiquang.dto.response.StatisticDto;
import com.khaiquang.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatisticDto> getStatistics() {
        return ResponseEntity.ok(statisticService.getStatistics());
    }
}
