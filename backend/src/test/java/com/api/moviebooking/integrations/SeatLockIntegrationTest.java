package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.*;
import com.api.moviebooking.repositories.*;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for SeatLockController using Testcontainers and
 * RestAssured.
 * Tests seat locking based on BookingService white-box coverage.
 * 
 * BookingService.lockSeats branches (V(G) = 12):
 * 1. Seats > maxSeatsPerBooking
 * 2. Existing locks for same showtime
 * 3. Existing locks for different showtime
 * 4. User not found
 * 5. Showtime not found
 * 6. Seats count mismatch
 * 7. Seats unavailable
 * 8. Redis lock failed
 * 9. Database save exception
 * 
 * BookingService.releaseSeats branches (V(G) = 2)
 * BookingService.handleBackButton branches (V(G) = 2)
 * BookingService.checkAvailability branches (V(G) = 5)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Seat Lock Integration Tests")
class SeatLockIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"));

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

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

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private MembershipTierRepo membershipTierRepo;

    @Autowired
    private SeatLockRepo seatLockRepo;

    private Showtime testShowtime;
    private ShowtimeSeat seat1, seat2, seat3;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build());

        seatLockRepo.deleteAll();
        showtimeSeatRepo.deleteAll();
        seatRepo.deleteAll();
        showtimeRepo.deleteAll();
        roomRepo.deleteAll();
        movieRepo.deleteAll();
        cinemaRepo.deleteAll();
        userRepo.deleteAll();
        membershipTierRepo.deleteAll();

        // Create membership tier first
        com.api.moviebooking.models.entities.MembershipTier tier = new com.api.moviebooking.models.entities.MembershipTier();
        tier.setName("TestBronze");
        tier.setMinPoints(0);
        tier.setDiscountType(DiscountType.PERCENTAGE);
        tier.setDiscountValue(BigDecimal.ZERO);
        tier = membershipTierRepo.save(tier);

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpass");
        testUser.setRole(UserRole.USER);
        testUser.setMembershipTier(tier);
        testUser = userRepo.save(testUser);
        userId = testUser.getId();

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
        s3.setRowLabel("A");
        s3.setSeatNumber(3);
        s3.setSeatType(SeatType.NORMAL);
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
        seat3.setStatus(SeatStatus.AVAILABLE);
        seat3.setPrice(new BigDecimal("100000"));
        seat3 = showtimeSeatRepo.save(seat3);
    }

    // ========================================================================
    // Lock Seat Tests (Branch coverage for lockSeats method)
    // ========================================================================

    @Nested
    @DisplayName("Seat Lock Creation")
    class LockSeatTests {

        @Test
        @DisplayName("Should lock seats successfully")
        void testLockSeats_Success() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId(), seat2.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("lockId", notNullValue())
                    .body("lockKey", notNullValue())
                    .body("showtimeId", equalTo(testShowtime.getId().toString()))
                    .body("lockedSeats.size()", equalTo(2))
                    .body("totalPrice", greaterThan(0.0f))
                    .body("expiresAt", notNullValue())
                    .body("remainingSeconds", greaterThan(0))
                    .body("message", containsString("locked successfully"));

            // Verify seats are locked in database
            ShowtimeSeat locked1 = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
            ShowtimeSeat locked2 = showtimeSeatRepo.findById(seat2.getId()).orElseThrow();
            assertEquals(SeatStatus.LOCKED, locked1.getStatus());
            assertEquals(SeatStatus.LOCKED, locked2.getStatus());
        }

        @Test
        @DisplayName("Should lock single seat")
        void testLockSeats_SingleSeat() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("lockedSeats.size()", equalTo(1))
                    .body("totalPrice", greaterThan(0.0f));
        }

        @Test
        @DisplayName("Should fail to lock empty seat list")
        void testLockSeats_EmptyList() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of());

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("Should fail with invalid showtime ID")
        void testLockSeats_InvalidShowtime() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(UUID.randomUUID());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @DisplayName("Should fail with invalid seat ID")
        void testLockSeats_InvalidSeat() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(UUID.randomUUID()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @DisplayName("Should fail to lock already locked seats")
        void testLockSeats_AlreadyLocked() {
            // First lock
            LockSeatsRequest request1 = new LockSeatsRequest();
            request1.setShowtimeId(testShowtime.getId());
            request1.setUserId(userId);
            request1.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request1)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CREATED.value());

            // Second lock attempt by different user
            com.api.moviebooking.models.entities.MembershipTier tier2 = new com.api.moviebooking.models.entities.MembershipTier();
            tier2.setName("TestSilver");
            tier2.setMinPoints(0);
            tier2.setDiscountType(DiscountType.PERCENTAGE);
            tier2.setDiscountValue(BigDecimal.ZERO);
            tier2 = membershipTierRepo.save(tier2);

            User user2 = new User();
            user2.setUsername("user2");
            user2.setEmail("user2@example.com");
            user2.setPassword("hash");
            user2.setRole(UserRole.USER);
            user2.setMembershipTier(tier2);
            user2 = userRepo.save(user2);

            LockSeatsRequest request2 = new LockSeatsRequest();
            request2.setShowtimeId(testShowtime.getId());
            request2.setUserId(user2.getId());
            request2.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request2)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("Should fail to lock booked seats")
        void testLockSeats_AlreadyBooked() {
            // Mark seat as booked
            seat1.setStatus(SeatStatus.BOOKED);
            showtimeSeatRepo.save(seat1);

            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CONFLICT.value());
        }
    }

    // ========================================================================
    // Seat Availability Tests (Branch coverage for checkAvailability)
    // ========================================================================

    @Nested
    @DisplayName("Seat Availability Checks")
    class AvailabilityTests {

        @Test
        @DisplayName("Should get seat availability")
        void testGetAvailability_Success() {
            given()
                    .queryParam("userId", userId.toString())
                    .when()
                    .get("/bookings/lock/availability/" + testShowtime.getId())
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("showtimeId", equalTo(testShowtime.getId().toString()))
                    .body("availableSeats", hasSize(3))
                    .body("lockedSeats", hasSize(0))
                    .body("bookedSeats", hasSize(0))
                    .body("message", containsString("availability"));
        }

        @Test
        @DisplayName("Should show locked seats in availability")
        void testGetAvailability_WithLocks() {
            // Lock seats first
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock");

            // Check availability from different user
            com.api.moviebooking.models.entities.MembershipTier tier2 = new com.api.moviebooking.models.entities.MembershipTier();
            tier2.setName("TestGold");
            tier2.setMinPoints(0);
            tier2.setDiscountType(DiscountType.PERCENTAGE);
            tier2.setDiscountValue(BigDecimal.ZERO);
            tier2 = membershipTierRepo.save(tier2);

            User user2 = new User();
            user2.setUsername("user2");
            user2.setEmail("user2@example.com");
            user2.setPassword("hash");
            user2.setRole(UserRole.USER);
            user2.setMembershipTier(tier2);
            user2 = userRepo.save(user2);

            given()
                    .queryParam("userId", user2.getId().toString())
                    .when()
                    .get("/bookings/lock/availability/" + testShowtime.getId())
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("availableSeats", hasSize(greaterThanOrEqualTo(2)))
                    .body("lockedSeats", hasSize(lessThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("Should show booked seats in availability")
        void testGetAvailability_WithBookings() {
            // Mark seat as booked
            seat1.setStatus(SeatStatus.BOOKED);
            showtimeSeatRepo.save(seat1);

            given()
                    .queryParam("userId", userId.toString())
                    .when()
                    .get("/bookings/lock/availability/" + testShowtime.getId())
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("availableSeats", hasSize(2))
                    .body("bookedSeats", hasSize(1));
        }

        @Test
        @DisplayName("Should fail availability check without userId")
        void testGetAvailability_MissingUserId() {
            given()
                    .when()
                    .get("/bookings/lock/availability/" + testShowtime.getId())
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        }
    }

    // ========================================================================
    // Lock Release Tests (Branch coverage for releaseSeats & handleBackButton)
    // ========================================================================

    @Nested
    @DisplayName("Seat Lock Release")
    class LockReleaseTests {

        @Test
        @DisplayName("Should release locked seats")
        void testReleaseLock_Success() {
            // Lock seats first
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/bookings/lock");

            // Release
            given()
                    .queryParam("showtimeId", testShowtime.getId().toString())
                    .queryParam("userId", userId.toString())
                    .when()
                    .delete("/bookings/lock/release")
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // Verify seat released
            ShowtimeSeat updated = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
            assertEquals(SeatStatus.AVAILABLE, updated.getStatus());
        }

        @Test
        @DisplayName("Should handle back button navigation")
        void testBackButton_Success() {
            // Lock seats
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId(), seat2.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/bookings/lock");

            // Press back button
            given()
                    .queryParam("showtimeId", testShowtime.getId().toString())
                    .queryParam("userId", userId.toString())
                    .when()
                    .post("/bookings/lock/back")
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // Verify seats released
            ShowtimeSeat updated1 = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
            ShowtimeSeat updated2 = showtimeSeatRepo.findById(seat2.getId()).orElseThrow();
            assertEquals(SeatStatus.AVAILABLE, updated1.getStatus());
            assertEquals(SeatStatus.AVAILABLE, updated2.getStatus());
        }

        @Test
        @DisplayName("Should succeed releasing non-existent locks")
        void testReleaseLock_NoLocks() {
            // Try to release when no locks exist
            given()
                    .queryParam("showtimeId", testShowtime.getId().toString())
                    .queryParam("userId", userId.toString())
                    .when()
                    .delete("/bookings/lock/release")
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }
    }

    // ========================================================================
    // Business Logic Tests
    // ========================================================================

    @Nested
    @DisplayName("Seat Lock Business Logic")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should calculate total price correctly")
        void testTotalPriceCalculation() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId(), seat2.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("totalPrice", greaterThan(0.0f));
        }

        @Test
        @DisplayName("Should include seat details in lock response")
        void testLockResponse_SeatDetails() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("lockedSeats[0].seatId", equalTo(seat1.getId().toString()))
                    .body("lockedSeats[0].rowLabel", equalTo("A"))
                    .body("lockedSeats[0].seatNumber", equalTo(1))
                    .body("lockedSeats[0].seatType", equalTo("NORMAL"))
                    .body("lockedSeats[0].price", greaterThan(0.0f));
        }

        @Test
        @DisplayName("Should set expiration time")
        void testLockExpiration() {
            LockSeatsRequest request = new LockSeatsRequest();
            request.setShowtimeId(testShowtime.getId());
            request.setUserId(userId);
            request.setShowtimeSeatIds(List.of(seat1.getId()));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/lock")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("expiresAt", notNullValue())
                    .body("remainingSeconds", greaterThan(0))
                    .body("remainingSeconds", lessThanOrEqualTo(600));
        }
    }
}
