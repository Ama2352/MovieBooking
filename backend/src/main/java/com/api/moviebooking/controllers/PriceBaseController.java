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
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.priceBase.AddPriceBaseRequest;
import com.api.moviebooking.models.dtos.priceBase.PriceBaseDataResponse;
import com.api.moviebooking.models.dtos.priceBase.UpdatePriceBaseRequest;
import com.api.moviebooking.services.PriceBaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/price-base")
@RequiredArgsConstructor
@Tag(name = "Price Base Operations")
public class PriceBaseController {

    private final PriceBaseService priceBaseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Add a new base price (Admin only)")
    public ResponseEntity<PriceBaseDataResponse> addPriceBase(@Valid @RequestBody AddPriceBaseRequest request) {
        PriceBaseDataResponse response = priceBaseService.addPriceBase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update base price (Admin only)")
    public ResponseEntity<PriceBaseDataResponse> updatePriceBase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriceBaseRequest request) {
        PriceBaseDataResponse response = priceBaseService.updatePriceBase(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete base price (Admin only)")
    public ResponseEntity<Void> deletePriceBase(@PathVariable UUID id) {
        priceBaseService.deletePriceBase(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get base price by ID")
    public ResponseEntity<PriceBaseDataResponse> getPriceBase(@PathVariable UUID id) {
        PriceBaseDataResponse response = priceBaseService.getPriceBase(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all base prices")
    public ResponseEntity<List<PriceBaseDataResponse>> getAllPriceBases() {
        List<PriceBaseDataResponse> response = priceBaseService.getAllPriceBases();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get current active base price")
    public ResponseEntity<PriceBaseDataResponse> getActiveBasePrice() {
        PriceBaseDataResponse response = priceBaseService.getActiveBasePrice();
        return ResponseEntity.ok(response);
    }
}
