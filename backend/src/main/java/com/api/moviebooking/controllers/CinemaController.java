package com.api.moviebooking.controllers;

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
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.cinema.AddCinemaRequest;
import com.api.moviebooking.models.dtos.cinema.CinemaDataResponse;
import com.api.moviebooking.models.dtos.cinema.UpdateCinemaRequest;
import com.api.moviebooking.models.dtos.room.AddRoomRequest;
import com.api.moviebooking.models.dtos.room.RoomDataResponse;
import com.api.moviebooking.models.dtos.room.UpdateRoomRequest;
import com.api.moviebooking.models.dtos.snack.AddSnackRequest;
import com.api.moviebooking.models.dtos.snack.SnackDataResponse;
import com.api.moviebooking.models.dtos.snack.UpdateSnackRequest;
import com.api.moviebooking.services.CinemaService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cinemas")
@SecurityRequirement(name = "bearerToken")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Cinema Operations")
public class CinemaController {

    private final CinemaService cinemaService;

    // Cinema CRUD endpoints
    @PostMapping
    public ResponseEntity<CinemaDataResponse> addCinema(@Valid @RequestBody AddCinemaRequest request) {
        CinemaDataResponse response = cinemaService.addCinema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{cinemaId}")
    public ResponseEntity<CinemaDataResponse> updateCinema(
            @PathVariable UUID cinemaId,
            @Valid @RequestBody UpdateCinemaRequest request) {
        CinemaDataResponse response = cinemaService.updateCinema(cinemaId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cinemaId}")
    public ResponseEntity<Void> deleteCinema(@PathVariable UUID cinemaId) {
        cinemaService.deleteCinema(cinemaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{cinemaId}")
    public ResponseEntity<CinemaDataResponse> getCinema(@PathVariable UUID cinemaId) {
        CinemaDataResponse response = cinemaService.getCinema(cinemaId);
        return ResponseEntity.ok(response);
    }

    // Room CRUD endpoints
    @PostMapping("/rooms")
    public ResponseEntity<RoomDataResponse> addRoom(@Valid @RequestBody AddRoomRequest request) {
        RoomDataResponse response = cinemaService.addRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<RoomDataResponse> updateRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomRequest request) {
        RoomDataResponse response = cinemaService.updateRoom(roomId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId) {
        cinemaService.deleteRoom(roomId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<RoomDataResponse> getRoom(@PathVariable UUID roomId) {
        RoomDataResponse response = cinemaService.getRoom(roomId);
        return ResponseEntity.ok(response);
    }

    // Snack CRUD endpoints
    @PostMapping("/snacks")
    public ResponseEntity<SnackDataResponse> addSnack(@Valid @RequestBody AddSnackRequest request) {
        SnackDataResponse response = cinemaService.addSnack(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/snacks/{snackId}")
    public ResponseEntity<SnackDataResponse> updateSnack(
            @PathVariable UUID snackId,
            @Valid @RequestBody UpdateSnackRequest request) {
        SnackDataResponse response = cinemaService.updateSnack(snackId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/snacks/{snackId}")
    public ResponseEntity<Void> deleteSnack(@PathVariable UUID snackId) {
        cinemaService.deleteSnack(snackId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/snacks/{snackId}")
    public ResponseEntity<SnackDataResponse> getSnack(@PathVariable UUID snackId) {
        SnackDataResponse response = cinemaService.getSnack(snackId);
        return ResponseEntity.ok(response);
    }
}
