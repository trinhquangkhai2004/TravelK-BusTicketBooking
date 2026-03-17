package com.khaiquang.repository;

import com.khaiquang.entity.Station;
import com.khaiquang.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface StationRepository extends JpaRepository<Station,Long> {
    Boolean existsByName(String name);
    Optional<Station> findByName(String name);
}
