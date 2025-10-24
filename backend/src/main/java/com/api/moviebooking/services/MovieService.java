package com.api.moviebooking.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.mapstructs.MovieMapper;
import com.api.moviebooking.models.dtos.movie.AddMovieRequest;
import com.api.moviebooking.models.dtos.movie.MovieDataResponse;
import com.api.moviebooking.models.dtos.movie.UpdateMovieRequest;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.repositories.MovieRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepo movieRepo;
    private final MovieMapper movieMapper;

    private Movie findMovieById(UUID movieId) {
        return movieRepo.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
    }

    @Transactional
    public MovieDataResponse addMovie(AddMovieRequest request) {
        // Validate no duplicate title
        if (movieRepo.existsByTitleIgnoreCase(request.getTitle())) {
            throw new IllegalArgumentException("Movie with this title already exists");
        }

        Movie newMovie = movieMapper.toEntity(request);
        movieRepo.save(newMovie);
        return movieMapper.toDataResponse(newMovie);
    }

    @Transactional
    public MovieDataResponse updateMovie(UUID movieId, UpdateMovieRequest request) {
        Movie movie = findMovieById(movieId);

        if (request.getTitle() != null) {
            // Check if new title conflicts with another movie
            if (!request.getTitle().equalsIgnoreCase(movie.getTitle()) && 
                movieRepo.existsByTitleIgnoreCase(request.getTitle())) {
                throw new IllegalArgumentException("Another movie with this title already exists");
            }
            movie.setTitle(request.getTitle());
        }
        if (request.getGenre() != null) {
            movie.setGenre(request.getGenre());
        }
        if (request.getDescription() != null) {
            movie.setDescription(request.getDescription());
        }
        if (request.getDuration() != null) {
            movie.setDuration(request.getDuration());
        }
        if (request.getMinimumAge() != null) {
            movie.setMinimumAge(request.getMinimumAge());
        }
        if (request.getDirector() != null) {
            movie.setDirector(request.getDirector());
        }
        if (request.getCast() != null) {
            movie.setCast(request.getCast());
        }
        if (request.getPosterUrl() != null) {
            movie.setPosterUrl(request.getPosterUrl());
        }
        if (request.getPosterCloudinaryId() != null) {
            movie.setPosterCloudinaryId(request.getPosterCloudinaryId());
        }
        if (request.getTrailerUrl() != null) {
            movie.setTrailerUrl(request.getTrailerUrl());
        }
        if (request.getStatus() != null) {
            movie.setStatus(MovieStatus.valueOf(request.getStatus()));
        }
        if (request.getLanguage() != null) {
            movie.setLanguage(request.getLanguage());
        }

        movieRepo.save(movie);
        return movieMapper.toDataResponse(movie);
    }

    @Transactional
    public void deleteMovie(UUID movieId) {
        Movie movie = findMovieById(movieId);
        
        // Check if movie has associated showtimes
        if (!movie.getShowtimes().isEmpty()) {
            throw new IllegalStateException("Cannot delete movie with existing showtimes");
        }

        movieRepo.delete(movie);
    }

    public MovieDataResponse getMovie(UUID movieId) {
        Movie movie = findMovieById(movieId);
        return movieMapper.toDataResponse(movie);
    }

    public List<MovieDataResponse> getAllMovies() {
        return movieRepo.findAll().stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    // Search movies by title
    public List<MovieDataResponse> searchMoviesByTitle(String title) {
        return movieRepo.findByTitleContainingIgnoreCase(title).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    // Filter movies by status
    public List<MovieDataResponse> getMoviesByStatus(String status) {
        MovieStatus movieStatus = MovieStatus.valueOf(status);
        return movieRepo.findByStatus(movieStatus).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    // Filter movies by genre
    public List<MovieDataResponse> getMoviesByGenre(String genre) {
        return movieRepo.findByGenreContainingIgnoreCase(genre).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    // Advanced search with multiple filters
    public List<MovieDataResponse> searchMovies(String title, String genre, String status) {
        MovieStatus movieStatus = (status != null && !status.isEmpty()) ? MovieStatus.valueOf(status) : null;
        return movieRepo.searchMovies(title, genre, movieStatus).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }
}
