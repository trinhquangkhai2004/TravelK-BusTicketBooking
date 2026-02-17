package com.khaiquang.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusResponseDto {
    private Long id;
    private String number;
    private int seats;
    private String busType;
    private Long stationId;
    private String stationName;

}
