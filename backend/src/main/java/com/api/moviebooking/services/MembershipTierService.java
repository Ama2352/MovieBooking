package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.MembershipTierMapper;
import com.api.moviebooking.models.dtos.membershipTier.AddMembershipTierRequest;
import com.api.moviebooking.models.dtos.membershipTier.MembershipTierDataResponse;
import com.api.moviebooking.models.dtos.membershipTier.UpdateMembershipTierRequest;
import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.enums.DiscountType;
import com.api.moviebooking.repositories.MembershipTierRepo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipTierService {

    private final MembershipTierRepo membershipTierRepo;
    private final MembershipTierMapper membershipTierMapper;

    /**
     * Initialize default membership tiers if none exist
     */
    @PostConstruct
    @Transactional
    public void initializeDefaultTiers() {
        long tierCount = membershipTierRepo.count();

        if (tierCount == 0) {
            log.info("No membership tiers found. Creating default tiers...");

            // Create Bronze tier (default for new users)
            MembershipTier bronze = new MembershipTier();
            bronze.setName("Bronze");
            bronze.setMinPoints(0);
            bronze.setDiscountType(DiscountType.PERCENTAGE);
            bronze.setDiscountValue(BigDecimal.ZERO);
            bronze.setDescription("Default tier for new members");
            bronze.setIsActive(true);
            membershipTierRepo.save(bronze);
            log.info("Created default Bronze tier");

            // Create Silver tier
            MembershipTier silver = new MembershipTier();
            silver.setName("Silver");
            silver.setMinPoints(500);
            silver.setDiscountType(DiscountType.PERCENTAGE);
            silver.setDiscountValue(BigDecimal.valueOf(5.0));
            silver.setDescription("Silver tier with 5% discount");
            silver.setIsActive(true);
            membershipTierRepo.save(silver);
            log.info("Created Silver tier");

            // Create Gold tier
            MembershipTier gold = new MembershipTier();
            gold.setName("Gold");
            gold.setMinPoints(1000);
            gold.setDiscountType(DiscountType.PERCENTAGE);
            gold.setDiscountValue(BigDecimal.valueOf(10.0));
            gold.setDescription("Gold tier with 10% discount");
            gold.setIsActive(true);
            membershipTierRepo.save(gold);
            log.info("Created Gold tier");

            // Create Platinum tier
            MembershipTier platinum = new MembershipTier();
            platinum.setName("Platinum");
            platinum.setMinPoints(2000);
            platinum.setDiscountType(DiscountType.PERCENTAGE);
            platinum.setDiscountValue(BigDecimal.valueOf(15.0));
            platinum.setDescription("Platinum tier with 15% discount");
            platinum.setIsActive(true);
            membershipTierRepo.save(platinum);
            log.info("Created Platinum tier");

            log.info("Default membership tiers initialization completed");
        } else {
            log.info("Membership tiers already exist. Skipping initialization.");
        }
    }

    private MembershipTier findMembershipTierById(UUID tierId) {
        return membershipTierRepo.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "id", tierId));
    }

    /**
     * Create a new membership tier (API: POST /membership-tiers)
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: existsByNameIgnoreCase, discountType!=null && discountValue!=null,
     * equals("PERCENTAGE") && compareTo(100)>0
     */
    @Transactional
    public MembershipTierDataResponse addMembershipTier(AddMembershipTierRequest request) {
        // Validate unique name
        if (membershipTierRepo.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Membership tier with this name already exists: " + request.getName());
        }

        // Validate discount value based on type
        if (request.getDiscountType() != null && request.getDiscountValue() != null) {
            if (request.getDiscountType().equals("PERCENTAGE")) {
                if (request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
                }
            }
        }

        MembershipTier newTier = membershipTierMapper.toEntity(request);
        membershipTierRepo.save(newTier);
        return membershipTierMapper.toDataResponse(newTier);
    }

    /**
     * Update membership tier details (API: PUT /membership-tiers/{tierId})
     * Predicate nodes (d): 9 -> V(G) = d + 1 = 10
     * Nodes: name!=null, !equalsIgnoreCase && existsByNameIgnoreCase,
     * minPoints!=null,
     * discountType!=null, newType==PERCENTAGE && value!=null && compareTo(100)>0,
     * discountValue!=null, type==PERCENTAGE && compareTo(100)>0, description!=null,
     * isActive!=null
     */
    @Transactional
    public MembershipTierDataResponse updateMembershipTier(UUID tierId, UpdateMembershipTierRequest request) {
        MembershipTier tier = findMembershipTierById(tierId);

        if (request.getName() != null) {
            if (!request.getName().equalsIgnoreCase(tier.getName())
                    && membershipTierRepo.existsByNameIgnoreCase(request.getName())) {
                throw new IllegalArgumentException(
                        "Membership tier with this name already exists: " + request.getName());
            }
            tier.setName(request.getName());
        }

        if (request.getMinPoints() != null) {
            tier.setMinPoints(request.getMinPoints());
        }

        if (request.getDiscountType() != null) {
            DiscountType newType = DiscountType.valueOf(request.getDiscountType());
            tier.setDiscountType(newType);

            // Validate discount value if type is PERCENTAGE
            if (newType == DiscountType.PERCENTAGE && tier.getDiscountValue() != null
                    && tier.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
        }

        if (request.getDiscountValue() != null) {
            // Validate discount value if current type is PERCENTAGE
            if (tier.getDiscountType() == DiscountType.PERCENTAGE
                    && request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
            tier.setDiscountValue(request.getDiscountValue());
        }

        if (request.getDescription() != null) {
            tier.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            tier.setIsActive(request.getIsActive());
        }

        membershipTierRepo.save(tier);
        return membershipTierMapper.toDataResponse(tier);
    }

    /**
     * Deactivate a membership tier (API: PATCH
     * /membership-tiers/{tierId}/deactivate)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findMembershipTierById
     */
    @Transactional
    public void deactivateMembershipTier(UUID tierId) {
        MembershipTier tier = findMembershipTierById(tierId);
        tier.setIsActive(false);
        membershipTierRepo.save(tier);
    }

    /**
     * Delete a membership tier (API: DELETE /membership-tiers/{tierId})
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: findMembershipTierById, !isEmpty(users)
     */
    @Transactional
    public void deleteMembershipTier(UUID tierId) {
        MembershipTier tier = findMembershipTierById(tierId);

        // Check if tier is assigned to any users
        if (!tier.getUsers().isEmpty()) {
            throw new IllegalStateException("Cannot delete membership tier that is assigned to users");
        }

        membershipTierRepo.delete(tier);
    }

    /**
     * Get membership tier by ID (API: GET /membership-tiers/{tierId})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findMembershipTierById
     */
    public MembershipTierDataResponse getMembershipTier(UUID tierId) {
        MembershipTier tier = findMembershipTierById(tierId);
        return membershipTierMapper.toDataResponse(tier);
    }

    /**
     * Get membership tier by name (API: GET /membership-tiers/name/{name})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findByName
     */
    public MembershipTierDataResponse getMembershipTierByName(String name) {
        MembershipTier tier = membershipTierRepo.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "name", name));
        return membershipTierMapper.toDataResponse(tier);
    }

    /**
     * Get all membership tiers (API: GET /membership-tiers)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<MembershipTierDataResponse> getAllMembershipTiers() {
        return membershipTierRepo.findAll().stream()
                .map(membershipTierMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active membership tiers (API: GET /membership-tiers/active)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<MembershipTierDataResponse> getActiveMembershipTiers() {
        return membershipTierRepo.findByIsActive(true).stream()
                .map(membershipTierMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get appropriate tier for user based on loyalty points
     */
    public MembershipTier getApproppriateTier(Integer loyaltyPoints) {
        List<MembershipTier> eligibleTiers = membershipTierRepo.findEligibleTiers(loyaltyPoints);

        if (!eligibleTiers.isEmpty()) {
            return eligibleTiers.get(0); // Highest eligible tier
        }

        // Return default tier if no eligible tier found
        return membershipTierRepo.findDefaultTier()
                .orElseThrow(() -> new IllegalStateException("No default membership tier configured"));
    }

    /**
     * Get default tier for new users
     */
    public MembershipTier getDefaultTier() {
        return membershipTierRepo.findDefaultTier()
                .orElseThrow(() -> new IllegalStateException(
                        "No default membership tier configured. Please create at least one tier."));
    }

    /**
     * Calculate membership tier discount for a given amount
     * 
     * @param membershipTier The user's membership tier (can be null)
     * @param amount         The amount to apply discount on
     * @return The discount amount (0 if no tier or no discount configured)
     */
    public BigDecimal calculateMembershipDiscount(MembershipTier membershipTier, BigDecimal amount) {
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
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> membershipTier.getDiscountValue().min(amount); // Don't exceed the amount
        };
    }
}
