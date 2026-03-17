package com.khaiquang.repository;

import com.khaiquang.entity.Station;
import com.khaiquang.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByBusId(Long busId);

    List<Trip> findByOriginAndDestinationAndDepartureDate(String origin, String destination, LocalDate departureDate);
    
    // Tìm kiếm linh hoạt cho Chatbot
    @Query("SELECT t FROM Trip t WHERE t.origin LIKE %:keyword% OR t.destination LIKE %:keyword%")
    List<Trip> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
        SELECT COUNT(t) > 0
        FROM Trip t
        WHERE t.bus.id = :busId
          AND t.departureStation.id = :departureStationId
          AND t.arrivalStation.id = :arrivalStationId
          AND t.departureDate = :departureDate
          AND :startTime < t.arrivalTime
          AND :endTime > t.departureTime
    """)
    boolean existsOverLap(@Param("busId") Long busId,
                          @Param("startTime") LocalTime start,
                          @Param("endTime") LocalTime end,
                          @Param("departureStationId") Long departureStationId,
                          @Param("arrivalStationId") Long arrivalStationId,
                          @Param("departureDate") LocalDate departureDate
    );

    @Query("SELECT DISTINCT t.departureStation FROM Trip t WHERE t.bus.id = :busId " +
           "AND t.departureDate = :date" )
    List<Station> findDepartureStationsByBusAndDate(
            Long busId,
            LocalDate date
    );
}
