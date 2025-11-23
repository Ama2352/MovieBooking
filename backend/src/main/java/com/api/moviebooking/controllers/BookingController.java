package com.api.moviebooking.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.helpers.utils.SessionHelper;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.models.dtos.booking.BookingResponse;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.services.CheckoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
}
