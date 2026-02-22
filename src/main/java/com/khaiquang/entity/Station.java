package com.khaiquang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "station")
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id")
    private Long id;
    
    @Column(name = "station_name")
    private String name;
    
    @Column(name = "address")
    private String address;
    
    @OneToMany(mappedBy = "station",  cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Bus> buses = new ArrayList<>();

}
