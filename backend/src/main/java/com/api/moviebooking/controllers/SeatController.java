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

import com.api.moviebooking.models.dtos.seat.AddSeatRequest;
import com.api.moviebooking.models.dtos.seat.BulkSeatResponse;
import com.api.moviebooking.models.dtos.seat.GenerateSeatsRequest;
import com.api.moviebooking.models.dtos.seat.RowLabelsResponse;
import com.api.moviebooking.models.dtos.seat.SeatDataResponse;
import com.api.moviebooking.models.dtos.seat.UpdateSeatRequest;
import com.api.moviebooking.services.SeatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
@Tag(name = "Seat Operations")
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Add a new seat (Admin only)")
    public ResponseEntity<SeatDataResponse> addSeat(@Valid @RequestBody AddSeatRequest request) {
        SeatDataResponse seat = seatService.addSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(seat);
    }

    @PutMapping("/{seatId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update an existing seat (Admin only)")
    public ResponseEntity<SeatDataResponse> updateSeat(
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatRequest request) {
        SeatDataResponse seat = seatService.updateSeat(seatId, request);
        return ResponseEntity.ok(seat);
    }

    @DeleteMapping("/{seatId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete a seat (Admin only)")
    public ResponseEntity<Void> deleteSeat(@PathVariable UUID seatId) {
        seatService.deleteSeat(seatId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{seatId}")
    @Operation(summary = "Get seat by ID")
    public ResponseEntity<SeatDataResponse> getSeat(@PathVariable UUID seatId) {
        SeatDataResponse seat = seatService.getSeat(seatId);
        return ResponseEntity.ok(seat);
    }

    @GetMapping
    @Operation(summary = "Get all seats")
    public ResponseEntity<List<SeatDataResponse>> getAllSeats() {
        List<SeatDataResponse> seats = seatService.getAllSeats();
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get all seats for a specific room")
    public ResponseEntity<List<SeatDataResponse>> getSeatsByRoom(@PathVariable UUID roomId) {
        List<SeatDataResponse> seats = seatService.getSeatsByRoom(roomId);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/row-labels")
    @Operation(summary = "Get row labels preview", 
               description = "Returns row labels (A, B, C, ..., Z, AA, AB, ...) for a given number of rows. Useful for frontend to display row options before generating seats.")
    public ResponseEntity<RowLabelsResponse> getRowLabels(
            @RequestParam(name = "rows", defaultValue = "10") int numberOfRows) {
        RowLabelsResponse response = seatService.getRowLabelsPreview(numberOfRows);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Auto-generate seats for a room based on layout (Admin only)", 
               description = "Automatically generates seats in a grid pattern. Example: 5 rows x 10 seats = A1-A10, B1-B10, ..., E1-E10. You can specify which rows should be VIP or COUPLE type.")
    public ResponseEntity<BulkSeatResponse> generateSeats(@Valid @RequestBody GenerateSeatsRequest request) {
        BulkSeatResponse response = seatService.generateSeats(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
