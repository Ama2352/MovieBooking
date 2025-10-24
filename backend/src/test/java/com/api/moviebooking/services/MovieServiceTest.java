package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.moviebooking.helpers.mapstructs.MovieMapper;
import com.api.moviebooking.models.dtos.movie.AddMovieRequest;
import com.api.moviebooking.models.dtos.movie.MovieDataResponse;
import com.api.moviebooking.models.dtos.movie.UpdateMovieRequest;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.repositories.MovieRepo;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepo movieRepo;

    @Mock
    private MovieMapper movieMapper;

    @InjectMocks
    private MovieService movieService;

    @Test
    void addMovie_mapsSavesAndReturnsResponse() {
        AddMovieRequest req = AddMovieRequest.builder()
                .title("Inception")
                .genre("Sci-Fi")
                .description("A mind-bending thriller")
                .duration(148)
                .minimumAge(13)
                .director("Christopher Nolan")
                .cast("Leonardo DiCaprio, Tom Hardy")
                .posterUrl("http://example.com/poster.jpg")
                .trailerUrl("http://example.com/trailer.mp4")
                .status("SHOWING")
                .language("English")
                .build();

        Movie entity = new Movie();
        entity.setTitle("Inception");
        entity.setGenre("Sci-Fi");
        entity.setDescription("A mind-bending thriller");
        entity.setDuration(148);

        MovieDataResponse expected = MovieDataResponse.builder()
                .title("Inception")
                .genre("Sci-Fi")
                .description("A mind-bending thriller")
                .duration(148)
                .build();

        when(movieRepo.existsByTitleIgnoreCase(req.getTitle())).thenReturn(false);
        when(movieMapper.toEntity(req)).thenReturn(entity);
        when(movieMapper.toDataResponse(entity)).thenReturn(expected);

        MovieDataResponse result = movieService.addMovie(req);

        verify(movieRepo).save(entity);
        assertSame(expected, result);
    }

    @Test
    void addMovie_throwsWhenTitleAlreadyExists() {
        AddMovieRequest req = AddMovieRequest.builder()
                .title("Inception")
                .genre("Sci-Fi")
                .build();

        when(movieRepo.existsByTitleIgnoreCase(req.getTitle())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> movieService.addMovie(req));
    }

    @Test
    void updateMovie_updatesNonNullFieldsAndSaves() {
        UUID id = UUID.randomUUID();
        Movie existing = new Movie();
        existing.setId(id);
        existing.setTitle("Old Title");
        existing.setGenre("Old Genre");
        existing.setDescription("Old Description");
        existing.setDuration(120);
        existing.setStatus(MovieStatus.SHOWING);

        UpdateMovieRequest req = UpdateMovieRequest.builder()
                .title("New Title")
                .genre("New Genre")
                .description(null)
                .duration(150)
                .status("UPCOMING")
                .build();

        when(movieRepo.findById(id)).thenReturn(Optional.of(existing));
        when(movieRepo.existsByTitleIgnoreCase("New Title")).thenReturn(false);

        MovieDataResponse mapped = MovieDataResponse.builder()
                .title("New Title")
                .genre("New Genre")
                .description("Old Description")
                .duration(150)
                .build();
        when(movieMapper.toDataResponse(existing)).thenReturn(mapped);

        MovieDataResponse result = movieService.updateMovie(id, req);

        verify(movieRepo).save(existing);
        assertEquals("New Title", existing.getTitle());
        assertEquals("New Genre", existing.getGenre());
        assertEquals("Old Description", existing.getDescription());
        assertEquals(150, existing.getDuration());
        assertEquals(MovieStatus.UPCOMING, existing.getStatus());

        assertSame(mapped, result);
    }

    @Test
    void updateMovie_throwsWhenNewTitleConflicts() {
        UUID id = UUID.randomUUID();
        Movie existing = new Movie();
        existing.setId(id);
        existing.setTitle("Old Title");

        UpdateMovieRequest req = UpdateMovieRequest.builder()
                .title("Conflicting Title")
                .build();

        when(movieRepo.findById(id)).thenReturn(Optional.of(existing));
        when(movieRepo.existsByTitleIgnoreCase("Conflicting Title")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> movieService.updateMovie(id, req));
    }

    @Test
    void deleteMovie_findsAndDeletes() {
        UUID id = UUID.randomUUID();
        Movie existing = new Movie();
        existing.setId(id);
        existing.setShowtimes(new ArrayList<>());

        when(movieRepo.findById(id)).thenReturn(Optional.of(existing));

        movieService.deleteMovie(id);

        verify(movieRepo).delete(existing);
    }

    @Test
    void deleteMovie_throwsWhenHasShowtimes() {
        UUID id = UUID.randomUUID();
        Movie existing = new Movie();
        existing.setId(id);
        existing.setShowtimes(List.of(new com.api.moviebooking.models.entities.Showtime()));

        when(movieRepo.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> movieService.deleteMovie(id));
    }

    @Test
    void getMovie_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        Movie existing = new Movie();
        existing.setId(id);
        existing.setTitle("Inception");
        existing.setGenre("Sci-Fi");

        when(movieRepo.findById(id)).thenReturn(Optional.of(existing));

        MovieDataResponse mapped = MovieDataResponse.builder()
                .title("Inception")
                .genre("Sci-Fi")
                .build();
        when(movieMapper.toDataResponse(existing)).thenReturn(mapped);

        MovieDataResponse result = movieService.getMovie(id);
        assertSame(mapped, result);
    }

    @Test
    void getAllMovies_returnsMappedList() {
        Movie movie1 = new Movie();
        movie1.setTitle("Movie 1");
        Movie movie2 = new Movie();
        movie2.setTitle("Movie 2");

        when(movieRepo.findAll()).thenReturn(List.of(movie1, movie2));

        MovieDataResponse resp1 = MovieDataResponse.builder()
                .title("Movie 1")
                .build();
        MovieDataResponse resp2 = MovieDataResponse.builder()
                .title("Movie 2")
                .build();

        when(movieMapper.toDataResponse(movie1)).thenReturn(resp1);
        when(movieMapper.toDataResponse(movie2)).thenReturn(resp2);

        List<MovieDataResponse> result = movieService.getAllMovies();

        assertEquals(2, result.size());
        assertEquals("Movie 1", result.get(0).getTitle());
        assertEquals("Movie 2", result.get(1).getTitle());
    }

    @Test
    void searchMoviesByTitle_returnsMappedList() {
        Movie movie = new Movie();
        movie.setTitle("Inception");

        when(movieRepo.findByTitleContainingIgnoreCase("Incep")).thenReturn(List.of(movie));

        MovieDataResponse resp = MovieDataResponse.builder()
                .title("Inception")
                .build();
        when(movieMapper.toDataResponse(movie)).thenReturn(resp);

        List<MovieDataResponse> result = movieService.searchMoviesByTitle("Incep");

        assertEquals(1, result.size());
        assertEquals("Inception", result.get(0).getTitle());
    }

    @Test
    void getMoviesByStatus_returnsMappedList() {
        Movie movie = new Movie();
        movie.setTitle("Showing Movie");
        movie.setStatus(MovieStatus.SHOWING);

        when(movieRepo.findByStatus(MovieStatus.SHOWING)).thenReturn(List.of(movie));

        MovieDataResponse resp = MovieDataResponse.builder()
                .title("Showing Movie")
                .build();
        when(movieMapper.toDataResponse(movie)).thenReturn(resp);

        List<MovieDataResponse> result = movieService.getMoviesByStatus("SHOWING");

        assertEquals(1, result.size());
        assertEquals("Showing Movie", result.get(0).getTitle());
    }

    @Test
    void getMoviesByGenre_returnsMappedList() {
        Movie movie = new Movie();
        movie.setTitle("Action Movie");
        movie.setGenre("Action");

        when(movieRepo.findByGenreContainingIgnoreCase("Action")).thenReturn(List.of(movie));

        MovieDataResponse resp = MovieDataResponse.builder()
                .title("Action Movie")
                .build();
        when(movieMapper.toDataResponse(movie)).thenReturn(resp);

        List<MovieDataResponse> result = movieService.getMoviesByGenre("Action");

        assertEquals(1, result.size());
        assertEquals("Action Movie", result.get(0).getTitle());
    }

    @Test
    void searchMovies_returnsMappedList() {
        Movie movie = new Movie();
        movie.setTitle("Advanced Search Movie");
        movie.setGenre("Sci-Fi");
        movie.setStatus(MovieStatus.SHOWING);

        when(movieRepo.searchMovies("Advanced", "Sci-Fi", MovieStatus.SHOWING))
                .thenReturn(List.of(movie));

        MovieDataResponse resp = MovieDataResponse.builder()
                .title("Advanced Search Movie")
                .build();
        when(movieMapper.toDataResponse(movie)).thenReturn(resp);

        List<MovieDataResponse> result = movieService.searchMovies("Advanced", "Sci-Fi", "SHOWING");

        assertEquals(1, result.size());
        assertEquals("Advanced Search Movie", result.get(0).getTitle());
    }

    @Test
    void operations_throwWhenMovieNotFound() {
        UUID id = UUID.randomUUID();
        when(movieRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> movieService.getMovie(id));
        assertThrows(RuntimeException.class,
                () -> movieService.updateMovie(id, UpdateMovieRequest.builder().build()));
        assertThrows(RuntimeException.class, () -> movieService.deleteMovie(id));
    }
}
