package com.api.moviebooking.models.dtos.seat;

import java.util.UUID;

import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLayoutResponse {
    
    private UUID seatId;
    private String row;
    private Integer number;
    private SeatType type; // NORMAL | VIP | COUPLE
    private SeatStatus status; // AVAILABLE | LOCKED | BOOKED
}
