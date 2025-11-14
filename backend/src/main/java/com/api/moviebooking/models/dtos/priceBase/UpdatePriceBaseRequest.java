package com.api.moviebooking.models.dtos.priceBase;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePriceBaseRequest {

    private String name;
    // Note: basePrice cannot be modified after creation to maintain data integrity
    // Create a new PriceBase if you need different pricing
    private Boolean isActive;
}
