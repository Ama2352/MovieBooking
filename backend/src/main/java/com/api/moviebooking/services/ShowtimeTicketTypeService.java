package com.api.moviebooking.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.ShowtimeTicketType;
import com.api.moviebooking.models.entities.TicketType;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.ShowtimeTicketTypeRepo;
import com.api.moviebooking.repositories.TicketTypeRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeTicketTypeService {

    private final ShowtimeTicketTypeRepo showtimeTicketTypeRepo;
    private final ShowtimeRepo showtimeRepo;
    private final TicketTypeRepo ticketTypeRepo;

    /**
     * Assign a ticket type to a showtime
     */
    @Transactional
    public void assignTicketTypeToShowtime(UUID showtimeId, UUID ticketTypeId) {
        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        TicketType ticketType = ticketTypeRepo.findById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketType", "id", ticketTypeId));

        // Check if already exists
        if (showtimeTicketTypeRepo.existsActiveByShowtimeAndTicketType(showtimeId, ticketTypeId)) {
            log.info("Ticket type {} already assigned to showtime {}", ticketTypeId, showtimeId);
            return;
        }

        ShowtimeTicketType showtimeTicketType = new ShowtimeTicketType();
        showtimeTicketType.setShowtime(showtime);
        showtimeTicketType.setTicketType(ticketType);
        showtimeTicketType.setActive(true);

        showtimeTicketTypeRepo.save(showtimeTicketType);
        log.info("Assigned ticket type {} to showtime {}", ticketTypeId, showtimeId);
    }

    /**
     * Assign multiple ticket types to a showtime
     */
    @Transactional
    public void assignTicketTypesToShowtime(UUID showtimeId, List<UUID> ticketTypeIds) {
        for (UUID ticketTypeId : ticketTypeIds) {
            assignTicketTypeToShowtime(showtimeId, ticketTypeId);
        }
    }

    /**
     * Remove a ticket type assignment from a showtime
     */
    @Transactional
    public void removeTicketTypeFromShowtime(UUID showtimeId, UUID ticketTypeId) {
        List<ShowtimeTicketType> assignments = showtimeTicketTypeRepo.findActiveTicketTypesByShowtime(showtimeId)
                .stream()
                .filter(stt -> stt.getTicketType().getId().equals(ticketTypeId))
                .collect(Collectors.toList());

        if (assignments.isEmpty()) {
            throw new ResourceNotFoundException("ShowtimeTicketType", "showtimeId and ticketTypeId",
                    showtimeId + " and " + ticketTypeId);
        }

        for (ShowtimeTicketType assignment : assignments) {
            assignment.setActive(false);
            showtimeTicketTypeRepo.save(assignment);
        }

        log.info("Removed ticket type {} from showtime {}", ticketTypeId, showtimeId);
    }

    /**
     * Get all ticket type assignments for a showtime
     */
    public List<UUID> getAssignedTicketTypeIds(UUID showtimeId) {
        // Verify showtime exists
        showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        return showtimeTicketTypeRepo.findActiveTicketTypesByShowtime(showtimeId)
                .stream()
                .map(stt -> stt.getTicketType().getId())
                .collect(Collectors.toList());
    }

    /**
     * Replace all ticket type assignments for a showtime
     */
    @Transactional
    public void replaceTicketTypesForShowtime(UUID showtimeId, List<UUID> ticketTypeIds) {
        // Verify showtime exists
        showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        // Deactivate all existing assignments
        List<ShowtimeTicketType> existingAssignments = showtimeTicketTypeRepo
                .findActiveTicketTypesByShowtime(showtimeId);
        for (ShowtimeTicketType assignment : existingAssignments) {
            assignment.setActive(false);
            showtimeTicketTypeRepo.save(assignment);
        }

        // Assign new ticket types
        assignTicketTypesToShowtime(showtimeId, ticketTypeIds);

        log.info("Replaced ticket types for showtime {} with {} types", showtimeId, ticketTypeIds.size());
    }
}
