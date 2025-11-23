package com.api.moviebooking.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.priceModifier.AddPriceModifierRequest;
import com.api.moviebooking.models.dtos.priceModifier.PriceModifierDataResponse;
import com.api.moviebooking.models.dtos.priceModifier.UpdatePriceModifierRequest;
import com.api.moviebooking.models.enums.ConditionType;
import com.api.moviebooking.services.PriceModifierService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/price-modifiers")
@RequiredArgsConstructor
@Tag(name = "Price Modifier Operations")
public class PriceModifierController {

    private final PriceModifierService priceModifierService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Add a new price modifier (Admin only)", description = "Create a modifier that affects ticket prices based on conditions like day type, time range, format, room type, or seat type")
    public ResponseEntity<PriceModifierDataResponse> addPriceModifier(
            @Valid @RequestBody AddPriceModifierRequest request) {
        PriceModifierDataResponse response = priceModifierService.addPriceModifier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update price modifier (Admin only)")
    public ResponseEntity<PriceModifierDataResponse> updatePriceModifier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriceModifierRequest request) {
        PriceModifierDataResponse response = priceModifierService.updatePriceModifier(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete price modifier (Admin only)")
    public ResponseEntity<Void> deletePriceModifier(@PathVariable UUID id) {
        priceModifierService.deletePriceModifier(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get price modifier by ID")
    public ResponseEntity<PriceModifierDataResponse> getPriceModifier(@PathVariable UUID id) {
        PriceModifierDataResponse response = priceModifierService.getPriceModifier(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all price modifiers")
    public ResponseEntity<List<PriceModifierDataResponse>> getAllPriceModifiers() {
        List<PriceModifierDataResponse> response = priceModifierService.getAllPriceModifiers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active price modifiers")
    public ResponseEntity<List<PriceModifierDataResponse>> getActivePriceModifiers() {
        List<PriceModifierDataResponse> response = priceModifierService.getActivePriceModifiers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-condition")
    @Operation(summary = "Get price modifiers by condition type")
    public ResponseEntity<List<PriceModifierDataResponse>> getPriceModifiersByConditionType(
            @RequestParam String conditionType) {
        try {
            ConditionType type = ConditionType.valueOf(conditionType.toUpperCase());
            List<PriceModifierDataResponse> response = priceModifierService.getPriceModifiersByConditionType(type);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid condition type: " + conditionType);
        }
    }
}
