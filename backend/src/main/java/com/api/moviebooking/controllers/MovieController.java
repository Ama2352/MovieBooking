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

import com.api.moviebooking.models.dtos.movie.AddMovieRequest;
import com.api.moviebooking.models.dtos.movie.MovieDataResponse;
import com.api.moviebooking.models.dtos.movie.UpdateMovieRequest;
import com.api.moviebooking.services.MovieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/movies")
@Tag(name = "Movie Operations")
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Add a new movie (Admin only)")
    public ResponseEntity<MovieDataResponse> addMovie(@Valid @RequestBody AddMovieRequest request) {
        MovieDataResponse response = movieService.addMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{movieId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Update movie details (Admin only)")
    public ResponseEntity<MovieDataResponse> updateMovie(
            @PathVariable UUID movieId,
            @Valid @RequestBody UpdateMovieRequest request) {
        MovieDataResponse response = movieService.updateMovie(movieId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{movieId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerToken")
    @Operation(summary = "Delete a movie (Admin only)")
    public ResponseEntity<Void> deleteMovie(@PathVariable UUID movieId) {
        movieService.deleteMovie(movieId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{movieId}")
    @Operation(summary = "Get movie details by ID")
    public ResponseEntity<MovieDataResponse> getMovieById(@PathVariable UUID movieId) {
        MovieDataResponse response = movieService.getMovie(movieId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all movies or search movies")
    public ResponseEntity<List<MovieDataResponse>> getAllMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status) {
        
        List<MovieDataResponse> movies;
        
        // If any search parameter is provided, use advanced search
        if (title != null || genre != null || status != null) {
            movies = movieService.searchMovies(title, genre, status);
        } else {
            movies = movieService.getAllMovies();
        }
        
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/search/title")
    @Operation(summary = "Search movies by title")
    public ResponseEntity<List<MovieDataResponse>> searchByTitle(@RequestParam String title) {
        List<MovieDataResponse> movies = movieService.searchMoviesByTitle(title);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/filter/status")
    @Operation(summary = "Filter movies by status (SHOWING, UPCOMING)")
    public ResponseEntity<List<MovieDataResponse>> getMoviesByStatus(@RequestParam String status) {
        List<MovieDataResponse> movies = movieService.getMoviesByStatus(status);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/filter/genre")
    @Operation(summary = "Filter movies by genre")
    public ResponseEntity<List<MovieDataResponse>> getMoviesByGenre(@RequestParam String genre) {
        List<MovieDataResponse> movies = movieService.getMoviesByGenre(genre);
        return ResponseEntity.ok(movies);
    }
}
