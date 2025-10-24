package com.api.moviebooking.controllers;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.api.moviebooking.models.dtos.movie.AddMovieRequest;
import com.api.moviebooking.models.dtos.movie.UpdateMovieRequest;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.repositories.MovieRepo;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for MovieController using Testcontainers and RestAssured.
 * Tests Movie CRUD operations with proper security context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
class MovieControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MovieRepo movieRepo;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Base64 encoded secret key (minimum 256 bits for HS256)
        registry.add("jwt.secret",
                () -> "dGVzdHNlY3JldHRlc3RzZWNyZXR0ZXN0c2VjcmV0dGVzdHNlY3JldHRlc3RzZWNyZXR0ZXN0c2VjcmV0");
    }

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build());

        // Clean up test data
        movieRepo.deleteAll();
    }

    // ==================== Movie CRUD Tests ====================

    @Test
    @DisplayName("Should create movie successfully when authenticated as admin")
    @WithMockUser(roles = "ADMIN")
    void testAddMovie_Success() {
        AddMovieRequest request = AddMovieRequest.builder()
                .title("Inception")
                .genre("Sci-Fi, Thriller")
                .description("A mind-bending thriller about dreams within dreams")
                .duration(148)
                .minimumAge(13)
                .director("Christopher Nolan")
                .actors("Leonardo DiCaprio, Tom Hardy, Ellen Page")
                .posterUrl("http://example.com/inception.jpg")
                .trailerUrl("http://example.com/inception-trailer.mp4")
                .status("SHOWING")
                .language("English")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/movies")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("title", equalTo("Inception"))
                .body("genre", equalTo("Sci-Fi, Thriller"))
                .body("duration", equalTo(148))
                .body("director", equalTo("Christopher Nolan"))
                .body("status", equalTo("SHOWING"))
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("Should fail to create movie when not authenticated")
    void testAddMovie_Unauthorized() {
        AddMovieRequest request = AddMovieRequest.builder()
                .title("Test Movie")
                .genre("Action")
                .description("Test description")
                .duration(120)
                .minimumAge(13)
                .director("Test Director")
                .actors("Test Actors")
                .trailerUrl("http://example.com/trailer.mp4")
                .status("SHOWING")
                .language("English")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/movies")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should fail to create movie with invalid data")
    @WithMockUser(roles = "ADMIN")
    void testAddMovie_InvalidData() {
        AddMovieRequest request = AddMovieRequest.builder()
                .title("")
                .genre("")
                .description("")
                .duration(-1)
                .minimumAge(-1)
                .director("")
                .actors("")
                .trailerUrl("")
                .status("INVALID_STATUS")
                .language("")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/movies")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should fail to create movie with duplicate title")
    @WithMockUser(roles = "ADMIN")
    void testAddMovie_DuplicateTitle() {
        // Create first movie
        Movie existingMovie = new Movie();
        existingMovie.setTitle("Duplicate Movie");
        existingMovie.setGenre("Action");
        existingMovie.setDescription("Description");
        existingMovie.setDuration(120);
        existingMovie.setMinimumAge(13);
        existingMovie.setDirector("Director");
        existingMovie.setActors("Actors");
        existingMovie.setTrailerUrl("http://example.com/trailer.mp4");
        existingMovie.setStatus(MovieStatus.SHOWING);
        existingMovie.setLanguage("English");
        movieRepo.save(existingMovie);

        // Try to create second movie with same title
        AddMovieRequest request = AddMovieRequest.builder()
                .title("Duplicate Movie")
                .genre("Drama")
                .description("Different description")
                .duration(90)
                .minimumAge(16)
                .director("Different Director")
                .actors("Different actors")
                .trailerUrl("http://example.com/trailer2.mp4")
                .status("SHOWING")
                .language("English")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/movies")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should get movie by ID successfully")
    @WithMockUser(roles = "USER")
    void testGetMovie_Success() {
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setGenre("Action");
        movie.setDescription("Test description");
        movie.setDuration(120);
        movie.setMinimumAge(13);
        movie.setDirector("Test Director");
        movie.setActors("Test Actors");
        movie.setTrailerUrl("http://example.com/trailer.mp4");
        movie.setStatus(MovieStatus.SHOWING);
        movie.setLanguage("English");
        movie = movieRepo.save(movie);

        given()
                .when()
                .get("/movies/" + movie.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("title", equalTo("Test Movie"))
                .body("genre", equalTo("Action"))
                .body("duration", equalTo(120))
                .body("id", equalTo(movie.getId().toString()));
    }

    @Test
    @DisplayName("Should return 404 when movie not found")
    void testGetMovie_NotFound() {
        UUID randomId = UUID.randomUUID();

        given()
                .when()
                .get("/movies/" + randomId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should update movie successfully")
    @WithMockUser(roles = "ADMIN")
    void testUpdateMovie_Success() {
        Movie movie = new Movie();
        movie.setTitle("Original Title");
        movie.setGenre("Action");
        movie.setDescription("Original description");
        movie.setDuration(120);
        movie.setMinimumAge(13);
        movie.setDirector("Original Director");
        movie.setActors("Original Actors");
        movie.setTrailerUrl("http://example.com/trailer.mp4");
        movie.setStatus(MovieStatus.SHOWING);
        movie.setLanguage("English");
        movie = movieRepo.save(movie);

        UpdateMovieRequest request = UpdateMovieRequest.builder()
                .title("Updated Title")
                .genre("Drama")
                .duration(150)
                .status("UPCOMING")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/movies/" + movie.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("title", equalTo("Updated Title"))
                .body("genre", equalTo("Drama"))
                .body("duration", equalTo(150))
                .body("status", equalTo("UPCOMING"))
                .body("description", equalTo("Original description")); // Not updated
    }

    @Test
    @DisplayName("Should fail to update movie when not authenticated as admin")
    @WithMockUser(roles = "USER")
    void testUpdateMovie_Forbidden() {
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setGenre("Action");
        movie.setDescription("Test description");
        movie.setDuration(120);
        movie.setMinimumAge(13);
        movie.setDirector("Test Director");
        movie.setActors("Test Actors");
        movie.setTrailerUrl("http://example.com/trailer.mp4");
        movie.setStatus(MovieStatus.SHOWING);
        movie.setLanguage("English");
        movie = movieRepo.save(movie);

        UpdateMovieRequest request = UpdateMovieRequest.builder()
                .title("Hacked Title")
                .genre("Action") // Add valid fields to pass validation
                .duration(120)
                .status("SHOWING")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/movies/" + movie.getId())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should delete movie successfully when no showtimes exist")
    @WithMockUser(roles = "ADMIN")
    void testDeleteMovie_Success() {
        Movie movie = new Movie();
        movie.setTitle("Movie to Delete");
        movie.setGenre("Action");
        movie.setDescription("Description");
        movie.setDuration(120);
        movie.setMinimumAge(13);
        movie.setDirector("Director");
        movie.setActors("Actors");
        movie.setTrailerUrl("http://example.com/trailer.mp4");
        movie.setStatus(MovieStatus.SHOWING);
        movie.setLanguage("English");
        movie = movieRepo.save(movie);

        UUID movieId = movie.getId();

        given()
                .when()
                .delete("/movies/" + movieId)
                .then()
                .statusCode(HttpStatus.OK.value());

        // Verify movie is deleted
        assert movieRepo.findById(movieId).isEmpty();
    }

    @Test
    @DisplayName("Should fail to delete movie when not authenticated as admin")
    @WithMockUser(roles = "USER")
    void testDeleteMovie_Forbidden() {
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setGenre("Action");
        movie.setDescription("Description");
        movie.setDuration(120);
        movie.setMinimumAge(13);
        movie.setDirector("Director");
        movie.setActors("Actors");
        movie.setTrailerUrl("http://example.com/trailer.mp4");
        movie.setStatus(MovieStatus.SHOWING);
        movie.setLanguage("English");
        movie = movieRepo.save(movie);

        given()
                .when()
                .delete("/movies/" + movie.getId())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should get all movies successfully")
    void testGetAllMovies_Success() {
        Movie movie1 = new Movie();
        movie1.setTitle("Movie 1");
        movie1.setGenre("Action");
        movie1.setDescription("Description 1");
        movie1.setDuration(120);
        movie1.setMinimumAge(13);
        movie1.setDirector("Director 1");
        movie1.setActors("Actors 1");
        movie1.setTrailerUrl("http://example.com/trailer1.mp4");
        movie1.setStatus(MovieStatus.SHOWING);
        movie1.setLanguage("English");
        movieRepo.save(movie1);

        Movie movie2 = new Movie();
        movie2.setTitle("Movie 2");
        movie2.setGenre("Drama");
        movie2.setDescription("Description 2");
        movie2.setDuration(90);
        movie2.setMinimumAge(16);
        movie2.setDirector("Director 2");
        movie2.setActors("Actors 2");
        movie2.setTrailerUrl("http://example.com/trailer2.mp4");
        movie2.setStatus(MovieStatus.UPCOMING);
        movie2.setLanguage("French");
        movieRepo.save(movie2);

        given()
                .when()
                .get("/movies")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(2))
                .body("[0].title", equalTo("Movie 1"))
                .body("[1].title", equalTo("Movie 2"));
    }

    @Test
    @DisplayName("Should search movies by title successfully")
    void testSearchMoviesByTitle_Success() {
        Movie movie1 = new Movie();
        movie1.setTitle("Inception");
        movie1.setGenre("Sci-Fi");
        movie1.setDescription("Description");
        movie1.setDuration(148);
        movie1.setMinimumAge(13);
        movie1.setDirector("Nolan");
        movie1.setActors("DiCaprio");
        movie1.setTrailerUrl("http://example.com/trailer.mp4");
        movie1.setStatus(MovieStatus.SHOWING);
        movie1.setLanguage("English");
        movieRepo.save(movie1);

        Movie movie2 = new Movie();
        movie2.setTitle("Interstellar");
        movie2.setGenre("Sci-Fi");
        movie2.setDescription("Description");
        movie2.setDuration(169);
        movie2.setMinimumAge(13);
        movie2.setDirector("Nolan");
        movie2.setActors("McConaughey");
        movie2.setTrailerUrl("http://example.com/trailer.mp4");
        movie2.setStatus(MovieStatus.SHOWING);
        movie2.setLanguage("English");
        movieRepo.save(movie2);

        given()
                .param("title", "Inception")
                .when()
                .get("/movies/search/title")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Inception"));
    }

    @Test
    @DisplayName("Should filter movies by status successfully")
    void testFilterMoviesByStatus_Success() {
        Movie movie1 = new Movie();
        movie1.setTitle("Showing Movie");
        movie1.setGenre("Action");
        movie1.setDescription("Description");
        movie1.setDuration(120);
        movie1.setMinimumAge(13);
        movie1.setDirector("Director");
        movie1.setActors("Actors");
        movie1.setTrailerUrl("http://example.com/trailer.mp4");
        movie1.setStatus(MovieStatus.SHOWING);
        movie1.setLanguage("English");
        movieRepo.save(movie1);

        Movie movie2 = new Movie();
        movie2.setTitle("Upcoming Movie");
        movie2.setGenre("Drama");
        movie2.setDescription("Description");
        movie2.setDuration(90);
        movie2.setMinimumAge(16);
        movie2.setDirector("Director");
        movie2.setActors("Actors");
        movie2.setTrailerUrl("http://example.com/trailer.mp4");
        movie2.setStatus(MovieStatus.UPCOMING);
        movie2.setLanguage("English");
        movieRepo.save(movie2);

        given()
                .param("status", "SHOWING")
                .when()
                .get("/movies/filter/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Showing Movie"))
                .body("[0].status", equalTo("SHOWING"));
    }

    @Test
    @DisplayName("Should filter movies by genre successfully")
    void testFilterMoviesByGenre_Success() {
        Movie movie1 = new Movie();
        movie1.setTitle("Action Movie");
        movie1.setGenre("Action, Thriller");
        movie1.setDescription("Description");
        movie1.setDuration(120);
        movie1.setMinimumAge(13);
        movie1.setDirector("Director");
        movie1.setActors("Actors");
        movie1.setTrailerUrl("http://example.com/trailer.mp4");
        movie1.setStatus(MovieStatus.SHOWING);
        movie1.setLanguage("English");
        movieRepo.save(movie1);

        Movie movie2 = new Movie();
        movie2.setTitle("Drama Movie");
        movie2.setGenre("Drama");
        movie2.setDescription("Description");
        movie2.setDuration(90);
        movie2.setMinimumAge(16);
        movie2.setDirector("Director");
        movie2.setActors("Actors");
        movie2.setTrailerUrl("http://example.com/trailer.mp4");
        movie2.setStatus(MovieStatus.SHOWING);
        movie2.setLanguage("English");
        movieRepo.save(movie2);

        given()
                .param("genre", "Action")
                .when()
                .get("/movies/filter/genre")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Action Movie"))
                .body("[0].genre", containsString("Action"));
    }

    @Test
    @DisplayName("Should search movies with multiple filters successfully")
    void testSearchMoviesAdvanced_Success() {
        Movie movie1 = new Movie();
        movie1.setTitle("Inception");
        movie1.setGenre("Sci-Fi");
        movie1.setDescription("Description");
        movie1.setDuration(148);
        movie1.setMinimumAge(13);
        movie1.setDirector("Nolan");
        movie1.setActors("DiCaprio");
        movie1.setTrailerUrl("http://example.com/trailer.mp4");
        movie1.setStatus(MovieStatus.SHOWING);
        movie1.setLanguage("English");
        movieRepo.save(movie1);

        Movie movie2 = new Movie();
        movie2.setTitle("Interstellar");
        movie2.setGenre("Sci-Fi");
        movie2.setDescription("Description");
        movie2.setDuration(169);
        movie2.setMinimumAge(13);
        movie2.setDirector("Nolan");
        movie2.setActors("McConaughey");
        movie2.setTrailerUrl("http://example.com/trailer.mp4");
        movie2.setStatus(MovieStatus.UPCOMING);
        movie2.setLanguage("English");
        movieRepo.save(movie2);

        given()
                .param("title", "Inception")
                .param("genre", "Sci-Fi")
                .param("status", "SHOWING")
                .when()
                .get("/movies")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Inception"))
                .body("[0].status", equalTo("SHOWING"));
    }
}
