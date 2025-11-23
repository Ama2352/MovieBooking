package com.api.moviebooking.models.dtos.booking;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingRequest {

    @NotNull(message = "Lock ID is required")
    private UUID lockId;

    private String promotionCode; // Optional promotion code for discount

    // Optional snack combo selection
    private List<SnackComboItem> snackCombos;

    /**
     * Guest information (required only if session is GUEST_SESSION)
     * For authenticated users, these fields are ignored (user info from JWT)
     */
    private GuestInfo guestInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnackComboItem {
        private UUID snackId;
        private Integer quantity;
    }

    /**
     * Guest information for first-time booking
     * Required fields for creating guest user record
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuestInfo {

        @NotBlank(message = "Email is required for guest booking")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Username is required for guest booking")
        private String username;

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
        private String phoneNumber;
    }
}
