package com.api.moviebooking.models.dtos.snack;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSnackRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
    private String imageUrl;
    private String imageCloudinaryId;
}
