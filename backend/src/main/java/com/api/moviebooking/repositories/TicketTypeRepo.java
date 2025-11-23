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
     * Find ticket type by its code (e.g., 'adult', 'student', etc.)
     */
    Optional<TicketType> findByCode(String code);

    /**
     * Check if ticket type code already exists
     */
    boolean existsByCode(String code);

    /**
     * Find all ticket types ordered by sort order (for admin)
     */
    @Query("SELECT t FROM TicketType t ORDER BY t.sortOrder ASC")
    List<TicketType> findAllOrderedBySortOrder();
}
