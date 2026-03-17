package com.khaiquang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "buses")
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bus_id")
    private Long id;
    private String number;
    private String busType;
    private String description;
    private int seats;
    @Column(name = "rating_points", columnDefinition = "Decimal(10,2) default '0.0'")
    private BigDecimal ratingPoints = BigDecimal.valueOf(0.0);
    @Column(name = "ratings")
    private Integer ratings;
    
    @Column(name = "is_deleted")
    private boolean deleted = false;

    @ManyToOne
    @JoinColumn(name = "station_id",  nullable = false)
    private Station station;
    @OneToMany(mappedBy = "bus")
    private List<Trip> trips;
    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL)
    private List<Seat> seatList = new ArrayList<>();

}
