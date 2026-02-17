package com.khaiquang.dto.response;

import com.khaiquang.dto.message.SeatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public record SeatResponseDto(List<SeatMessage> allSeats, List<String> orderedSeats) {

}
