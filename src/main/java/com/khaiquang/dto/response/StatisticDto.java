package com.khaiquang.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatisticDto {
    private Integer userCount;
    private Integer totalTicketSold;
    private Integer totalTrips;
    private Integer revenue;
}
