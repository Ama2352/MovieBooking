package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.api.moviebooking.models.dtos.showtimeSeat.UpdateShowtimeSeatRequest;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.repositories.*;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for ShowtimeSeatController.
 * Tests showtime seat availability, pricing, status management, and admin
 * operations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Showtime Seat Integration Tests")
class ShowtimeSeatIntegrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                        DockerImageName.parse("postgres:15-alpine"));

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private ShowtimeSeatRepo showtimeSeatRepo;

        @Autowired
        private ShowtimeRepo showtimeRepo;

        @Autowired
        private SeatRepo seatRepo;

        @Autowired
        private RoomRepo roomRepo;

        @Autowired
        private MovieRepo movieRepo;

        @Autowired
        private CinemaRepo cinemaRepo;

        private Showtime testShowtime;
        private ShowtimeSeat seat1, seat2, seat3;

        @BeforeEach
        void setUp() {
                RestAssuredMockMvc.mockMvc(MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build());

                showtimeSeatRepo.deleteAll();
                seatRepo.deleteAll();
                showtimeRepo.deleteAll();
                roomRepo.deleteAll();
                movieRepo.deleteAll();
                cinemaRepo.deleteAll();

                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("123 Test St");
                cinema.setHotline("1234567");
                cinema = cinemaRepo.save(cinema);

                Room room = new Room();
                room.setCinema(cinema);
                room.setRoomNumber(1);
                room.setRoomType("Standard");
                room = roomRepo.save(room);

                Movie movie = new Movie();
                movie.setTitle("Test Movie");
                movie.setDuration(120);
                movie.setGenre("Action");
                movie.setStatus(MovieStatus.SHOWING);
                movie = movieRepo.save(movie);

                testShowtime = new Showtime();
                testShowtime.setRoom(room);
                testShowtime.setMovie(movie);
                testShowtime.setStartTime(LocalDateTime.now().plusHours(2));
                testShowtime.setFormat("2D");
                testShowtime = showtimeRepo.save(testShowtime);

                Seat s1 = new Seat();
                s1.setRoom(room);
                s1.setRowLabel("A");
                s1.setSeatNumber(1);
                s1.setSeatType(SeatType.NORMAL);
                s1 = seatRepo.save(s1);

                Seat s2 = new Seat();
                s2.setRoom(room);
                s2.setRowLabel("A");
                s2.setSeatNumber(2);
                s2.setSeatType(SeatType.VIP);
                s2 = seatRepo.save(s2);

                Seat s3 = new Seat();
                s3.setRoom(room);
                s3.setRowLabel("B");
                s3.setSeatNumber(1);
                s3.setSeatType(SeatType.COUPLE);
                s3 = seatRepo.save(s3);

                seat1 = new ShowtimeSeat();
                seat1.setShowtime(testShowtime);
                seat1.setSeat(s1);
                seat1.setStatus(SeatStatus.AVAILABLE);
                seat1.setPrice(new BigDecimal("100000"));
                seat1 = showtimeSeatRepo.save(seat1);

                seat2 = new ShowtimeSeat();
                seat2.setShowtime(testShowtime);
                seat2.setSeat(s2);
                seat2.setStatus(SeatStatus.AVAILABLE);
                seat2.setPrice(new BigDecimal("120000"));
                seat2 = showtimeSeatRepo.save(seat2);

                seat3 = new ShowtimeSeat();
                seat3.setShowtime(testShowtime);
                seat3.setSeat(s3);
                seat3.setStatus(SeatStatus.BOOKED);
                seat3.setPrice(new BigDecimal("150000"));
                seat3 = showtimeSeatRepo.save(seat3);
        }

        // ========================================================================
        // Query Tests
        // ========================================================================

        @Nested
        @DisplayName("Showtime Seat Query Operations")
        class QueryTests {

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @DisplayName("Should get showtime seat by ID")
                void testGetShowtimeSeat_Success() {
                        given()
                                        .when()
                                        .get("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("showtimeSeatId", equalTo(seat1.getId().toString()))
                                        .body("showtimeId", equalTo(testShowtime.getId().toString()))
                                        .body("status", equalTo("AVAILABLE"))
                                        .body("price", equalTo(100000.0f));
                }

                @Test
                @RegressionTest
                @DisplayName("Should return 404 for non-existent showtime seat")
                void testGetShowtimeSeat_NotFound() {
                        given()
                                        .when()
                                        .get("/showtime-seats/" + UUID.randomUUID())
                                        .then()
                                        .statusCode(HttpStatus.NOT_FOUND.value());
                }

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @DisplayName("Should get all seats for showtime")
                void testGetSeatsByShowtime_Success() {
                        given()
                                        .when()
                                        .get("/showtime-seats/showtime/" + testShowtime.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("size()", equalTo(3))
                                        .body("[0].showtimeId", equalTo(testShowtime.getId().toString()));
                }

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @DisplayName("Should get available seats")
                void testGetAvailableSeats_Success() {
                        given()
                                        .when()
                                        .get("/showtime-seats/showtime/" + testShowtime.getId() + "/available")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("size()", equalTo(2))
                                        .body("[0].status", equalTo("AVAILABLE"))
                                        .body("[1].status", equalTo("AVAILABLE"));
                }

                @Test
                @RegressionTest
                @DisplayName("Should return empty list when no available seats")
                void testGetAvailableSeats_Empty() {
                        // Mark all seats as booked
                        seat1.setStatus(SeatStatus.BOOKED);
                        seat2.setStatus(SeatStatus.BOOKED);
                        showtimeSeatRepo.save(seat1);
                        showtimeSeatRepo.save(seat2);

                        given()
                                        .when()
                                        .get("/showtime-seats/showtime/" + testShowtime.getId() + "/available")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("size()", equalTo(0));
                }

                @Test
                @RegressionTest
                @DisplayName("Should handle non-existent showtime")
                void testGetSeatsByShowtime_NotFound() {
                        given()
                                        .when()
                                        .get("/showtime-seats/showtime/" + UUID.randomUUID())
                                        .then()
                                        .statusCode(anyOf(
                                                        equalTo(HttpStatus.OK.value()),
                                                        equalTo(HttpStatus.NOT_FOUND.value())))
                                        .body("size()", equalTo(0));
                }
        }

        // ========================================================================
        // Admin Update Tests
        // ========================================================================

        @Nested
        @DisplayName("Admin Update Operations")
        class AdminUpdateTests {

                @Test
                @SanityTest
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should update seat status")
                void testUpdateShowtimeSeat_Status() {
                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setStatus("LOCKED");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("LOCKED"));

                        ShowtimeSeat updated = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                        assertEquals(SeatStatus.LOCKED, updated.getStatus());
                }

                @Test
                @SanityTest
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should update seat price")
                void testUpdateShowtimeSeat_Price() {
                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setPrice(new BigDecimal("110000"));
                        request.setStatus("AVAILABLE"); // Ensure status is not updated

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("price", equalTo(110000)); // Price returned as integer, not float

                        ShowtimeSeat updated = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                        assertTrue(updated.getPrice().compareTo(new BigDecimal("110000")) == 0,
                                        "Price should be 110000");
                }

                @Test
                @SanityTest
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should update both status and price")
                void testUpdateShowtimeSeat_Both() {
                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setStatus("BOOKED");
                        request.setPrice(new BigDecimal("95000"));

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("BOOKED"))
                                        .body("price", equalTo(95000)); // Price returned as integer, not float

                        ShowtimeSeat updated = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                        assertEquals(SeatStatus.BOOKED, updated.getStatus());
                        assertTrue(updated.getPrice().compareTo(new BigDecimal("95000")) == 0);
                }

                @Test
                @RegressionTest
                @DisplayName("Should fail without authentication")
                void testUpdateShowtimeSeat_Unauthorized() {
                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setStatus("BOOKED");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.FORBIDDEN.value());
                }

                @Test
                @RegressionTest
                @WithMockUser(roles = "USER")
                @DisplayName("Should fail with USER role")
                void testUpdateShowtimeSeat_Forbidden() {
                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setStatus("BOOKED");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.FORBIDDEN.value());
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should reset seat status to available")
                void testResetSeatStatus_Success() {
                        // First set seat to booked
                        seat1.setStatus(SeatStatus.BOOKED);
                        showtimeSeatRepo.save(seat1);

                        given()
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId() + "/reset")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("AVAILABLE"));

                        ShowtimeSeat reset = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                        assertEquals(SeatStatus.AVAILABLE, reset.getStatus());
                }

                @Test
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should reset locked seat")
                void testResetSeatStatus_Locked() {
                        seat1.setStatus(SeatStatus.LOCKED);
                        showtimeSeatRepo.save(seat1);

                        given()
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId() + "/reset")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("AVAILABLE"));
                }
        }

        // ========================================================================
        // Price Recalculation Tests
        // ========================================================================

        @Nested
        @DisplayName("Price Recalculation Operations")
        class PriceRecalculationTests {

                @Test
                @SanityTest
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should recalculate prices for all seats in showtime")
                void testRecalculatePrices_Success() {
                        given()
                                        .when()
                                        .post("/showtime-seats/showtime/" + testShowtime.getId()
                                                        + "/recalculate-prices")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("size()", equalTo(3))
                                        .body("[0].price", notNullValue());
                }

                @Test
                @RegressionTest
                @DisplayName("Should fail recalculation without authentication")
                void testRecalculatePrices_Unauthorized() {
                        given()
                                        .when()
                                        .post("/showtime-seats/showtime/" + testShowtime.getId()
                                                        + "/recalculate-prices")
                                        .then()
                                        .statusCode(HttpStatus.FORBIDDEN.value());
                }

                @Test
                @RegressionTest
                @WithMockUser(roles = "USER")
                @DisplayName("Should fail recalculation with USER role")
                void testRecalculatePrices_Forbidden() {
                        given()
                                        .when()
                                        .post("/showtime-seats/showtime/" + testShowtime.getId()
                                                        + "/recalculate-prices")
                                        .then()
                                        .statusCode(HttpStatus.FORBIDDEN.value());
                }

                @Test
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should handle non-existent showtime for recalculation")
                void testRecalculatePrices_ShowtimeNotFound() {
                        given()
                                        .when()
                                        .post("/showtime-seats/showtime/" + UUID.randomUUID()
                                                        + "/recalculate-prices")
                                        .then()
                                        .statusCode(anyOf(
                                                        equalTo(HttpStatus.NOT_FOUND.value()),
                                                        equalTo(HttpStatus.OK.value())));
                }
        }

        // ========================================================================
        // Business Logic Tests
        // ========================================================================

        @Nested
        @DisplayName("Showtime Seat Business Logic")
        class BusinessLogicTests {

                @Test
                @RegressionTest
                @DisplayName("Should differentiate prices by seat type")
                void testPriceDifferentiation() {
                        given()
                                        .when()
                                        .get("/showtime-seats/showtime/" + testShowtime.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("size()", equalTo(3));

                        // Normal seat should be cheapest
                        ShowtimeSeat normal = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                        // VIP should be more expensive
                        ShowtimeSeat vip = showtimeSeatRepo.findById(seat2.getId()).orElseThrow();
                        // Couple should be most expensive
                        ShowtimeSeat couple = showtimeSeatRepo.findById(seat3.getId()).orElseThrow();

                        assertTrue(vip.getPrice().compareTo(normal.getPrice()) >= 0,
                                        "VIP should cost at least as much as normal");
                        assertTrue(couple.getPrice().compareTo(vip.getPrice()) >= 0,
                                        "Couple should cost at least as much as VIP");
                }

                @Test
                @RegressionTest
                @DisplayName("Should maintain seat-showtime relationship")
                void testSeatShowtimeRelationship() {
                        var seats = showtimeSeatRepo.findByShowtimeId(testShowtime.getId());

                        for (ShowtimeSeat showtimeSeat : seats) {
                                assertEquals(testShowtime.getId(), showtimeSeat.getShowtime().getId());
                                assertNotNull(showtimeSeat.getSeat());
                        }
                }

                @Test
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should allow status transition AVAILABLE -> LOCKED")
                void testStatusTransition_AvailableToLocked() {
                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setStatus("LOCKED");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("LOCKED"));
                }

                @Test
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should allow status transition LOCKED -> BOOKED")
                void testStatusTransition_LockedToBooked() {
                        // First lock the seat
                        seat1.setStatus(SeatStatus.LOCKED);
                        showtimeSeatRepo.save(seat1);

                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setStatus("BOOKED");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat1.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("BOOKED"));
                }

                @Test
                @RegressionTest
                @WithMockUser(roles = "ADMIN")
                @DisplayName("Should allow status transition BOOKED -> AVAILABLE (refund scenario)")
                void testStatusTransition_BookedToAvailable() {
                        UpdateShowtimeSeatRequest request = new UpdateShowtimeSeatRequest();
                        request.setStatus("AVAILABLE");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .put("/showtime-seats/" + seat3.getId()) // seat3 is BOOKED
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("AVAILABLE"));
                }
        }
}
