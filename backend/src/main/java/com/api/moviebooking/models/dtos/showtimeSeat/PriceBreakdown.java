package com.api.moviebooking.models.dtos.showtimeSeat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceBreakdown {

    private BigDecimal basePrice;
    private List<ModifierInfo> modifiers = new ArrayList<>();
    private BigDecimal finalPrice;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierInfo {
        private String name;
        private String type;
        private BigDecimal value;
    }
}
