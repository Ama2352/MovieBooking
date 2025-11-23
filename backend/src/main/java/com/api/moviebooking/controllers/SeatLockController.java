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
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.helpers.utils.SessionHelper;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsResponse;
import com.api.moviebooking.models.dtos.booking.SeatAvailabilityResponse;
import com.api.moviebooking.services.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seat-locks")
@RequiredArgsConstructor
@Tag(name = "Seat Lock Operations")
public class SeatLockController {

        private final BookingService bookingService;
        private final SessionHelper sessionHelper;

        @PostMapping
        @Operation(summary = "Lock seats with session support", description = """
                        Locks seats for 10 minutes. Authenticated users use JWT; guests use X-Session-Id header.
                        One active lock per session+showtime. See API-Bookings.md for details.
                        """, parameters = {
                        @Parameter(name = "X-Session-Id", description = "Guest session ID (UUID format). Required for guests, ignored if JWT present.", example = "550e8400-e29b-41d4-a716-446655440000", required = false, schema = @Schema(type = "string", format = "uuid"))
        })
        public ResponseEntity<LockSeatsResponse> lockSeats(
                        @Valid @RequestBody LockSeatsRequest request,
                        HttpServletRequest httpRequest) {

                // Extract session context (userId from JWT or sessionId from header)
                SessionContext session = sessionHelper.extractSessionContext(httpRequest);

                // Delegate to service
                LockSeatsResponse response = bookingService.lockSeats(request, session);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .header("X-Lock-Owner-Type", session.getLockOwnerType().name())
                                .body(response);
        }

        @DeleteMapping("/showtime/{showtimeId}")
        @Operation(summary = "Release locked seats", description = """
                        Manually releases seats before confirmation. Only lock owner can release.
                        Idempotent - returns 200 even if no locks found.
                        """, parameters = {
                        @Parameter(name = "X-Session-Id", description = "Guest session ID. Required for guests, ignored if JWT present.", required = false, schema = @Schema(type = "string", format = "uuid"))
        })
        public ResponseEntity<Void> releaseSeats(
                        @PathVariable UUID showtimeId,
                        HttpServletRequest httpRequest) {

                SessionContext session = sessionHelper.extractSessionContext(httpRequest);
                bookingService.releaseSeats(session.getLockOwnerId(), showtimeId);
                return ResponseEntity.ok().build();
        }

        @GetMapping("/availability/showtime/{showtimeId}")
        @Operation(summary = "Check seat availability", description = """
                        READ-ONLY: Returns available, locked, and booked seats. Optional session context.
                        Does not release locks or perform writes. Safe to call multiple times.
                        """, parameters = {
                        @Parameter(name = "X-Session-Id", description = "Optional: Guest session ID to check your own locks", required = false, schema = @Schema(type = "string", format = "uuid"))
        })
        public ResponseEntity<SeatAvailabilityResponse> checkAvailability(
                        @PathVariable UUID showtimeId,
                        HttpServletRequest httpRequest) {

                // Session is optional for this endpoint (public seat map viewing)
                SessionContext session = sessionHelper.extractSessionContextOptional(httpRequest);

                SeatAvailabilityResponse response = bookingService.checkAvailability(showtimeId, session);
                return ResponseEntity.ok(response);
        }
}
