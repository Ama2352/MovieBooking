package com.api.moviebooking.models.dtos.priceModifier;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConditionTypeInfo {
    private String name;
    private String description;
    private List<String> exampleValues;
}
