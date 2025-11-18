package com.api.moviebooking.models.dtos.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Pattern(regexp = "^(03|05|07|08|09)[0-9]{8}$", message = "Invalid Vietnamese phone number")
    private String phoneNumber;

    private String avatarUrl;
}
