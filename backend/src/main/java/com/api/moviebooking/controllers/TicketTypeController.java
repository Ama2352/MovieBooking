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

import com.api.moviebooking.models.dtos.ticketType.CreateTicketTypeRequest;
import com.api.moviebooking.models.dtos.ticketType.TicketTypeResponse;
import com.api.moviebooking.models.dtos.ticketType.UpdateTicketTypeRequest;
import com.api.moviebooking.services.TicketTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ticket-types")
@Tag(name = "Ticket Type Operations")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    /**
     * GET /ticket-types
     * Get all active ticket types with base prices (for guest)
     */
    @GetMapping
    @Operation(summary = "Get all active ticket types", 
               description = "Returns all active ticket types with their base prices. Used for UI to display ticket type selection.")
    public ResponseEntity<List<TicketTypeResponse>> getAllActiveTicketTypes(
            @RequestParam(required = false) UUID showtimeId,
            @RequestParam(required = false) UUID userId) {
        
        // If showtimeId is provided, calculate prices for that specific showtime
        if (showtimeId != null) {
            List<TicketTypeResponse> response = ticketTypeService.getTicketTypesForShowtime(showtimeId, userId);
            return ResponseEntity.ok(response);
        }
        
        // Otherwise, return base prices only
        List<TicketTypeResponse> response = ticketTypeService.getAllActiveTicketTypes();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/ticket-types
     * Get all ticket types including inactive (for admin)
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Get all ticket types for admin", 
               description = "Returns all ticket types including inactive ones. For admin management.")
    public ResponseEntity<List<TicketTypeResponse>> getAllTicketTypesForAdmin() {
        List<TicketTypeResponse> response = ticketTypeService.getAllTicketTypesForAdmin();
        return ResponseEntity.ok(response);
    }

    /**
     * POST /admin/ticket-types
     * Create a new ticket type (admin only)
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Create a new ticket type (Admin only)", 
               description = "Create a new ticket type with specified price base and sort order.")
    public ResponseEntity<TicketTypeResponse> createTicketType(@Valid @RequestBody CreateTicketTypeRequest request) {
        TicketTypeResponse response = ticketTypeService.createTicketType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /admin/ticket-types/{id}
     * Update an existing ticket type (admin only)
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update a ticket type (Admin only)", 
               description = "Update ticket type label, price base, active status, or sort order.")
    public ResponseEntity<TicketTypeResponse> updateTicketType(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketTypeRequest request) {
        TicketTypeResponse response = ticketTypeService.updateTicketType(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /admin/ticket-types/{id}
     * Delete a ticket type (admin only)
     * Soft delete if used, hard delete if not used
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete a ticket type (Admin only)", 
               description = "Soft delete (set active=false) if ticket type is used in bookings, hard delete otherwise.")
    public ResponseEntity<Void> deleteTicketType(@PathVariable UUID id) {
        ticketTypeService.deleteTicketType(id);
        return ResponseEntity.ok().build();
    }
}
