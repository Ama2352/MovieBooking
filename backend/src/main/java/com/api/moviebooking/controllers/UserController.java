package com.api.moviebooking.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.user.UpdatePasswordRequest;
import com.api.moviebooking.models.dtos.user.UpdateProfileRequest;
import com.api.moviebooking.models.dtos.user.UserProfileResponse;
import com.api.moviebooking.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@SecurityRequirement(name = "bearerToken")
@Tag(name = "User Management")
public class UserController {

    private final UserService userService;

    /**
     * Get current user's profile
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Retrieve authenticated user's profile information")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user's profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update authenticated user's profile information")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updatedProfile = userService.updateUserProfile(request);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Update current user's password
     */
    @PatchMapping("/password")
    @Operation(summary = "Update password", description = "Change authenticated user's password")
    public ResponseEntity<String> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(request);
        return ResponseEntity.ok("Password updated successfully");
    }

    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Retrieve user information by ID (Admin only)")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID userId) {
        UserProfileResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieve all users in the system (Admin only)")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Update user role (Admin only)
     */
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role", description = "Change user's role (Admin only)")
    public ResponseEntity<UserProfileResponse> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody String role) {
        UserProfileResponse updatedUser = userService.updateUserRole(userId, role);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete user by ID (Admin only)")
    public ResponseEntity<String> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok("User deleted successfully");
    }

    /**
     * Get user's loyalty points and tier information
     */
    @GetMapping("/loyalty")
    @Operation(summary = "Get loyalty information", description = "Retrieve loyalty points and membership tier")
    public ResponseEntity<UserProfileResponse> getLoyaltyInfo() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }
}
