package com.api.moviebooking.models.dtos.user;

import java.time.LocalDateTime;
import java.util.UUID;

import com.api.moviebooking.models.dtos.membershipTier.MembershipTierDataResponse;
import com.api.moviebooking.models.enums.UserRole;   // ⬅️ THÊM IMPORT NÀY

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID userId;
    private String email;
    private String username;
    private String phoneNumber;
    private String avatarUrl;
    private String avatarCloudinaryId;
    private Integer loyaltyPoints;
    private MembershipTierDataResponse membershipTier;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ⬇⬇⬇ THÊM TRƯỜNG ROLE Ở ĐÂY
    private UserRole role;
}
