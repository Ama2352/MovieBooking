package com.api.moviebooking.models.dtos.room;

import lombok.Data;

@Data
public class RoomDataResponse {
    private String id;
    private String cinemaId;
    private String roomType;
    private int roomNumber;
}
