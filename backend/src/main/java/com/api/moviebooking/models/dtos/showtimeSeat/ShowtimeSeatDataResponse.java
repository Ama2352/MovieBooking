package com.api.moviebooking.models.dtos.showtimeSeat;

import java.math.BigDecimal;
import java.util.UUID;

import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeSeatDataResponse {

    private UUID id;
    private UUID showtimeId;
    private UUID seatId;
    private String rowLabel;
    private Integer seatNumber;
    private SeatType seatType;
    private SeatStatus status;
    private BigDecimal price;
    private String priceBreakdown; // JSON string explaining how the price was calculated
}
