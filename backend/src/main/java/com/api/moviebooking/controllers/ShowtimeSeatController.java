package com.api.moviebooking.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.showtimeSeat.ShowtimeSeatDataResponse;
import com.api.moviebooking.models.dtos.showtimeSeat.UpdateShowtimeSeatRequest;
import com.api.moviebooking.services.ShowtimeSeatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/showtime-seats")
@RequiredArgsConstructor
@Tag(name = "Showtime Seat Operations")
public class ShowtimeSeatController {

    private final ShowtimeSeatService showtimeSeatService;

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update showtime seat (Admin only)", 
               description = "Update seat status or price manually")
    public ResponseEntity<ShowtimeSeatDataResponse> updateShowtimeSeat(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShowtimeSeatRequest request) {
        ShowtimeSeatDataResponse response = showtimeSeatService.updateShowtimeSeat(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Reset showtime seat status to AVAILABLE (Admin only)", 
               description = "Resets seat status to AVAILABLE instead of deleting it to maintain data integrity")
    public ResponseEntity<ShowtimeSeatDataResponse> resetShowtimeSeatStatus(@PathVariable UUID id) {
        ShowtimeSeatDataResponse response = showtimeSeatService.resetShowtimeSeatStatus(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get showtime seat by ID")
    public ResponseEntity<ShowtimeSeatDataResponse> getShowtimeSeat(@PathVariable UUID id) {
        ShowtimeSeatDataResponse response = showtimeSeatService.getShowtimeSeat(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/showtime/{showtimeId}")
    @Operation(summary = "Get all seats for a showtime")
    public ResponseEntity<List<ShowtimeSeatDataResponse>> getShowtimeSeatsByShowtime(@PathVariable UUID showtimeId) {
        List<ShowtimeSeatDataResponse> response = showtimeSeatService.getShowtimeSeatsByShowtime(showtimeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/showtime/{showtimeId}/available")
    @Operation(summary = "Get available seats for a showtime")
    public ResponseEntity<List<ShowtimeSeatDataResponse>> getAvailableShowtimeSeats(@PathVariable UUID showtimeId) {
        List<ShowtimeSeatDataResponse> response = showtimeSeatService.getAvailableShowtimeSeats(showtimeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/showtime/{showtimeId}/recalculate-prices")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Recalculate prices for all seats in a showtime (Admin only)", 
               description = "Useful after updating price modifiers to apply new pricing rules")
    public ResponseEntity<List<ShowtimeSeatDataResponse>> recalculatePrices(@PathVariable UUID showtimeId) {
        List<ShowtimeSeatDataResponse> response = showtimeSeatService.recalculatePrices(showtimeId);
        return ResponseEntity.ok(response);
    }
}
