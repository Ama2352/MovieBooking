package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.TicketTypeMapper;
import com.api.moviebooking.models.dtos.ticketType.CreateTicketTypeRequest;
import com.api.moviebooking.models.dtos.ticketType.TicketTypeResponse;
import com.api.moviebooking.models.dtos.ticketType.TicketTypePublicResponse;
import com.api.moviebooking.models.dtos.ticketType.UpdateTicketTypeRequest;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.ShowtimeTicketType;
import com.api.moviebooking.models.entities.TicketType;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.SeatLockSeatRepo;
import com.api.moviebooking.repositories.BookingSeatRepo;
import com.api.moviebooking.repositories.ShowtimeTicketTypeRepo;
import com.api.moviebooking.repositories.TicketTypeRepo;
import com.api.moviebooking.models.enums.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketTypeService {

    private final TicketTypeRepo ticketTypeRepo;
    private final ShowtimeRepo showtimeRepo;
    private final SeatLockSeatRepo seatLockSeatRepo;
    private final BookingSeatRepo bookingSeatRepo;
    private final ShowtimeTicketTypeRepo showtimeTicketTypeRepo;
    private final PriceCalculationService priceCalculationService;
    private final TicketTypeMapper ticketTypeMapper;

    /**
     * Get all active ticket types with their base prices
     * Used for guest endpoint: GET /ticket-types
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     * Minimum test cases: 1
     */
    public List<TicketTypePublicResponse> getAllActiveTicketTypes() {
        List<TicketType> ticketTypes = ticketTypeRepo.findAllByActiveTrue();
        return ticketTypes.stream()
                .map(ticketTypeMapper::toPublicResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get ticket types with calculated prices for a specific showtime
     * Used for guest endpoint: GET /ticket-types?showtimeId={showtimeId}
     * 
     * Returns only ticket types that are enabled for this specific showtime via
     * ShowtimeTicketType.
     * Prices are calculated based on a NORMAL seat as a reference price.
     * The actual final price will vary based on the specific seat selected (VIP,
     * COUPLE, etc.)
     * and will be calculated during the seat locking phase.
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: showtime.isEmpty, showtimeTicketTypes.isEmpty
     * Minimum test cases: 3
     */
    public List<TicketTypePublicResponse> getTicketTypesForShowtime(UUID showtimeId, UUID userId) {
        Showtime showtime = showtimeRepo.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));

        // Get only ticket types that are active for this specific showtime
        List<ShowtimeTicketType> showtimeTicketTypes = showtimeTicketTypeRepo
                .findActiveTicketTypesByShowtime(showtimeId);

        // If no ticket types are assigned to this showtime, fall back to all active
        // ticket types
        List<TicketType> ticketTypes;
        if (showtimeTicketTypes.isEmpty()) {
            log.warn("No ticket types assigned to showtime {}. Falling back to all active ticket types.", showtimeId);
            ticketTypes = ticketTypeRepo.findAllByActiveTrue();
        } else {
            ticketTypes = showtimeTicketTypes.stream()
                    .map(ShowtimeTicketType::getTicketType)
                    .collect(Collectors.toList());
        }

        // Calculate reference price based on NORMAL seat type
        // This gives users an idea of ticket type pricing
        // Actual price will be calculated per seat during lock phase
        BigDecimal referenceSeatPrice = calculateReferencePriceForShowtime(showtime);

        return ticketTypes.stream()
                .map(ticketType -> {
                    // Apply ticket type modifier to reference price
                    BigDecimal priceWithTicketType = applyTicketTypeModifier(referenceSeatPrice, ticketType);

                    TicketTypePublicResponse response = ticketTypeMapper.toPublicResponse(ticketType);
                    response.setPrice(priceWithTicketType);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate reference price for a showtime based on NORMAL seat type
     * This is used to show estimated ticket type prices before seat selection
     * 
     * @param showtime The showtime to calculate price for
     * @return Base price including showtime modifiers (time, day, format, room) for
     *         a NORMAL seat
     *         Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     *         Nodes: none
     *         Minimum test cases: 1
     */
    private BigDecimal calculateReferencePriceForShowtime(Showtime showtime) {
        // Create a reference seat with NORMAL type for price calculation
        Seat referenceSeat = new Seat();
        referenceSeat.setSeatType(SeatType.NORMAL);

        // Calculate base price for the showtime with NORMAL seat
        return priceCalculationService.calculatePrice(showtime, referenceSeat);
    }

    /**
     * Apply ticket type modifier to a base price
     * This is the ONLY place where ticket type pricing should be applied
     * PERCENTAGE: finalPrice = basePrice * (1 + modifierValue / 100)
     * FIXED_AMOUNT: finalPrice = basePrice + modifierValue
     * 
     * @param basePrice  The base price from PriceCalculationService (already
     *                   includes seat/showtime modifiers)
     * @param ticketType The ticket type with modifier configuration
     * @return Final price after applying ticket type modifier
     *         Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     *         Nodes: switch(PERCENTAGE), switch(FIXED_AMOUNT), switch(default)
     *         Minimum test cases: 4
     */
    public BigDecimal applyTicketTypeModifier(BigDecimal basePrice, TicketType ticketType) {
        switch (ticketType.getModifierType()) {
            case PERCENTAGE:
                // e.g., basePrice = 100000, modifierValue = -20 -> finalPrice = 100000 * (1 -
                // 0.20) = 80000
                BigDecimal multiplier = BigDecimal.ONE.add(ticketType.getModifierValue().divide(new BigDecimal("100")));
                return basePrice.multiply(multiplier).setScale(0, java.math.RoundingMode.HALF_UP);

            case FIXED_AMOUNT:
                // e.g., basePrice = 100000, modifierValue = -15000 -> finalPrice = 100000 -
                // 15000 = 85000
                return basePrice.add(ticketType.getModifierValue()).setScale(0, java.math.RoundingMode.HALF_UP);

            default:
                return basePrice;
        }
    }

    /**
     * Get all ticket types (including inactive) for admin
     * Used for admin endpoint: GET /ticket-types/admin
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     * Minimum test cases: 1
     */
    public List<TicketTypeResponse> getAllTicketTypesForAdmin() {
        List<TicketType> ticketTypes = ticketTypeRepo.findAllOrderedBySortOrder();
        return ticketTypes.stream()
                .map(ticketTypeMapper::toAdminResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new ticket type
     * Used for admin endpoint: POST /admin/ticket-types
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: existsByCode
     * Minimum test cases: 2
     */
    @Transactional
    public TicketTypeResponse createTicketType(CreateTicketTypeRequest request) {
        // Validate ticket type code is unique
        if (ticketTypeRepo.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Ticket type code already exists: " + request.getCode());
        }

        TicketType ticketType = new TicketType();
        ticketType.setCode(request.getCode());
        ticketType.setLabel(request.getLabel());
        ticketType.setModifierType(request.getModifierType());
        ticketType.setModifierValue(request.getModifierValue());
        ticketType.setActive(request.getActive());
        ticketType.setSortOrder(request.getSortOrder());

        ticketTypeRepo.save(ticketType);
        log.info("Created ticket type: {}", ticketType.getCode());

        return ticketTypeMapper.toAdminResponse(ticketType);
    }

    /**
     * Update an existing ticket type
     * Used for admin endpoint: PUT /admin/ticket-types/{id}
     * Predicate nodes (d): 6 -> V(G) = d + 1 = 7
     * Nodes: ticketType.isEmpty, label != null, modifierType != null, modifierValue
     * != null,
     * active != null, sortOrder != null
     * Minimum test cases: 7
     */
    @Transactional
    public TicketTypeResponse updateTicketType(UUID id, UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketType", "id", id));

        if (request.getLabel() != null) {
            ticketType.setLabel(request.getLabel());
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
        log.info("Updated ticket type: {}", ticketType.getCode());

        return ticketTypeMapper.toAdminResponse(ticketType);
    }

    /**
     * Delete a ticket type
     * Soft delete if used in seat locks or bookings, hard delete if not used
     * Used for admin endpoint: DELETE /admin/ticket-types/{id}
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: ticketType.isEmpty, isUsedInLocks || isUsedInBookings, else (hard
     * delete)
     * Minimum test cases: 4
     */
    @Transactional
    public void deleteTicketType(UUID id) {
        TicketType ticketType = ticketTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketType", "id", id));

        // Check if ticket type is used in seat locks or bookings
        // SeatLockSeat: temporary locks (may contain this ticket type)
        // BookingSeat: confirmed bookings (historical data, must preserve)
        boolean isUsedInLocks = seatLockSeatRepo.isTicketTypeUsed(id);
        boolean isUsedInBookings = isTicketTypeUsedInBookings(id);

        if (isUsedInLocks || isUsedInBookings) {
            // Soft delete: set active to false
            ticketType.setActive(false);
            ticketTypeRepo.save(ticketType);
            log.info("Soft deleted ticket type (set active=false, used in {} locks, {} bookings): {}",
                    isUsedInLocks ? "some" : "no",
                    isUsedInBookings ? "some" : "no",
                    ticketType.getCode());
        } else {
            // Hard delete: remove from database
            ticketTypeRepo.delete(ticketType);
            log.info("Hard deleted ticket type: {}", ticketType.getCode());
        }
    }

    /**
     * Validate that a ticket type is available for a specific showtime
     * Throws IllegalArgumentException if not available
     * 
     * @param showtimeId   The showtime ID
     * @param ticketTypeId The ticket type ID
     * @throws ResourceNotFoundException if ticket type doesn't exist
     * @throws IllegalArgumentException  if ticket type is not available for the
     *                                   showtime
     *                                   Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     *                                   Nodes: ticketType.isEmpty,
     *                                   !isValidForShowtime
     *                                   Minimum test cases: 3
     */
    public void validateTicketTypeForShowtime(UUID showtimeId, UUID ticketTypeId) {
        // First check if ticket type exists
        TicketType ticketType = ticketTypeRepo.findById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketType", "id", ticketTypeId));

        // Then check if it's active for this showtime
        boolean isValidForShowtime = showtimeTicketTypeRepo.existsActiveByShowtimeAndTicketType(
                showtimeId, ticketTypeId);

        if (!isValidForShowtime) {
            throw new IllegalArgumentException(
                    String.format("Ticket type '%s' is not available for this showtime. " +
                            "Please select from the available ticket types for this showtime.",
                            ticketType.getCode()));
        }
    }

    /**
     * Check if ticket type is used in any booking seats
     * Uses BookingSeatRepo to check historical bookings
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     * Minimum test cases: 1
     */
    private boolean isTicketTypeUsedInBookings(UUID ticketTypeId) {
        return bookingSeatRepo.isTicketTypeUsed(ticketTypeId);
    }
}
