package com.khaiquang.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class BusRequestDto {
    @NotEmpty(message = "number of bus can not be null")
    private String number;
    @NotEmpty(message = "type of bus should not be null")
    private String busType;
    @NotNull
    private int seats;


}
