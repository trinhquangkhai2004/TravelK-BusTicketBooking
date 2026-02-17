package com.khaiquang.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusUpdateRequest {
    @NotNull
    private Long id;
    @NotNull
    private int number;
    @NotNull
    private int seats;
    @NotNull
    private String busType;
}
