package com.api.moviebooking.models.dtos.membershipTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.api.moviebooking.models.enums.DiscountType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MembershipTierDataResponse {

    private UUID id;
    private String name;
    private Integer minPoints;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
