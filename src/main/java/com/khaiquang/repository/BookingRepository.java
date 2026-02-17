package com.khaiquang.repository;

import com.khaiquang.entity.BookingTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingTrip, Long> {
    List<BookingTrip> findByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);
}
