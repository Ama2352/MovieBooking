package com.api.moviebooking.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.moviebooking.models.dtos.showtimetickettype.AssignTicketTypesRequest;
import com.api.moviebooking.models.dtos.showtimetickettype.ShowtimeTicketTypesResponse;
import com.api.moviebooking.services.ShowtimeTicketTypeService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/showtimes/{showtimeId}/ticket-types")
@RequiredArgsConstructor
@Tag(name = "Showtime Ticket Type Operations")
public class ShowtimeTicketTypeController {

    private final ShowtimeTicketTypeService showtimeTicketTypeService;

    /**
     * Get all ticket types assigned to a showtime
     * GET /showtimes/{showtimeId}/ticket-types
     */
    @GetMapping
    public ResponseEntity<ShowtimeTicketTypesResponse> getAssignedTicketTypes(@PathVariable UUID showtimeId) {
        List<UUID> ticketTypeIds = showtimeTicketTypeService.getAssignedTicketTypeIds(showtimeId);
        ShowtimeTicketTypesResponse response = new ShowtimeTicketTypesResponse(showtimeId, ticketTypeIds);
        return ResponseEntity.ok(response);
    }

    /**
     * Assign a single ticket type to a showtime
     * POST /showtimes/{showtimeId}/ticket-types/{ticketTypeId}
     */
    @PostMapping("/{ticketTypeId}")
    public ResponseEntity<Void> assignTicketType(
            @PathVariable UUID showtimeId,
            @PathVariable UUID ticketTypeId) {
        showtimeTicketTypeService.assignTicketTypeToShowtime(showtimeId, ticketTypeId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Assign multiple ticket types to a showtime
     * POST /showtimes/{showtimeId}/ticket-types
     */
    @PostMapping
    public ResponseEntity<Void> assignTicketTypes(
            @PathVariable UUID showtimeId,
            @Valid @RequestBody AssignTicketTypesRequest request) {
        showtimeTicketTypeService.assignTicketTypesToShowtime(showtimeId, request.getTicketTypeIds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Replace all ticket types for a showtime
     * PUT /showtimes/{showtimeId}/ticket-types
     */
    @PutMapping
    public ResponseEntity<Void> replaceTicketTypes(
            @PathVariable UUID showtimeId,
            @Valid @RequestBody AssignTicketTypesRequest request) {
        showtimeTicketTypeService.replaceTicketTypesForShowtime(showtimeId, request.getTicketTypeIds());
        return ResponseEntity.ok().build();
    }

    /**
     * Remove a ticket type from a showtime
     * DELETE /showtimes/{showtimeId}/ticket-types/{ticketTypeId}
     */
    @DeleteMapping("/{ticketTypeId}")
    public ResponseEntity<Void> removeTicketType(
            @PathVariable UUID showtimeId,
            @PathVariable UUID ticketTypeId) {
        showtimeTicketTypeService.removeTicketTypeFromShowtime(showtimeId, ticketTypeId);
        return ResponseEntity.noContent().build();
    }
}
