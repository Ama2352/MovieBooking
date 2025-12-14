package com.api.moviebooking.models.dtos.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String username,
        String email,
        String phoneNumber,
        String provider,
        String role,
        String avatarUrl,
        String avatarCloudinaryId,
        Integer loyaltyPoints,
        String membershipTierId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
