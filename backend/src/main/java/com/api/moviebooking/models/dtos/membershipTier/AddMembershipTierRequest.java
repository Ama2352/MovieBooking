package com.api.moviebooking.models.dtos.membershipTier;

import java.math.BigDecimal;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.DiscountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddMembershipTierRequest {

    @NotBlank(message = "Tier name is required")
    private String name; // SILVER, GOLD, PLATINUM

    @NotNull(message = "Minimum points is required")
    @Min(value = 0, message = "Minimum points must be at least 0")
    private Integer minPoints;

    @Schema(example = "PERCENTAGE")
    @NotNull(message = "Discount type is required")
    @EnumValidator(enumClass = DiscountType.class, message = "Discount type must be PERCENTAGE or FIXED_AMOUNT")
    private String discountType; 

    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @NotNull(message = "Discount value is required")
    private BigDecimal discountValue; 

    private String description; // Nullable

    private Boolean isActive;
}
