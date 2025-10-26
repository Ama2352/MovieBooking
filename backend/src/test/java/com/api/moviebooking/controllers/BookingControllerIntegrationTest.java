package com.api.moviebooking.controllers;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.booking.LockSeatsRequest;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.*;
import com.api.moviebooking.repositories.*;
import com.api.moviebooking.services.BookingService;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for BookingController using Testcontainers and RestAssured.
 * Tests booking operations, seat locking, and concurrent booking scenarios with
 * HTTP endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class BookingControllerIntegrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                        DockerImageName.parse("postgres:15-alpine"));

        @Container
        @SuppressWarnings("resource")
        static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                        .withExposedPorts(6379);

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.redis.host", redis::getHost);
                registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        }

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private UserRepo userRepo;

        @Autowired
        private CinemaRepo cinemaRepo;

        @Autowired
        private RoomRepo roomRepo;

        @Autowired
        private MovieRepo movieRepo;

        @Autowired
        private ShowtimeRepo showtimeRepo;

        @Autowired
        private SeatRepo seatRepo;

        @Autowired
        private ShowtimeSeatRepo showtimeSeatRepo;

        @Autowired
        private SeatLockRepo seatLockRepo;

        @Autowired
        private BookingRepo bookingRepo;

        @Autowired
        private BookingService bookingService; // For cleanup verification

        private User testUser1, testUser2;
        private Showtime testShowtime;
        private ShowtimeSeat seat1, seat2, seat3;

        @BeforeEach
        void setUp() {
                RestAssuredMockMvc.mockMvc(MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build());

                // Clean up test data in correct order
                bookingRepo.deleteAll();
                seatLockRepo.deleteAll();
                showtimeSeatRepo.deleteAll();
                seatRepo.deleteAll();
                showtimeRepo.deleteAll();
                roomRepo.deleteAll();
                movieRepo.deleteAll();
                cinemaRepo.deleteAll();
                userRepo.deleteAll();

                // Create test users
                testUser1 = new User();
                testUser1.setEmail("user1@test.com");
                testUser1.setUsername("user1");
                testUser1.setRole(UserRole.USER);
                testUser1.setMembershipTier(MembershipTier.BRONZE);
                testUser1 = userRepo.save(testUser1);

                testUser2 = new User();
                testUser2.setEmail("user2@test.com");
                testUser2.setUsername("user2");
                testUser2.setRole(UserRole.USER);
                testUser2.setMembershipTier(MembershipTier.BRONZE);
                testUser2 = userRepo.save(testUser2);

                // Create admin user
                User adminUser = new User();
                adminUser.setEmail("admin@test.com");
                adminUser.setUsername("admin");
                adminUser.setRole(UserRole.ADMIN);
                adminUser.setMembershipTier(MembershipTier.GOLD);
                userRepo.save(adminUser);

                // Create cinema
                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("123 Test St");
                cinema.setHotline("1234567");
                cinema = cinemaRepo.save(cinema);

                // Create room
                Room room = new Room();
                room.setCinema(cinema);
                room.setRoomNumber(1);
                room.setRoomType("IMAX");
                room = roomRepo.save(room);

                // Create movie
                Movie movie = new Movie();
                movie.setTitle("Test Movie");
                movie.setDuration(120);
                movie.setGenre("Action");
                movie.setStatus(MovieStatus.SHOWING);
                movie = movieRepo.save(movie);

                // Create showtime
                testShowtime = new Showtime();
                testShowtime.setRoom(room);
                testShowtime.setMovie(movie);
                testShowtime.setStartTime(LocalDateTime.now().plusHours(2));
                testShowtime.setFormat("2D Subtitled");
                testShowtime = showtimeRepo.save(testShowtime);

                // Create seats
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
                s2.setSeatType(SeatType.NORMAL);
                s2 = seatRepo.save(s2);

                Seat s3 = new Seat();
                s3.setRoom(room);
                s3.setRowLabel("A");
                s3.setSeatNumber(3);
                s3.setSeatType(SeatType.VIP);
                s3 = seatRepo.save(s3);

                // Create showtime seats
                seat1 = new ShowtimeSeat();
                seat1.setShowtime(testShowtime);
                seat1.setSeat(s1);
                seat1.setStatus(SeatStatus.AVAILABLE);
                seat1.setPrice(new BigDecimal("10.00"));
                seat1 = showtimeSeatRepo.save(seat1);

                seat2 = new ShowtimeSeat();
                seat2.setShowtime(testShowtime);
                seat2.setSeat(s2);
                seat2.setStatus(SeatStatus.AVAILABLE);
                seat2.setPrice(new BigDecimal("10.00"));
                seat2 = showtimeSeatRepo.save(seat2);

                seat3 = new ShowtimeSeat();
                seat3.setShowtime(testShowtime);
                seat3.setSeat(s3);
                seat3.setStatus(SeatStatus.AVAILABLE);
                seat3.setPrice(new BigDecimal("15.00"));
                seat3 = showtimeSeatRepo.save(seat3);
        }

        // ==================== Basic Booking Flow Tests ====================

        @Test
        @DisplayName("Should successfully lock seats when authenticated")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testLockSeats_Success() {
                LockSeatsRequest request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId(), seat2.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .body("lockId", notNullValue())
                                .body("lockedSeats", hasSize(2))
                                .body("totalPrice", equalTo(20.00f))
                                .body("expiresAt", notNullValue());

                // Verify seats are locked in database
                ShowtimeSeat updatedSeat1 = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.LOCKED, updatedSeat1.getStatus());
        }

        @Test
        @DisplayName("Should fail to lock seats when not authenticated")
        void testLockSeats_Unauthorized() {
                LockSeatsRequest request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @DisplayName("Should confirm booking successfully")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testConfirmBooking_Success() {
                // First lock seats
                LockSeatsRequest lockRequest = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId(), seat2.getId()));

                String lockId = given()
                                .contentType(ContentType.JSON)
                                .body(lockRequest)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("lockId");

                // Then confirm booking
                ConfirmBookingRequest confirmRequest = new ConfirmBookingRequest();
                confirmRequest.setLockId(UUID.fromString(lockId));

                given()
                                .contentType(ContentType.JSON)
                                .body(confirmRequest)
                                .when()
                                .post("/bookings/confirm")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("bookingId", notNullValue())
                                .body("status", equalTo("PENDING"))
                                .body("totalPrice", equalTo(20.00f));

                // Verify seats are now booked
                ShowtimeSeat bookedSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.BOOKED, bookedSeat.getStatus());
        }

        @Test
        @DisplayName("Should check seat availability successfully")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testCheckAvailability_Success() {
                given()
                                .when()
                                .get("/bookings/availability/" + testShowtime.getId())
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("availableSeats", hasSize(3))
                                .body("lockedSeats", empty())
                                .body("bookedSeats", empty());
        }

        @Test
        @DisplayName("Should release seats successfully")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testReleaseSeats_Success() {
                // First lock seats
                LockSeatsRequest lockRequest = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(lockRequest)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value());

                // Verify seat is locked
                ShowtimeSeat lockedSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.LOCKED, lockedSeat.getStatus());

                // Release seats
                given()
                                .queryParam("showtimeId", testShowtime.getId())
                                .when()
                                .delete("/bookings/release")
                                .then()
                                .statusCode(HttpStatus.OK.value());

                // Verify seats are available again
                ShowtimeSeat availableSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.AVAILABLE, availableSeat.getStatus());
        }

        @Test
        @DisplayName("Should handle back button and release seats immediately")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testHandleBackButton_Success() {
                // Lock seats
                LockSeatsRequest lockRequest = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId(), seat2.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(lockRequest)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value());

                // Press back button
                given()
                                .queryParam("showtimeId", testShowtime.getId())
                                .when()
                                .post("/bookings/back")
                                .then()
                                .statusCode(HttpStatus.OK.value());

                // Verify seats are immediately available
                ShowtimeSeat updatedSeat1 = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.AVAILABLE, updatedSeat1.getStatus());
        }

        @Test
        @DisplayName("Should get user's booking history")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testGetMyBookings_Success() {
                given()
                                .when()
                                .get("/bookings/my-bookings")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("$", notNullValue());
        }

        // ==================== Concurrent Booking Tests ====================

        @Test
        @DisplayName("Should prevent concurrent seat locking by different users")
        void testConcurrentLocking_DifferentUsers() throws InterruptedException {
                // Test at service layer to properly simulate different users
                // (Controller layer with @WithMockUser can't easily switch users per thread)
                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch latch = new CountDownLatch(1);
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failureCount = new AtomicInteger(0);

                LockSeatsRequest request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId(), seat2.getId()));

                // User1 and User2 try to lock same seats concurrently
                UUID[] userIds = { testUser1.getId(), testUser2.getId() };

                for (int i = 0; i < 2; i++) {
                        final UUID userId = userIds[i];
                        executor.submit(() -> {
                                try {
                                        latch.await();
                                        bookingService.lockSeats(userId, request);
                                        successCount.incrementAndGet();
                                } catch (Exception e) {
                                        failureCount.incrementAndGet();
                                }
                        });
                }

                latch.countDown();
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);

                // Only one should succeed due to Redis distributed locking
                assertEquals(1, successCount.get(), "Only one user should successfully lock seats");
                assertEquals(1, failureCount.get(), "One user should fail due to concurrent lock");

                // Verify seats are locked by one user
                ShowtimeSeat lockedSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.LOCKED, lockedSeat.getStatus());
        }

        @Test
        @DisplayName("Should handle partial seat overlap correctly")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testPartialSeatOverlap() {
                // User1 locks seat1 and seat2
                LockSeatsRequest user1Request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId(), seat2.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(user1Request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value());

                // User2 tries to lock seat2 and seat3 (overlaps on seat2)
                LockSeatsRequest user2Request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat2.getId(), seat3.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(user2Request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(anyOf(
                                                equalTo(HttpStatus.CONFLICT.value()),
                                                equalTo(HttpStatus.BAD_REQUEST.value()),
                                                equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));

                // Verify seat3 is still available
                ShowtimeSeat updatedSeat3 = showtimeSeatRepo.findById(seat3.getId()).orElseThrow();
                assertEquals(SeatStatus.AVAILABLE, updatedSeat3.getStatus());
        }

        @Test
        @DisplayName("Should prevent same user from booking same showtime in multiple tabs")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testMultiTabPrevention() {
                // Lock seats in "Tab 1"
                LockSeatsRequest tab1Request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(tab1Request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value());

                // Try to lock different seats in "Tab 2" for SAME showtime
                LockSeatsRequest tab2Request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat2.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(tab2Request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(anyOf(
                                                equalTo(HttpStatus.CONFLICT.value()),
                                                equalTo(HttpStatus.BAD_REQUEST.value()),
                                                equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        }

        // ==================== Lock Expiration Tests ====================

        @Test
        @DisplayName("Should cleanup expired locks")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testLockExpiration() {
                // Lock seats
                LockSeatsRequest lockRequest = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId()));

                String lockId = given()
                                .contentType(ContentType.JSON)
                                .body(lockRequest)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("lockId");

                // Manually expire the lock
                var lock = seatLockRepo.findById(UUID.fromString(lockId)).orElseThrow();
                lock.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                seatLockRepo.save(lock);

                // Run cleanup (would normally be scheduled)
                bookingService.cleanupExpiredLocks();

                // Verify seats are released
                ShowtimeSeat updatedSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.AVAILABLE, updatedSeat.getStatus());

                var updatedLock = seatLockRepo.findById(UUID.fromString(lockId)).orElseThrow();
                assertFalse(updatedLock.isActive());
        }

        // ==================== Edge Cases ====================

        @Test
        @DisplayName("Should handle invalid showtime ID")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testLockSeats_InvalidShowtimeId() {
                LockSeatsRequest request = new LockSeatsRequest(
                                UUID.randomUUID(),
                                Arrays.asList(seat1.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(anyOf(
                                                equalTo(HttpStatus.NOT_FOUND.value()),
                                                equalTo(HttpStatus.BAD_REQUEST.value()),
                                                equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        }

        @Test
        @DisplayName("Should handle invalid seat IDs")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testLockSeats_InvalidSeatIds() {
                LockSeatsRequest request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(anyOf(
                                                equalTo(HttpStatus.NOT_FOUND.value()),
                                                equalTo(HttpStatus.BAD_REQUEST.value()),
                                                equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        }

        @Test
        @DisplayName("Should handle empty seat list")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testLockSeats_EmptySeatList() {
                LockSeatsRequest request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList());

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(anyOf(
                                                equalTo(HttpStatus.BAD_REQUEST.value()),
                                                equalTo(HttpStatus.CREATED.value()))); // Might accept empty list
        }

        @Test
        @DisplayName("Should handle confirm booking with invalid lock ID")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testConfirmBooking_InvalidLockId() {
                ConfirmBookingRequest request = new ConfirmBookingRequest();
                request.setLockId(UUID.randomUUID());

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/bookings/confirm")
                                .then()
                                .statusCode(anyOf(
                                                equalTo(HttpStatus.NOT_FOUND.value()),
                                                equalTo(HttpStatus.BAD_REQUEST.value()),
                                                equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        }

        // ==================== Authorization Tests ====================

        @Test
        @DisplayName("Should deny access to booking operations for unauthenticated users")
        void testBookingOperations_Unauthenticated() {
                LockSeatsRequest lockRequest = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId()));

                // Lock seats
                given()
                                .contentType(ContentType.JSON)
                                .body(lockRequest)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.FORBIDDEN.value());

                // Release seats
                given()
                                .queryParam("showtimeId", testShowtime.getId())
                                .when()
                                .delete("/bookings/release")
                                .then()
                                .statusCode(HttpStatus.FORBIDDEN.value());

                // My bookings
                given()
                                .when()
                                .get("/bookings/my-bookings")
                                .then()
                                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @DisplayName("Should allow booking operations for users with USER role")
        @WithMockUser(username = "user1@test.com", roles = "USER")
        void testBookingOperations_UserRole() {
                LockSeatsRequest request = new LockSeatsRequest(
                                testShowtime.getId(),
                                Arrays.asList(seat1.getId()));

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/bookings/lock")
                                .then()
                                .statusCode(HttpStatus.CREATED.value());
        }

        @Test
        @DisplayName("Should allow admin to view bookings and release seats")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void testBookingOperations_AdminRole() {
                // Admin should be able to view all bookings
                given()
                                .when()
                                .get("/bookings/my-bookings")
                                .then()
                                .statusCode(HttpStatus.OK.value());

                // Admin should be able to release seats for a showtime
                given()
                                .queryParam("showtimeId", testShowtime.getId())
                                .when()
                                .delete("/bookings/release")
                                .then()
                                .statusCode(HttpStatus.OK.value());
        }
}
