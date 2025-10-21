package com.api.moviebooking.models.dtos.room;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRoomRequest {
    private String roomType;
    private Integer roomNumber;
}
