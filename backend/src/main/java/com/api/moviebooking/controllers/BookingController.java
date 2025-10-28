package com.api.moviebooking.controllers;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.services.BookingService;
import com.api.moviebooking.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Operations")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    /**
     * Lock seats for booking (Step 1: User proceeds to checkout)
     */
    @PostMapping("/lock")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Lock seats for booking", description = "Locks selected seats for 10 minutes. User must confirm booking before timeout.")
    public ResponseEntity<LockSeatsResponse> lockSeats(
            @Valid @RequestBody LockSeatsRequest request,
            Principal principal) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        LockSeatsResponse response = bookingService.lockSeats(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Confirm booking (Step 2: After lock, before payment)
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Confirm booking", description = "Confirms the booking and creates a booking record. Transitions seats from LOCKED to BOOKED.")
    public ResponseEntity<BookingResponse> confirmBooking(
            @Valid @RequestBody ConfirmBookingRequest request,
            Principal principal) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        BookingResponse response = bookingService.confirmBooking(userId, request.getLockId());
        return ResponseEntity.ok(response);
    }

    /**
     * Release seats (User cancels or navigates away)
     */
    @DeleteMapping("/release")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Release locked seats", description = "Manually releases locked seats before confirmation.")
    public ResponseEntity<Void> releaseSeats(
            @RequestParam UUID showtimeId,
            Principal principal) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        bookingService.releaseSeats(userId, showtimeId);
        return ResponseEntity.ok().build();
    }

    /**
     * Handle back button - Releases all locked seats immediately
     */
    @PostMapping("/back")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Handle back button", description = "Releases all locked seats immediately when user presses back. Seats become available to everyone.")
    public ResponseEntity<Void> handleBackButton(
            @RequestParam UUID showtimeId,
            Principal principal) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        bookingService.handleBackButton(userId, showtimeId);
        return ResponseEntity.ok().build();
    }

    /**
     * Check seat availability for a showtime
     */
    @GetMapping("/availability/{showtimeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Check seat availability", description = "Returns available, locked, and booked seats for a showtime. Releases any existing locks for the user.")
    public ResponseEntity<SeatAvailabilityResponse> checkAvailability(
            @PathVariable UUID showtimeId,
            Principal principal) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's booking history
     */
    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Get user's booking history", description = "Returns all bookings for the authenticated user.")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        List<BookingResponse> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Get specific booking details
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Get booking details", description = "Returns details of a specific booking.")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID bookingId,
            Principal principal) {
        // Implementation would fetch single booking with authorization check
        return ResponseEntity.ok().build();
    }
}
