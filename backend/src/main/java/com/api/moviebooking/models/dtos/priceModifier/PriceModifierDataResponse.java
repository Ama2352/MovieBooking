package com.api.moviebooking.models.dtos.priceModifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.api.moviebooking.models.enums.ConditionType;
import com.api.moviebooking.models.enums.ModifierType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceModifierDataResponse {

    private UUID priceModifierId;
    private String name;
    private ConditionType conditionType;
    private String conditionValue;
    private ModifierType modifierType;
    private BigDecimal modifierValue;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
