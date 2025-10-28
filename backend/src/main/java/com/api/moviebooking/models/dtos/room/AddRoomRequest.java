package com.api.moviebooking.models.dtos.room;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddRoomRequest {

    @NotNull
    private UUID cinemaId;

    @NotBlank
    private String roomType;

    @NotNull
    private Integer roomNumber;
}
