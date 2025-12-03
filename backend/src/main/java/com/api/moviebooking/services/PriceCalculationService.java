package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.api.moviebooking.models.dtos.booking.DiscountResult;
import com.api.moviebooking.models.dtos.showtimeSeat.PriceBreakdown;
import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.entities.PriceBase;
import com.api.moviebooking.models.entities.PriceModifier;
import com.api.moviebooking.models.entities.Promotion;
import com.api.moviebooking.models.entities.Seat;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.DiscountType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.repositories.PriceBaseRepo;
import com.api.moviebooking.repositories.PriceModifierRepo;
import com.api.moviebooking.repositories.PromotionRepo;
import com.api.moviebooking.repositories.UserRepo;
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
    private final UserRepo userRepo;
    private final PromotionRepo promotionRepo;

    /**
     * Calculate final price and generate price breakdown for a showtime seat
     * This calculates BASE price only with seat/showtime modifiers
     * Ticket type modifiers should be applied separately by the caller
     * Returns an array: [0] = final price, [1] = price breakdown JSON string
     */
    public Object[] calculatePriceWithBreakdown(Showtime showtime, Seat seat) {
        // Get base price from PriceBase table
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
            if (isModifierApplicable(modifier, showtime, seat)) {
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

        log.info("Final calculated base price for showtime {} seat {}{}: {}",
                showtime.getId(), seat.getRowLabel(), seat.getSeatNumber(), finalPrice);

        return new Object[] { finalPrice, breakdownJson };
    }

    /**
     * Calculate final base price only
     */
    public BigDecimal calculatePrice(Showtime showtime, Seat seat) {
        Object[] result = calculatePriceWithBreakdown(showtime, seat);
        return (BigDecimal) result[0];
    }

    /**
     * Check if a modifier should be applied based on conditions
     * Checks if the conditions of the modifier match the showtime and seat
     * attributes
     * Does NOT check ticket type - that's handled separately
     */
    private boolean isModifierApplicable(PriceModifier modifier, Showtime showtime, Seat seat) {
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
     * Calculate discounts (membership tier + promotion code) for a given subtotal.
     * This is the single source of truth for discount calculation logic.
     * Used by both price preview and booking confirmation.
     *
     * @param subtotal      The base amount before discounts
     * @param userId        Optional user ID to apply membership discount (null for
     *                      guests)
     * @param promotionCode Optional promotion code to apply
     * @return DiscountResult containing total discount, breakdown, and reason
     */
    public DiscountResult calculateDiscounts(BigDecimal subtotal, UUID userId, String promotionCode) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal membershipDiscount = BigDecimal.ZERO;
        BigDecimal promotionDiscount = BigDecimal.ZERO;
        StringBuilder discountReason = new StringBuilder();

        // Apply membership tier discount if userId is provided
        if (userId != null) {
            User user = userRepo.findById(userId).orElse(null);
            if (user != null && user.getMembershipTier() != null) {
                membershipDiscount = calculateMembershipDiscount(user.getMembershipTier(), subtotal);
                if (membershipDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    totalDiscount = totalDiscount.add(membershipDiscount);
                    MembershipTier tier = user.getMembershipTier();
                    discountReason.append("Membership ").append(tier.getName())
                            .append(" (-").append(tier.getDiscountValue())
                            .append(tier.getDiscountType() == DiscountType.PERCENTAGE ? "%" : " VND")
                            .append(")");
                    log.debug("Applied membership discount: {} for tier {}", membershipDiscount, tier.getName());
                }
            }
        }

        // Apply promotion code discount if provided
        if (promotionCode != null && !promotionCode.trim().isEmpty()) {
            var promotionOpt = promotionRepo.findByCode(promotionCode);
            if (promotionOpt.isPresent()) {
                Promotion promotion = promotionOpt.get();
                promotionDiscount = calculatePromotionDiscount(promotion, subtotal);
                totalDiscount = totalDiscount.add(promotionDiscount);
                if (discountReason.length() > 0) {
                    discountReason.append(", ");
                }
                discountReason.append("Promotion: ").append(promotion.getName());
                log.debug("Applied promotion discount: {} for code {}", promotionDiscount, promotionCode);
            }
        }

        return DiscountResult.builder()
                .totalDiscount(totalDiscount)
                .membershipDiscount(membershipDiscount)
                .promotionDiscount(promotionDiscount)
                .discountReason(discountReason.length() > 0 ? discountReason.toString() : null)
                .build();
    }

    /**
     * Calculate membership tier discount for a given amount
     */
    private BigDecimal calculateMembershipDiscount(MembershipTier membershipTier, BigDecimal amount) {
        if (membershipTier == null || !membershipTier.getIsActive()) {
            return BigDecimal.ZERO;
        }

        if (membershipTier.getDiscountType() == null || membershipTier.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }

        if (membershipTier.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return switch (membershipTier.getDiscountType()) {
            case PERCENTAGE -> amount.multiply(membershipTier.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> membershipTier.getDiscountValue().min(amount);
        };
    }

    /**
     * Calculate promotion discount for a given amount
     */
    private BigDecimal calculatePromotionDiscount(Promotion promotion, BigDecimal amount) {
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            return amount.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            return promotion.getDiscountValue().min(amount);
        }
    }
}
