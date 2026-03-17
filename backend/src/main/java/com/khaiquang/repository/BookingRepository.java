package com.khaiquang.repository;

import com.khaiquang.entity.BookingTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingTrip, Long> {
    List<BookingTrip> findByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);
    List<BookingTrip> findByUserId(Long userId);
    @Modifying
    @Query("UPDATE BookingTrip b SET b.status = :newStatus WHERE b.id IN :ids")
    void updateStatusByIds(String newStatus, List<Long> ids);
}
