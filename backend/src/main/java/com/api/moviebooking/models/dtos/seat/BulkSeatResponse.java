package com.api.moviebooking.models.dtos.seat;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkSeatResponse {

    private int totalSeatsGenerated;
    private int normalSeats;
    private int vipSeats;
    private int coupleSeats;
    private List<SeatDataResponse> seats;
}
