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
     * Prices are calculated by applying ticket type modifiers to showtime seat base prices
     */
    public List<TicketTypeResponse> getTicketTypesForShowtime(UUID showtimeId, UUID userId) {
        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        List<TicketType> ticketTypes = ticketTypeRepo.findAllByActiveTrue();

        // For each ticket type, calculate price based on seat type mapping
        // "double" ticket type maps to COUPLE seat, others map to NORMAL seat
        return ticketTypes.stream()
                .map(ticketType -> {
                    // Determine seat type from ticket type ID
                    Seat dummySeat = new Seat();
                    dummySeat.setSeatType(getSeatTypeFromTicketType(ticketType.getTicketTypeId()));

                    // Get base showtime seat price (without ticket type modifier)
                    BigDecimal baseSeatPrice = priceCalculationService.calculatePrice(showtime, dummySeat, null);

                    // Apply ticket type modifier to base price
                    BigDecimal finalPrice = applyTicketTypeModifier(baseSeatPrice, ticketType);

                    TicketTypeResponse response = toResponse(ticketType);
                    response.setPrice(finalPrice);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Apply ticket type modifier to a base price
     * PERCENTAGE: finalPrice = basePrice * (1 + modifierValue / 100)
     * FIXED_AMOUNT: finalPrice = basePrice + modifierValue
     */
    private BigDecimal applyTicketTypeModifier(BigDecimal basePrice, TicketType ticketType) {
        switch (ticketType.getModifierType()) {
            case PERCENTAGE:
                // e.g., basePrice = 100000, modifierValue = -20 -> finalPrice = 100000 * (1 - 0.20) = 80000
                BigDecimal multiplier = BigDecimal.ONE.add(ticketType.getModifierValue().divide(new BigDecimal("100")));
                return basePrice.multiply(multiplier).setScale(0, java.math.RoundingMode.HALF_UP);

            case FIXED_AMOUNT:
                // e.g., basePrice = 100000, modifierValue = -15000 -> finalPrice = 100000 - 15000 = 85000
                return basePrice.add(ticketType.getModifierValue()).setScale(0, java.math.RoundingMode.HALF_UP);

            default:
                return basePrice;
        }
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
        ticketType.setDescription(request.getDescription());
        ticketType.setModifierType(request.getModifierType());
        ticketType.setModifierValue(request.getModifierValue());
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

        if (request.getDescription() != null) {
            ticketType.setDescription(request.getDescription());
        }

        if (request.getModifierType() != null) {
            ticketType.setModifierType(request.getModifierType());
        }

        if (request.getModifierValue() != null) {
            ticketType.setModifierValue(request.getModifierValue());
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
     * Price field is null - should be set by caller based on context
     */
    private TicketTypeResponse toResponse(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .ticketTypeId(ticketType.getTicketTypeId())
                .label(ticketType.getLabel())
                .price(null) // Price will be calculated and set by caller
                .build();
    }

    /**
     * Convert entity to response DTO (with admin fields)
     * Includes modifier information for admins to see configuration
     */
    private TicketTypeResponse toResponseWithAdminFields(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .ticketTypeId(ticketType.getTicketTypeId())
                .label(ticketType.getLabel())
                .description(ticketType.getDescription())
                .modifierType(ticketType.getModifierType())
                .modifierValue(ticketType.getModifierValue())
                .price(null) // No base price in new architecture
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
