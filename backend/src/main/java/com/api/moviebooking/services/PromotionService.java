package com.api.moviebooking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.PromotionMapper;
import com.api.moviebooking.models.dtos.promotion.AddPromotionRequest;
import com.api.moviebooking.models.dtos.promotion.PromotionDataResponse;
import com.api.moviebooking.models.dtos.promotion.UpdatePromotionRequest;
import com.api.moviebooking.models.entities.Promotion;
import com.api.moviebooking.models.enums.DiscountType;
import com.api.moviebooking.repositories.PromotionRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepo promotionRepo;
    private final PromotionMapper promotionMapper;

    /**
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes: isPresent
     * Minimum test cases: 2
     * 1. Promotion exists (success path)
     * 2. Promotion not found (throws ResourceNotFoundException)
     */
    private Promotion findPromotionById(UUID promotionId) {
        return promotionRepo.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));
    }

    /**
     * Predicate nodes (d): 5 -> V(G)=d+1=6
     * Nodes: existsByCodeIgnoreCase, endDate.isBefore(startDate), discountType.equals("PERCENTAGE"), 
     *        discountValue.compareTo(100) > 0, perUserLimit > usageLimit
     * Minimum test cases: 6
     */
    @Transactional
    public PromotionDataResponse addPromotion(AddPromotionRequest request) {
        // Validate unique code
        if (promotionRepo.existsByCodeIgnoreCase(request.getCode())) {
            throw new IllegalArgumentException("Promotion code already exists: " + request.getCode());
        }

        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Validate discount value based on type
        if (request.getDiscountType().equals("PERCENTAGE")) {
            if (request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
        }

        // Validate per user limit doesn't exceed usage limit
        if (request.getPerUserLimit() > request.getUsageLimit()) {
            throw new IllegalArgumentException("Per user limit cannot exceed total usage limit");
        }

        Promotion newPromotion = promotionMapper.toEntity(request);
        promotionRepo.save(newPromotion);
        return promotionMapper.toDataResponse(newPromotion);
    }

    /**
     * Predicate nodes (d): 14 -> V(G)=d+1=15
     * Nodes:  * - request.getCode() != null
    * - !code.equalsIgnoreCase && existsByCode
    * - request.getDescription() != null
    * - request.getDiscountType() != null
    * - newType == PERCENTAGE && value > 100
    * - request.getDiscountValue() != null
    * - type == PERCENTAGE && value > 100
    * - request.getStartDate() != null
    * - request.getEndDate() != null
    * - endDate.isBefore(startDate)
    * - request.getUsageLimit() != null
    * - request.getPerUserLimit() != null
    * - perUserLimit > usageLimit
    * - request.getIsActive() != null
     * Minimum test cases: 15
     */
    @Transactional
    public PromotionDataResponse updatePromotion(UUID promotionId, UpdatePromotionRequest request) {
        Promotion promotion = findPromotionById(promotionId);

        if (request.getCode() != null) {
            if (!request.getCode().equalsIgnoreCase(promotion.getCode())
                    && promotionRepo.existsByCodeIgnoreCase(request.getCode())) {
                throw new IllegalArgumentException("Promotion code already exists: " + request.getCode());
            }
            promotion.setCode(request.getCode());
        }

        if (request.getDescription() != null) {
            promotion.setDescription(request.getDescription());
        }

        if (request.getDiscountType() != null) {
            DiscountType newType = DiscountType.valueOf(request.getDiscountType());
            promotion.setDiscountType(newType);

            // Validate discount value if type is PERCENTAGE
            if (newType == DiscountType.PERCENTAGE && promotion.getDiscountValue()
                    .compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
        }

        if (request.getDiscountValue() != null) {
            // Validate discount value if current type is PERCENTAGE
            if (promotion.getDiscountType() == DiscountType.PERCENTAGE
                    && request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
            promotion.setDiscountValue(request.getDiscountValue());
        }

        if (request.getStartDate() != null) {
            promotion.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            promotion.setEndDate(request.getEndDate());
        }

        // Validate date range after updates
        if (promotion.getEndDate().isBefore(promotion.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (request.getUsageLimit() != null) {
            promotion.setUsageLimit(request.getUsageLimit());
        }

        if (request.getPerUserLimit() != null) {
            promotion.setPerUserLimit(request.getPerUserLimit());
        }

        // Validate per user limit doesn't exceed usage limit
        if (promotion.getPerUserLimit() > promotion.getUsageLimit()) {
            throw new IllegalArgumentException("Per user limit cannot exceed total usage limit");
        }

        if (request.getIsActive() != null) {
            promotion.setIsActive(request.getIsActive());
        }

        promotionRepo.save(promotion);
        return promotionMapper.toDataResponse(promotion);
    }

    /**
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes: isPresent (from findPromotionById)
     * Minimum test cases: 2
     * 1. Promotion exists (success path)
     * 2. Promotion not found (throws ResourceNotFoundException)
     */
    @Transactional
    public void deactivatePromotion(UUID promotionId) {
        Promotion promotion = findPromotionById(promotionId);
        promotion.setIsActive(false);
        promotionRepo.save(promotion);
    }

    /**
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes: isPresent (from findPromotionById)
     * Minimum test cases: 2
     * 1. Promotion exists (success path)
     * 2. Promotion not found (throws ResourceNotFoundException)
     */
    @Transactional
    public void deletePromotion(UUID promotionId) {
        Promotion promotion = findPromotionById(promotionId);
        // TODO: Check if promotion is used in any bookings when booking module is implemented
        // Similar to MovieService checking for showtimes before deletion
        promotionRepo.delete(promotion);
    }

    /**
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes: isPresent (from findPromotionById)
     * Minimum test cases: 2
     * 1. Promotion exists (success path)
     * 2. Promotion not found (throws ResourceNotFoundException)
     */
    public PromotionDataResponse getPromotion(UUID promotionId) {
        Promotion promotion = findPromotionById(promotionId);
        return promotionMapper.toDataResponse(promotion);
    }

    /**
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes: isPresent
     * Minimum test cases: 2
     * 1. Promotion exists with code (success path)
     * 2. Promotion not found with code (throws ResourceNotFoundException)
     */
    public PromotionDataResponse getPromotionByCode(String code) {
        Promotion promotion = promotionRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "code", code));
        return promotionMapper.toDataResponse(promotion);
    }

    /**
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: None (linear execution)
     * Minimum test cases: 1
     * 1. Return all promotions (success path)
     */
    public List<PromotionDataResponse> getAllPromotions() {
        return promotionRepo.findAll().stream()
                .map(promotionMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: None (linear execution)
     * Minimum test cases: 1
     * 1. Return all active promotions (success path)
     */
    //Promotions is active but not useable atm
    public List<PromotionDataResponse> getActivePromotions() {
        return promotionRepo.findByIsActive(true).stream()
                .map(promotionMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: None (linear execution)
     * Minimum test cases: 1
     * 1. Return all valid promotions (active and within date range) (success path)
     */
    //Promotions is active and within date range
    public List<PromotionDataResponse> getValidPromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepo.findByIsActiveAndStartDateBeforeAndEndDateAfter(true, now, now).stream()
                .map(promotionMapper::toDataResponse)
                .collect(Collectors.toList());
    }
}
