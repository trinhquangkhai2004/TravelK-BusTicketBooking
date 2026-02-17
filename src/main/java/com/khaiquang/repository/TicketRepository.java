package com.khaiquang.repository;

import com.khaiquang.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByTripId(Long tripId);
    
    boolean existsByTripIdAndSeatNumber(Long tripId, String seatNumber);
    
    void deleteByBookingTripId(Long bookingTripId);

    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.bookingTrip b WHERE b.status = 'PAID'")
    Integer countSoldTickets();

    @Query("SELECT SUM(t.price) FROM Ticket t JOIN t.bookingTrip b WHERE b.status = 'PAID'")
    Integer calculateRevenue();
}
