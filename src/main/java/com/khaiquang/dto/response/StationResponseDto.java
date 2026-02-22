package com.khaiquang.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StationResponseDto {
    private Long stationId;
    private String stationName;
    private String address;
    private List<BusResponseDto> buses;
}
