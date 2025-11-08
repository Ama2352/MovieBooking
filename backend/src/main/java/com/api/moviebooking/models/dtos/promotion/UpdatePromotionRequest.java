package com.api.moviebooking.models.dtos.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdatePromotionRequest {

    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must contain only uppercase letters, numbers, hyphens and underscores")
    private String code;

    private String name;

    private String description;

    @EnumValidator(enumClass = DiscountType.class, message = "Discount type must be PERCENTAGE or FIXED_AMOUNT")
    private String discountType;

    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Per user limit must be at least 1")
    private Integer perUserLimit;

    private Boolean isActive;
}
