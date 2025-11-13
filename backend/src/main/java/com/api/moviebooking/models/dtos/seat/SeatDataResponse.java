package com.api.moviebooking.models.dtos.seat;

import java.util.UUID;

import com.api.moviebooking.models.enums.SeatType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatDataResponse {

    private UUID id;
    private UUID roomId;
    private String roomNumber;
    private String cinemaName;
    private Integer seatNumber;
    private String rowLabel;
    private SeatType seatType;
}
