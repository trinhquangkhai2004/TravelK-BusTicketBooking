package com.khaiquang.repository;

import com.khaiquang.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat,Long> {
    List<Seat> findByBusId(Long busId);
}
