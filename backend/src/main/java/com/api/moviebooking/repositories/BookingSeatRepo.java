package com.api.moviebooking.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.BookingSeat;

@Repository
public interface BookingSeatRepo extends JpaRepository<BookingSeat, UUID> {

    /**
     * Find all booking seats for a specific booking
     */
    @Query("SELECT bs FROM BookingSeat bs WHERE bs.booking.id = :bookingId")
    List<BookingSeat> findByBookingId(@Param("bookingId") UUID bookingId);

    /**
     * Check if a ticket type is used in any booking seat (for soft delete check)
     */
    @Query("SELECT COUNT(bs) > 0 FROM BookingSeat bs WHERE bs.ticketTypeApplied.id = :ticketTypeId")
    boolean isTicketTypeUsed(@Param("ticketTypeId") UUID ticketTypeId);
}
