package com.api.moviebooking.models.dtos.priceModifier;

import java.math.BigDecimal;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.ConditionType;
import com.api.moviebooking.models.enums.ModifierType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddPriceModifierRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Condition type is required")
    @EnumValidator(enumClass = ConditionType.class, 
    message = "Condition type must be a DAY_TYPE_, TIME_RANGE, SEAT_TYPE, FORMAT, or ROOM_TYPE")
    @Schema(example = "SEAT_TYPE")
    private String conditionType;  // Will be converted to ConditionType enum

    @NotBlank(message = "Condition value is required")
    private String conditionValue;

    @NotBlank(message = "Modifier type is required")
    @EnumValidator(enumClass = ModifierType.class, 
    message = "Modifier type must be a PERCENTAGE or FIXED_AMOUNT")
    @Schema(example = "PERCENTAGE")
    private String modifierType;   // Will be converted to ModifierType enum

    @NotNull(message = "Modifier value is required")
    private BigDecimal modifierValue;
}
