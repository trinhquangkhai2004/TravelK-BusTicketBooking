package com.khaiquang.dto.response;

import com.khaiquang.dto.request.TripDto;
import com.khaiquang.dto.request.UserRegisterRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class APIResponseDto {
    UserRegisterRequest users;
    List<TripDto> trips;
}
