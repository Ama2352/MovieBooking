package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.api.moviebooking.models.dtos.showtimeSeat.PriceBreakdown;
import com.api.moviebooking.models.entities.PriceBase;
import com.api.moviebooking.models.entities.PriceModifier;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.TicketType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.repositories.PriceBaseRepo;
import com.api.moviebooking.repositories.PriceModifierRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCalculationService {

    private final PriceBaseRepo priceBaseRepo;
    private final PriceModifierRepo priceModifierRepo;
    private final ObjectMapper objectMapper;

    /**
     * Calculate final price and generate price breakdown for a showtime seat
     * Ticket type modifier should be applied separately after this calculation
     * Returns an array: [0] = final price, [1] = price breakdown JSON string
     * For internal use
     */
    public Object[] calculatePriceWithBreakdown(Showtime showtime, Seat seat, TicketType ticketType) {
        // Get base price from PriceBase table
        // Ticket type modifiers are applied separately by TicketTypeService
        PriceBase priceBase = priceBaseRepo.findActiveBasePrice()
                .orElseThrow(() -> new IllegalStateException("No active base price configured"));
        BigDecimal basePriceValue = priceBase.getBasePrice();

        BigDecimal finalPrice = basePriceValue;
        log.debug("Starting price calculation. Base price: {}", finalPrice);

        // Create price breakdown
        PriceBreakdown breakdown = new PriceBreakdown();
        breakdown.setBasePrice(basePriceValue);

        // Get all active modifiers
        List<PriceModifier> modifiers = priceModifierRepo.findAllActive();

        // Apply modifiers based on conditions
        List<PriceModifier> applicableModifiers = new ArrayList<>();

        for (PriceModifier modifier : modifiers) {
            if (isModifierApplicable(modifier, showtime, seat, ticketType)) {
                applicableModifiers.add(modifier);
                log.debug("Applicable modifier: {} - {} = {}",
                        modifier.getName(), modifier.getConditionType(), modifier.getConditionValue());
            }
        }

        // Apply all applicable modifiers and track changes
        for (PriceModifier modifier : applicableModifiers) {
            BigDecimal beforePrice = finalPrice;
            finalPrice = applyModifier(finalPrice, modifier);
            BigDecimal change = finalPrice.subtract(beforePrice);

            // Add to breakdown
            PriceBreakdown.ModifierInfo modifierInfo = new PriceBreakdown.ModifierInfo();
            modifierInfo.setName(modifier.getName());
            modifierInfo.setType(modifier.getConditionType().toString() + ":" + modifier.getConditionValue());
            modifierInfo.setValue(change);
            breakdown.getModifiers().add(modifierInfo);

            log.debug("After applying {}: {}", modifier.getName(), finalPrice);
        }

        // Round to 2 decimal places
        finalPrice = finalPrice.setScale(2, RoundingMode.HALF_UP);
        breakdown.setFinalPrice(finalPrice);

        // Convert breakdown to JSON
        String breakdownJson = null;
        try {
            breakdownJson = objectMapper.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize price breakdown", e);
            breakdownJson = "{}";
        }

        log.info("Final calculated price for showtime {} seat {}{}: {}",
                showtime.getId(), seat.getRowLabel(), seat.getSeatNumber(), finalPrice);

        return new Object[] { finalPrice, breakdownJson };
    }

    /**
     * Calculate final price and generate price breakdown for a showtime seat (backward compatibility)
     * Returns an array: [0] = final price, [1] = price breakdown JSON string
     * For calculate showtime seat without ticket type
     */
    public Object[] calculatePriceWithBreakdown(Showtime showtime, Seat seat) {
        return calculatePriceWithBreakdown(showtime, seat, null);
    }

    /**
     * Calculate final price only with ticket type
     */
    public BigDecimal calculatePrice(Showtime showtime, Seat seat, TicketType ticketType) {
        Object[] result = calculatePriceWithBreakdown(showtime, seat, ticketType);
        return (BigDecimal) result[0];
    }

    /**
     * Calculate final price only (backward compatibility)
     */
    public BigDecimal calculatePrice(Showtime showtime, Seat seat) {
        return calculatePrice(showtime, seat, null);
    }

    /**
     * Check if a modifier should be applied based on conditions
     * Check if the conditions of the modifier match the showtime, seat, and ticket type attributes
     */
    private boolean isModifierApplicable(PriceModifier modifier, Showtime showtime, Seat seat, TicketType ticketType) {
        switch (modifier.getConditionType()) {
            case DAY_TYPE:
                return checkDayType(modifier.getConditionValue(), showtime.getStartTime());

            case TIME_RANGE:
                return checkTimeRange(modifier.getConditionValue(), showtime.getStartTime());

            case FORMAT:
                return checkFormat(modifier.getConditionValue(), showtime.getFormat());

            case ROOM_TYPE:
                return checkRoomType(modifier.getConditionValue(), showtime.getRoom().getRoomType());

            case SEAT_TYPE:
                return checkSeatType(modifier.getConditionValue(), seat.getSeatType().toString());

            case TICKET_TYPE:
                return checkTicketType(modifier.getConditionValue(), ticketType);

            default:
                return false;
        }
    }

    /**
     * Apply a modifier to the current price
     */
    private BigDecimal applyModifier(BigDecimal currentPrice, PriceModifier modifier) {
        if (modifier.getModifierType() == ModifierType.PERCENTAGE) {
            // Percentage: multiply by (1 + percentage/100)
            // Example: 20% increase = multiply by 1.2
            BigDecimal multiplier = BigDecimal.ONE.add(
                    modifier.getModifierValue().divide(BigDecimal.valueOf(100)));
            return currentPrice.multiply(multiplier);
        } else {
            // Fixed: add the fixed amount
            return currentPrice.add(modifier.getModifierValue());
        }
    }

    /**
     * Check if showtime is on weekend/weekday
     */
    private boolean checkDayType(String conditionValue, LocalDateTime startTime) {
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

        return (conditionValue.equalsIgnoreCase("WEEKEND") && isWeekend) ||
                (conditionValue.equalsIgnoreCase("WEEKDAY") && !isWeekend);
    }

    /**
     * Check time range (MORNING, AFTERNOON, EVENING, NIGHT)
     */
    private boolean checkTimeRange(String conditionValue, LocalDateTime startTime) {
        LocalTime time = startTime.toLocalTime();

        switch (conditionValue.toUpperCase()) {
            case "MORNING":
                return !time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(12, 0));
            case "AFTERNOON":
                return !time.isBefore(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(17, 0));
            case "EVENING":
                return !time.isBefore(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(22, 0));
            case "NIGHT":
                return !time.isBefore(LocalTime.of(22, 0)) || time.isBefore(LocalTime.of(6, 0));
            default:
                return false;
        }
    }

    /**
     * Check showtime format (2D, 3D, IMAX, 4DX)
     */
    private boolean checkFormat(String conditionValue, String showtimeFormat) {
        if (showtimeFormat == null) {
            return false;
        }
        // Check if showtime format contains the condition value (case insensitive)
        return showtimeFormat.toUpperCase().contains(conditionValue.toUpperCase());
    }

    /**
     * Check room type
     */
    private boolean checkRoomType(String conditionValue, String roomType) {
        if (roomType == null) {
            return false;
        }
        return roomType.equalsIgnoreCase(conditionValue);
    }

    /**
     * Check seat type
     */
    private boolean checkSeatType(String conditionValue, String seatType) {
        return seatType.equalsIgnoreCase(conditionValue);
    }

    /**
     * Check ticket type
     */
    private boolean checkTicketType(String conditionValue, TicketType ticketType) {
        if (ticketType == null) {
            return false;
        }
        return ticketType.getTicketTypeId().equalsIgnoreCase(conditionValue);
    }
}
