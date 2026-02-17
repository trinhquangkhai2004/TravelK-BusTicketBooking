package com.khaiquang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long id;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Column(nullable = false)
    private String origin;
    @Column(nullable = false)
    private String destination;
    @Column(nullable = false)
    private LocalDate departureDate;
    @Column(nullable = false)
    private LocalDate arrivalDate;
    @Column(nullable = false)
    private LocalTime departureTime;
    @Column(nullable = false)
    private LocalTime arrivalTime;
    @ManyToOne
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;
    @ManyToOne
    @JoinColumn(name = "departure_station_id", nullable = false)
    private Station departureStation;
    @ManyToOne
    @JoinColumn(name = "arrival_station_id", nullable = false)
    private Station arrivalStation;

}
