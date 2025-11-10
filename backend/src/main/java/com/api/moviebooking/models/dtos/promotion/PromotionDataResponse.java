package com.api.moviebooking.models.dtos.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.api.moviebooking.models.enums.DiscountType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PromotionDataResponse {

    private UUID promotionId;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer perUserLimit;
    private Boolean isActive;
}
