package com.khaiquang.repository;

import com.khaiquang.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus,Long> {
    List<Bus> findByStationIdAndDeletedFalse(Long stationId);
    List<Bus> findByDeletedFalse();
    Optional<Bus> findByStationIdAndId(Long stationId, Long id);
}
