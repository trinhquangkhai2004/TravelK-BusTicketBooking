package com.khaiquang.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "seat")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long id;
    @Column(name = "seat_name")
    private String name;
    @ManyToOne
    @JoinColumn(name = "bus_id", nullable = false)
    @JsonIgnore
    private Bus bus;
}
