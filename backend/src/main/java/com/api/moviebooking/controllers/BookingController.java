package com.api.moviebooking.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.helpers.utils.SessionHelper;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.booking.UpdateQrCodeRequest;
import com.api.moviebooking.services.BookingService;
import com.api.moviebooking.services.CheckoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Operations")
public class BookingController {

    private final CheckoutService checkoutService;
    private final BookingService bookingService;
    private final SessionHelper sessionHelper;

    @PostMapping("/confirm")
    @Operation(summary = "Confirm booking with guest support", description = """
            Validates seat locks and creates a booking. For guests, creates User account automatically.
            Authenticated users provide JWT; guests provide X-Session-Id header and guestInfo.
            """, parameters = {
            @Parameter(name = "X-Session-Id", description = "Guest session ID (required for guests, ignored if JWT present)", example = "550e8400-e29b-41d4-a716-446655440000", required = false, schema = @Schema(type = "string", format = "uuid"))
    })
    public ResponseEntity<BookingResponse> confirmBooking(
            @Valid @RequestBody ConfirmBookingRequest request,
            HttpServletRequest httpRequest) {

        // Extract session context
        SessionContext session = sessionHelper.extractSessionContext(httpRequest);

        // Validate guest info if guest session
        if (session.isGuest() && request.getGuestInfo() == null) {
            throw new IllegalArgumentException("Guest information required for guest booking");
        }

        // Confirm booking (creates User for guests)
        BookingResponse response = checkoutService.confirmBooking(request, session);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Get user's bookings", description = "Retrieve all bookings for the authenticated user")
    public ResponseEntity<List<BookingResponse>> getUserBookings(HttpServletRequest httpRequest) {
        SessionContext session = sessionHelper.extractSessionContext(httpRequest);

        if (!session.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required to view bookings");
        }

        List<BookingResponse> bookings = bookingService.getUserBookings(session.getUserId());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Get booking by ID", description = "Retrieve a specific booking by ID (user can only access their own bookings)")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable UUID bookingId,
            HttpServletRequest httpRequest) {
        SessionContext session = sessionHelper.extractSessionContext(httpRequest);

        if (!session.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required to view booking");
        }

        BookingResponse booking = bookingService.getBookingByIdForUser(bookingId, session.getUserId());
        return ResponseEntity.ok(booking);
    }

    @PatchMapping("/{bookingId}/qr")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update QR code URL", description = "Update the QR code URL for a booking (user can only update their own bookings)")
    public ResponseEntity<BookingResponse> updateQrCode(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateQrCodeRequest request,
            HttpServletRequest httpRequest) {
        SessionContext session = sessionHelper.extractSessionContext(httpRequest);

        if (!session.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication required to update booking");
        }

        BookingResponse booking = bookingService.updateQrCode(bookingId, session.getUserId(), request.getQrCodeUrl());
        return ResponseEntity.ok(booking);
    }
}
