package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.models.dtos.ticketType.CreateTicketTypeRequest;
import com.api.moviebooking.models.dtos.ticketType.TicketTypeResponse;
import com.api.moviebooking.models.dtos.ticketType.UpdateTicketTypeRequest;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.TicketType;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.ShowtimeSeatRepo;
import com.api.moviebooking.repositories.TicketTypeRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketTypeService {

    private final TicketTypeRepo ticketTypeRepo;
    private final ShowtimeRepo showtimeRepo;
    private final ShowtimeSeatRepo showtimeSeatRepo;
    private final PriceCalculationService priceCalculationService;

    /**
     * Get all active ticket types with their base prices
     * Used for guest endpoint: GET /ticket-types
     */
    public List<TicketTypeResponse> getAllActiveTicketTypes() {
        List<TicketType> ticketTypes = ticketTypeRepo.findAllByActiveTrue();
        return ticketTypes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get ticket types with calculated prices for a specific showtime
     * Used for guest endpoint: GET /ticket-types?showtimeId={showtimeId}&userId={userId}
     * Prices are calculated based on showtime, room type, format, time, etc.
     */
    public List<TicketTypeResponse> getTicketTypesForShowtime(UUID showtimeId, UUID userId) {
        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        List<TicketType> ticketTypes = ticketTypeRepo.findAllByActiveTrue();

        // For each ticket type, calculate price based on its seat type mapping
        // "double" ticket type maps to COUPLE seat, others map to NORMAL seat
        return ticketTypes.stream()
                .map(ticketType -> {
                    // Determine seat type from ticket type ID
                    Seat dummySeat = new Seat();
                    dummySeat.setSeatType(getSeatTypeFromTicketType(ticketType.getTicketTypeId()));

                    BigDecimal calculatedPrice = priceCalculationService.calculatePrice(showtime, dummySeat, ticketType);

                    TicketTypeResponse response = toResponse(ticketType);
                    response.setPrice(calculatedPrice);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all ticket types (including inactive) for admin
     * Used for admin endpoint: GET /admin/ticket-types
     */
    public List<TicketTypeResponse> getAllTicketTypesForAdmin() {
        List<TicketType> ticketTypes = ticketTypeRepo.findAllOrderedBySortOrder();
        return ticketTypes.stream()
                .map(this::toResponseWithAdminFields)
                .collect(Collectors.toList());
    }

    /**
     * Create a new ticket type
     * Used for admin endpoint: POST /admin/ticket-types
     */
    @Transactional
    public TicketTypeResponse createTicketType(CreateTicketTypeRequest request) {
        // Validate ticket type ID is unique
        if (ticketTypeRepo.existsByTicketTypeId(request.getTicketTypeId())) {
            throw new IllegalArgumentException("Ticket type ID already exists: " + request.getTicketTypeId());
        }

        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(request.getTicketTypeId());
        ticketType.setLabel(request.getLabel());
        ticketType.setBasePrice(request.getPrice());
        ticketType.setActive(request.getActive());
        ticketType.setSortOrder(request.getSortOrder());

        ticketTypeRepo.save(ticketType);
        log.info("Created ticket type: {}", ticketType.getTicketTypeId());

        return toResponseWithAdminFields(ticketType);
    }

    /**
     * Update an existing ticket type
     * Used for admin endpoint: PUT /admin/ticket-types/{id}
     */
    @Transactional
    public TicketTypeResponse updateTicketType(UUID id, UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketType", "id", id));

        if (request.getLabel() != null) {
            ticketType.setLabel(request.getLabel());
        }

        if (request.getPrice() != null) {
            ticketType.setBasePrice(request.getPrice());
        }

        if (request.getActive() != null) {
            ticketType.setActive(request.getActive());
        }

        if (request.getSortOrder() != null) {
            ticketType.setSortOrder(request.getSortOrder());
        }

        ticketTypeRepo.save(ticketType);
        log.info("Updated ticket type: {}", ticketType.getTicketTypeId());

        return toResponseWithAdminFields(ticketType);
    }

    /**
     * Delete a ticket type
     * Soft delete if used in bookings, hard delete if not used
     * Used for admin endpoint: DELETE /admin/ticket-types/{id}
     */
    @Transactional
    public void deleteTicketType(UUID id) {
        TicketType ticketType = ticketTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketType", "id", id));

        // Check if ticket type is used in any showtime seats
        boolean isUsed = showtimeSeatRepo.isTicketTypeUsed(id);

        if (isUsed) {
            // Soft delete: set active to false
            ticketType.setActive(false);
            ticketTypeRepo.save(ticketType);
            log.info("Soft deleted ticket type (set active=false): {}", ticketType.getTicketTypeId());
        } else {
            // Hard delete: remove from database
            ticketTypeRepo.delete(ticketType);
            log.info("Hard deleted ticket type: {}", ticketType.getTicketTypeId());
        }
    }

    /**
     * Convert entity to response DTO (basic fields only)
     */
    private TicketTypeResponse toResponse(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .ticketTypeId(ticketType.getTicketTypeId())
                .label(ticketType.getLabel())
                .price(ticketType.getBasePrice())
                .build();
    }

    /**
     * Convert entity to response DTO (with admin fields)
     */
    private TicketTypeResponse toResponseWithAdminFields(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .ticketTypeId(ticketType.getTicketTypeId())
                .label(ticketType.getLabel())
                .price(ticketType.getBasePrice())
                .active(ticketType.getActive())
                .sortOrder(ticketType.getSortOrder())
                .build();
    }

    /**
     * Map ticket type ID to seat type for pricing calculation
     * "double" ticket type represents COUPLE seats
     * All other ticket types represent NORMAL seats (adult, student, senior, member, etc.)
     */
    private com.api.moviebooking.models.enums.SeatType getSeatTypeFromTicketType(String ticketTypeId) {
        if ("double".equalsIgnoreCase(ticketTypeId)) {
            return com.api.moviebooking.models.enums.SeatType.COUPLE;
        }
        // Default to NORMAL for all other ticket types (adult, student, senior, member, etc.)
        return com.api.moviebooking.models.enums.SeatType.NORMAL;
    }
}
