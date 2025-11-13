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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.promotion.AddPromotionRequest;
import com.api.moviebooking.models.dtos.promotion.PromotionDataResponse;
import com.api.moviebooking.models.dtos.promotion.UpdatePromotionRequest;
import com.api.moviebooking.services.PromotionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/promotions")
@Tag(name = "Promotion Operations")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Create a new promotion (Admin only)")
    public ResponseEntity<PromotionDataResponse> createPromotion(@Valid @RequestBody AddPromotionRequest request) {
        PromotionDataResponse response = promotionService.addPromotion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{promotionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update promotion details (Admin only)")
    public ResponseEntity<PromotionDataResponse> updatePromotion(
            @PathVariable UUID promotionId,
            @Valid @RequestBody UpdatePromotionRequest request) {
        PromotionDataResponse response = promotionService.updatePromotion(promotionId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{promotionId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Deactivate a promotion (Admin only)")
    public ResponseEntity<Void> deactivatePromotion(@PathVariable UUID promotionId) {
        promotionService.deactivatePromotion(promotionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{promotionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete a promotion (Admin only)")
    public ResponseEntity<Void> deletePromotion(@PathVariable UUID promotionId) {
        promotionService.deletePromotion(promotionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{promotionId}")
    @Operation(summary = "Get promotion by ID")
    public ResponseEntity<PromotionDataResponse> getPromotionById(@PathVariable UUID promotionId) {
        PromotionDataResponse response = promotionService.getPromotion(promotionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get promotion by code")
    public ResponseEntity<PromotionDataResponse> getPromotionByCode(@PathVariable String code) {
        PromotionDataResponse response = promotionService.getPromotionByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all promotions or filter by status(active/valid)")
    public ResponseEntity<List<PromotionDataResponse>> getPromotions(
            @RequestParam(required = false) String filter) {
        List<PromotionDataResponse> response;

        if ("active".equalsIgnoreCase(filter)) {
            response = promotionService.getActivePromotions();
        } else if ("valid".equalsIgnoreCase(filter)) {
            response = promotionService.getValidPromotions();
        } else {
            response = promotionService.getAllPromotions();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active promotions")
    public ResponseEntity<List<PromotionDataResponse>> getActivePromotions() {
        List<PromotionDataResponse> response = promotionService.getActivePromotions();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/valid")
    @Operation(summary = "Get all valid promotions (active and within date range)")
    public ResponseEntity<List<PromotionDataResponse>> getValidPromotions() {
        List<PromotionDataResponse> response = promotionService.getValidPromotions();
        return ResponseEntity.ok(response);
    }
}
