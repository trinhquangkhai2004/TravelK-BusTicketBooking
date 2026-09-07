package com.khaiquang.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
// Chốt chặn cuối cùng chống đặt trùng ghế: một ghế chỉ tồn tại 1 vé trên mỗi chuyến.
// Vé bị huỷ được xoá hẳn (autoCancelUnpaidBookings) nên ràng buộc này không chặn việc đặt lại ghế đã huỷ.
@Table(name = "tickets", uniqueConstraints = @UniqueConstraint(
        name = "uk_tickets_trip_seat", columnNames = {"trip_id", "seat_number"}))
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingTrip bookingTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
}
