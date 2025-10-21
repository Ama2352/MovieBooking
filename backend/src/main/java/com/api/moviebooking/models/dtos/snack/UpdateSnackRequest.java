package com.api.moviebooking.models.dtos.snack;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateSnackRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
}
