package com.api.moviebooking.models.dtos.seat;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.SeatType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSeatRequest {

    private Integer seatNumber;
    private String rowLabel;
    @EnumValidator(enumClass = SeatType.class, message = "Seat type must be a NORMAL,VIP, COUPLE")
    @Schema(example = "NORMAL")
    private String seatType;
}
