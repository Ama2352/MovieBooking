package com.api.moviebooking.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.services.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings/lock")
@RequiredArgsConstructor
@Tag(name = "Seat Lock Operations")
public class SeatLockController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Lock seats for booking", description = "Locks selected seats for 10 minutes. User must confirm booking before timeout.")
    public ResponseEntity<LockSeatsResponse> lockSeats(@Valid @RequestBody LockSeatsRequest request) {
        LockSeatsResponse response = bookingService.lockSeats(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/release")
    @Operation(summary = "Release locked seats", description = "Manually releases locked seats before confirmation.")
    public ResponseEntity<Void> releaseSeats(
            @RequestParam UUID showtimeId,
            @RequestParam UUID userId) {
        bookingService.releaseSeats(userId, showtimeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/back")
    @Operation(summary = "Handle back button", description = "Releases all locked seats immediately when user presses back. Seats become available to everyone.")
    public ResponseEntity<Void> handleBackButton(
            @RequestParam UUID showtimeId,
            @RequestParam UUID userId) {
        bookingService.handleBackButton(userId, showtimeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/availability/{showtimeId}")
    @Operation(summary = "Check seat availability", description = "Returns available, locked, and booked seats for a showtime. Releases any existing locks for the user.")
    public ResponseEntity<SeatAvailabilityResponse> checkAvailability(
            @PathVariable UUID showtimeId,
            @RequestParam UUID userId) {
        SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, userId);
        return ResponseEntity.ok(response);
    }
}
