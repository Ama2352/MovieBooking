package com.api.moviebooking.models.dtos.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoomRequest {
    private String roomType;
    private Integer roomNumber;
}
