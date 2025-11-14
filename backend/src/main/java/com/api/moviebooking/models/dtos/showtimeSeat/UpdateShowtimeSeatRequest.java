package com.api.moviebooking.models.dtos.showtimeSeat;

import java.math.BigDecimal;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.SeatStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShowtimeSeatRequest {

    @EnumValidator(enumClass = SeatStatus.class, 
    message = "Seat status must be AVAILABLE, LOCKED, or BOOKED")
    @Schema(example = "AVAILABLE")
    private String status;  // Will be converted to SeatStatus enum
    private BigDecimal price;
}
