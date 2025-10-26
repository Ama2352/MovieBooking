package com.api.moviebooking.models.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(example = "admin@gmail.com")
    private String email;

    @NotBlank
    @Schema(example = "admin123")
    private String password;
}
