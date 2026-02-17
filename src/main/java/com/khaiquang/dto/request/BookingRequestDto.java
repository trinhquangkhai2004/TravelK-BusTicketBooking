package com.khaiquang.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequestDto {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bookingDate;

    @NotNull(message = "Bus ID is required")
    private Long busId;

    @NotNull(message = "Station ID is required")
    private Long stationId;

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotEmpty(message = "Seats cannot be empty")
    private List<String> seats;

    private Long userId;

    private String busName;
    private String stationName;
    private String userFullName;
}
