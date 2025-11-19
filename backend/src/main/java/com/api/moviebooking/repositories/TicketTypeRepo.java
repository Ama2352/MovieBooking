package com.api.moviebooking.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.api.moviebooking.models.entities.TicketType;

@Repository
public interface TicketTypeRepo extends JpaRepository<TicketType, UUID> {

    /**
     * Find all active ticket types ordered by sort order
     */
    @Query("SELECT t FROM TicketType t WHERE t.active = true ORDER BY t.sortOrder ASC")
    List<TicketType> findAllByActiveTrue();

    /**
     * Find ticket type by its logical ID
     */
    Optional<TicketType> findByTicketTypeId(String ticketTypeId);

    /**
     * Check if ticket type ID already exists
     */
    boolean existsByTicketTypeId(String ticketTypeId);

    /**
     * Find all ticket types ordered by sort order (for admin)
     */
    @Query("SELECT t FROM TicketType t ORDER BY t.sortOrder ASC")
    List<TicketType> findAllOrderedBySortOrder();
}
