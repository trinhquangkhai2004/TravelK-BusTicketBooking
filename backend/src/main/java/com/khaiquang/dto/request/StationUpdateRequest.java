package com.khaiquang.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StationUpdateRequest {
    private String name;
    private List<BusUpdateRequest> busList;
}
