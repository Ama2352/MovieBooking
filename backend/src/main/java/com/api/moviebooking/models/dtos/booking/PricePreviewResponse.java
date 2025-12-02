package com.api.moviebooking.models.dtos.booking;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricePreviewResponse {
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
}
