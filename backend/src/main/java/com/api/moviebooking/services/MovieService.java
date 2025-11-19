package com.api.moviebooking.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.MovieMapper;
import com.api.moviebooking.models.dtos.movie.AddMovieRequest;
import com.api.moviebooking.models.dtos.movie.CinemaShowtimesResponse;
import com.api.moviebooking.models.dtos.movie.MovieDataResponse;
import com.api.moviebooking.models.dtos.movie.UpdateMovieRequest;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.repositories.MovieRepo;
import com.api.moviebooking.repositories.ShowtimeRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepo movieRepo;
    private final ShowtimeRepo showtimeRepo;
    private final MovieMapper movieMapper;

    /**
     * Predicate nodes (d): 0 -> V(G)=d+1=1
     * Nodes: none (just findById with exception)
     * Minimum test cases: 1
     */
    private Movie findMovieById(UUID movieId) {
        return movieRepo.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
    }

    /**
     * Add a new movie (API: POST /movies)
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes: existsByTitleIgnoreCase
     * Minimum test cases: 2
     */
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

    /**
     * Update movie details (API: PUT /movies/{movieId})
     * Predicate nodes (d): 13 -> V(G)=d+1=14
     * Nodes: existsByTitleIgnoreCase, multiple null checks for each field
     * Minimum test cases: 14
     */
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
        if (request.getActors() != null) {
            movie.setActors(request.getActors());
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

    /**
     * Delete a movie (API: DELETE /movies/{movieId})
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes: check for associated showtimes
     * Minimum test cases: 2
     */
    @Transactional
    public void deleteMovie(UUID movieId) {
        Movie movie = findMovieById(movieId);

        // Check if movie has associated showtimes
        if (!movie.getShowtimes().isEmpty()) {
            throw new IllegalStateException("Cannot delete movie with existing showtimes");
        }

        movieRepo.delete(movie);
    }

    /**
     * Get movie details by ID (API: GET /movies/{movieId})
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findMovieById
     */
    public MovieDataResponse getMovie(UUID movieId) {
        Movie movie = findMovieById(movieId);
        return movieMapper.toDataResponse(movie);
    }

    /**
     * Get all movies (API: GET /movies)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<MovieDataResponse> getAllMovies() {
        return movieRepo.findAll().stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search movies by title (API: GET /movies/search/title)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<MovieDataResponse> searchMoviesByTitle(String title) {
        return movieRepo.findByTitleContainingIgnoreCase(title).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Filter movies by status (API: GET /movies/filter/status)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<MovieDataResponse> getMoviesByStatus(String status) {
        MovieStatus movieStatus = MovieStatus.valueOf(status);
        return movieRepo.findByStatus(movieStatus).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Filter movies by genre (API: GET /movies/filter/genre)
     * Predicate nodes (d): 0 -> V(G) = d + 1 = 1
     * Nodes: none
     */
    public List<MovieDataResponse> getMoviesByGenre(String genre) {
        return movieRepo.findByGenreContainingIgnoreCase(genre).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Advanced search with multiple filters (API: GET /movies with query params)
     * Predicate nodes (d): 1 -> V(G)=d+1=2
     * Nodes:
     * - status != null && !status.isEmpty()
     */
    public List<MovieDataResponse> searchMovies(String title, String genre, String status) {
        MovieStatus movieStatus = (status != null && !status.isEmpty()) ? MovieStatus.valueOf(status) : null;
        return movieRepo.searchMovies(title, genre, movieStatus).stream()
                .map(movieMapper::toDataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get showtimes for a movie grouped by cinema on a specific date
     * API: GET /movies/{id}/showtimes?date=YYYY-MM-DD
     * If date is today, only return showtimes from current time onwards
     */
    public List<CinemaShowtimesResponse> getMovieShowtimesByDate(UUID movieId, LocalDate date) {
        Movie movie = findMovieById(movieId);
        
        // Check if querying for today
        LocalDate today = LocalDate.now();
        boolean isToday = date.isEqual(today);
        
        // Define start and end time
        LocalDateTime startOfDay;
        if (isToday) {
            // If today, start from current time
            startOfDay = LocalDateTime.now();
        } else {
            // If other date, start from beginning of day
            startOfDay = date.atStartOfDay();
        }
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        // Get all showtimes for this movie on the specified date
        List<Showtime> showtimes = showtimeRepo.findByMovieAndStartTimeBetween(movie, startOfDay, endOfDay);
        
        // Group showtimes by cinema
        Map<UUID, CinemaShowtimesResponse> cinemaMap = new LinkedHashMap<>();
        
        for (Showtime showtime : showtimes) {
            UUID cinemaId = showtime.getRoom().getCinema().getId();
            
            // Create cinema entry if not exists
            if (!cinemaMap.containsKey(cinemaId)) {
                CinemaShowtimesResponse cinemaResponse = new CinemaShowtimesResponse();
                cinemaResponse.setCinemaId(cinemaId);
                cinemaResponse.setCinemaName(showtime.getRoom().getCinema().getName());
                cinemaResponse.setAddress(showtime.getRoom().getCinema().getAddress());
                cinemaResponse.setShowtimes(new ArrayList<>());
                cinemaMap.put(cinemaId, cinemaResponse);
            }
            
            // Add showtime info to cinema
            CinemaShowtimesResponse.ShowtimeInfo showtimeInfo = new CinemaShowtimesResponse.ShowtimeInfo();
            showtimeInfo.setShowtimeId(showtime.getId());
            showtimeInfo.setStartTime(showtime.getStartTime().toString());
            showtimeInfo.setFormat(showtime.getFormat());
            showtimeInfo.setRoomName("Phòng " + showtime.getRoom().getRoomNumber());
            
            cinemaMap.get(cinemaId).getShowtimes().add(showtimeInfo);
        }
        
        return new ArrayList<>(cinemaMap.values());
    }
}
