package com.api.moviebooking.models.dtos.membershipTier;

import java.math.BigDecimal;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.DiscountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateMembershipTierRequest {

    private String name;

    @Min(value = 0, message = "Minimum points must be at least 0")
    private Integer minPoints;

    @Schema(example = "PERCENTAGE")
    @EnumValidator(enumClass = DiscountType.class, message = "Discount type must be PERCENTAGE or FIXED_AMOUNT")
    private String discountType;

    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    private String description;

    private Boolean isActive;
}
