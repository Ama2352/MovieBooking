package com.api.moviebooking.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.api.moviebooking.models.dtos.showtime.AddShowtimeRequest;
import com.api.moviebooking.models.dtos.showtime.ShowtimeDataResponse;
import com.api.moviebooking.models.dtos.showtime.UpdateShowtimeRequest;
import com.api.moviebooking.services.ShowtimeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/showtimes")
@Tag(name = "Showtime Operations")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Add a new showtime (Admin only)")
    public ResponseEntity<ShowtimeDataResponse> addShowtime(@Valid @RequestBody AddShowtimeRequest request) {
        ShowtimeDataResponse response = showtimeService.addShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{showtimeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update showtime details (Admin only)")
    public ResponseEntity<ShowtimeDataResponse> updateShowtime(
            @PathVariable UUID showtimeId,
            @Valid @RequestBody UpdateShowtimeRequest request) {
        ShowtimeDataResponse response = showtimeService.updateShowtime(showtimeId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{showtimeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete a showtime (Admin only)")
    public ResponseEntity<Void> deleteShowtime(@PathVariable UUID showtimeId) {
        showtimeService.deleteShowtime(showtimeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{showtimeId}")
    @Operation(summary = "Get showtime details by ID")
    public ResponseEntity<ShowtimeDataResponse> getShowtimeById(@PathVariable UUID showtimeId) {
        ShowtimeDataResponse response = showtimeService.getShowtime(showtimeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all showtimes")
    public ResponseEntity<List<ShowtimeDataResponse>> getAllShowtimes() {
        List<ShowtimeDataResponse> showtimes = showtimeService.getAllShowtimes();
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get all showtimes for a specific movie")
    public ResponseEntity<List<ShowtimeDataResponse>> getShowtimesByMovie(@PathVariable UUID movieId) {
        List<ShowtimeDataResponse> showtimes = showtimeService.getShowtimesByMovie(movieId);
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/movie/{movieId}/upcoming")
    @Operation(summary = "Get upcoming showtimes for a specific movie")
    public ResponseEntity<List<ShowtimeDataResponse>> getUpcomingShowtimesByMovie(@PathVariable UUID movieId) {
        List<ShowtimeDataResponse> showtimes = showtimeService.getUpcomingShowtimesByMovie(movieId);
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get all showtimes for a specific room")
    public ResponseEntity<List<ShowtimeDataResponse>> getShowtimesByRoom(@PathVariable UUID roomId) {
        List<ShowtimeDataResponse> showtimes = showtimeService.getShowtimesByRoom(roomId);
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/movie/{movieId}/date-range")
    @Operation(summary = "Get showtimes for a movie within a date range")
    public ResponseEntity<List<ShowtimeDataResponse>> getShowtimesByMovieAndDateRange(
            @PathVariable UUID movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<ShowtimeDataResponse> showtimes = showtimeService.getShowtimesByMovieAndDateRange(movieId, startDate, endDate);
        return ResponseEntity.ok(showtimes);
    }
}
