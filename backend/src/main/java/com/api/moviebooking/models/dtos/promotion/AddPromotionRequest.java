package com.api.moviebooking.models.dtos.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddPromotionRequest {

    @NotBlank(message = "Promotion code is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must contain only uppercase letters, numbers, hyphens and underscores")
    private String code;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Discount type is required")
    @EnumValidator(enumClass = DiscountType.class, message = "Discount type must be PERCENTAGE or FIXED_AMOUNT")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @NotNull(message = "Per user limit is required")
    @Min(value = 1, message = "Per user limit must be at least 1")
    private Integer perUserLimit;

    private Boolean isActive;
}
