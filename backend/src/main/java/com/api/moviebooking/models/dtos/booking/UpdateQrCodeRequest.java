package com.api.moviebooking.models.dtos.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQrCodeRequest {

    @NotBlank(message = "QR code URL is required")
    private String qrCodeUrl;
}
