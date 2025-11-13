package com.api.moviebooking.models.dtos.seat;

import java.util.UUID;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.SeatType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddSeatRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Seat number is required")
    @Positive(message = "Seat number must be positive")
    private Integer seatNumber;

    @NotBlank(message = "Row label is required")
    private String rowLabel;

    @NotNull(message = "Seat type is required")
    @EnumValidator(enumClass = SeatType.class, message = "Seat type must be a NORMAL,VIP, COUPLE")
    @Schema(example = "NORMAL")
    private String seatType;
}
