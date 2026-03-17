package com.khaiquang.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteBookingRequest {
    private Long busId;
    private Long tripId;
    private Long bookingId;
}
