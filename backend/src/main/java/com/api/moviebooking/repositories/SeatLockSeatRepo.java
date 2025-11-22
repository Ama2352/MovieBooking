package com.api.moviebooking.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.SeatLockSeat;

@Repository
public interface SeatLockSeatRepo extends JpaRepository<SeatLockSeat, UUID> {

    /**
     * Find all seat lock seats for a specific seat lock
     */
    @Query("SELECT sls FROM SeatLockSeat sls WHERE sls.seatLock.id = :seatLockId")
    List<SeatLockSeat> findBySeatLockId(@Param("seatLockId") UUID seatLockId);

    /**
     * Check if a ticket type is used in any seat lock seat
     */
    @Query("SELECT COUNT(sls) > 0 FROM SeatLockSeat sls WHERE sls.ticketType.id = :ticketTypeId")
    boolean isTicketTypeUsed(@Param("ticketTypeId") UUID ticketTypeId);
}
