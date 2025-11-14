package com.api.moviebooking.models.dtos.snack;

import java.math.BigDecimal;
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
public class AddSnackRequest {

    @NotNull
    private UUID cinemaId;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    private BigDecimal price;

    @NotBlank
    private String type;

    @NotBlank
    private String imageUrl;

    @NotBlank
    private String imageCloudinaryId;
}
