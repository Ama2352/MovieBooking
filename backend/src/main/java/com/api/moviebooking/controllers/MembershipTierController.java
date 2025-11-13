package com.api.moviebooking.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.membershipTier.AddMembershipTierRequest;
import com.api.moviebooking.models.dtos.membershipTier.MembershipTierDataResponse;
import com.api.moviebooking.models.dtos.membershipTier.UpdateMembershipTierRequest;
import com.api.moviebooking.services.MembershipTierService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/membership-tiers")
@Tag(name = "Membership Tier Operations")
public class MembershipTierController {

    private final MembershipTierService membershipTierService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Create a new membership tier (Admin only)")
    public ResponseEntity<MembershipTierDataResponse> createMembershipTier(
            @Valid @RequestBody AddMembershipTierRequest request) {
        MembershipTierDataResponse response = membershipTierService.addMembershipTier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{tierId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update membership tier details (Admin only)")
    public ResponseEntity<MembershipTierDataResponse> updateMembershipTier(
            @PathVariable UUID tierId,
            @Valid @RequestBody UpdateMembershipTierRequest request) {
        MembershipTierDataResponse response = membershipTierService.updateMembershipTier(tierId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tierId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Deactivate a membership tier (Admin only)")
    public ResponseEntity<Void> deactivateMembershipTier(@PathVariable UUID tierId) {
        membershipTierService.deactivateMembershipTier(tierId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tierId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete a membership tier (Admin only)")
    public ResponseEntity<Void> deleteMembershipTier(@PathVariable UUID tierId) {
        membershipTierService.deleteMembershipTier(tierId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tierId}")
    @Operation(summary = "Get membership tier by ID")
    public ResponseEntity<MembershipTierDataResponse> getMembershipTierById(@PathVariable UUID tierId) {
        MembershipTierDataResponse response = membershipTierService.getMembershipTier(tierId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get membership tier by name")
    public ResponseEntity<MembershipTierDataResponse> getMembershipTierByName(@PathVariable String name) {
        MembershipTierDataResponse response = membershipTierService.getMembershipTierByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all membership tiers")
    public ResponseEntity<List<MembershipTierDataResponse>> getAllMembershipTiers() {
        List<MembershipTierDataResponse> response = membershipTierService.getAllMembershipTiers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active membership tiers")
    public ResponseEntity<List<MembershipTierDataResponse>> getActiveMembershipTiers() {
        List<MembershipTierDataResponse> response = membershipTierService.getActiveMembershipTiers();
        return ResponseEntity.ok(response);
    }
}
