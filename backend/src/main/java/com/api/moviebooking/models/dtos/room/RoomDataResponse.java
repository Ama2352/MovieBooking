package com.api.moviebooking.models.dtos.room;

import lombok.Data;

@Data
public class RoomDataResponse {
    private String roomId;
    private String cinemaId;
    private String roomType;
    private int roomNumber;
}
