package com.api.moviebooking.models.dtos.priceModifier;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePriceModifierRequest {

    private String name;
    
    // Note: The following fields cannot be modified after creation to maintain data integrity:
    // - conditionType, conditionValue: Define when the modifier applies
    // - modifierType, modifierValue: Define the price adjustment amount
    // Create a new PriceModifier if you need different conditions or values
    
    private Boolean isActive;
}
