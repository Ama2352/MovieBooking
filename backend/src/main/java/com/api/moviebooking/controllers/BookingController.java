package com.api.moviebooking.controllers;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.booking.UpdateQrCodeRequest;
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
@SecurityRequirement(name = "bearerToken")
@Tag(name = "Booking Operations")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    /**
     * Confirm booking (Step 2: After lock, before payment) (Allow Guest)
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm booking", description = "Confirms the booking and creates a booking record. Transitions seats from LOCKED to BOOKED. Optionally applies promotion code for discount.")
    public ResponseEntity<BookingResponse> confirmBooking(@Valid @RequestBody ConfirmBookingRequest request) {
        BookingResponse response = bookingService.confirmBooking(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's booking history
     */
    @GetMapping("/my-bookings")
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
    @Operation(summary = "Get booking details", description = "Returns details of a specific booking.")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID bookingId,
            Principal principal) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        BookingResponse response = bookingService.getBookingForUser(bookingId, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{bookingId}/qr")
    @Operation(summary = "Attach Cloudinary QR code URL", description = "Accepts FE-provided Cloudinary URL after QR generation")
    public ResponseEntity<BookingResponse> updateQrCode(
            @PathVariable UUID bookingId,
            Principal principal,
            @Valid @RequestBody UpdateQrCodeRequest request) {
        UUID userId = userService.getUserIdFromPrincipal(principal);
        BookingResponse response = bookingService.updateQrCode(bookingId, userId, request.getQrCodeUrl());
        return ResponseEntity.ok(response);
    }
}
