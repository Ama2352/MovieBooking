package com.api.moviebooking.models.dtos.snack;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SnackDataResponse {
    private String id;
    private String cinemaId;
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
}
