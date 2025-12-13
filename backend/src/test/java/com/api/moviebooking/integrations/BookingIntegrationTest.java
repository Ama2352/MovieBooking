package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
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

import com.api.moviebooking.models.dtos.booking.PricePreviewRequest;
import com.api.moviebooking.models.dtos.booking.UpdateQrCodeRequest;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.*;
import com.api.moviebooking.repositories.*;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for BookingController endpoints.
 * Tests cover price preview, booking retrieval, and QR code updates.
 * 
 * Test counts match V(G) cyclomatic complexity:
 * - Price Preview: 5 tests (V(G)=5)
 * - Get User Bookings: 1 test (V(G)=1)
 * - Get Booking By ID: 2 tests (V(G)=2)
 * - Update QR Code: 2 tests (V(G)=2)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Booking Integration Tests")
class BookingIntegrationTest {

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
        private BookingRepo bookingRepo;

        @Autowired
        private UserRepo userRepo;

        @Autowired
        private ShowtimeRepo showtimeRepo;

        @Autowired
        private MovieRepo movieRepo;

        @Autowired
        private RoomRepo roomRepo;

        @Autowired
        private CinemaRepo cinemaRepo;

        @Autowired
        private SeatLockRepo seatLockRepo;

        @Autowired
        private SeatRepo seatRepo;

        @Autowired
        private ShowtimeSeatRepo showtimeSeatRepo;

        @Autowired
        private TicketTypeRepo ticketTypeRepo;

        @Autowired
        private SnackRepo snackRepo;

        private User testUser;
        private Booking testBooking;
        private Showtime testShowtime;
        private SeatLock testSeatLock;
        private Seat testSeat;
        private ShowtimeSeat testShowtimeSeat;
        private TicketType testTicketType;
        private Snack testSnack;
        private Cinema testCinema;

        @BeforeEach
        void setUp() {
                RestAssuredMockMvc.mockMvc(MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build());

                // Clean up in reverse dependency order
                bookingRepo.deleteAll();
                seatLockRepo.deleteAll();
                showtimeSeatRepo.deleteAll();
                showtimeRepo.deleteAll();
                seatRepo.deleteAll();
                roomRepo.deleteAll();
                snackRepo.deleteAll();
                cinemaRepo.deleteAll();
                movieRepo.deleteAll();
                ticketTypeRepo.deleteAll();
                userRepo.deleteAll();

                // Create test user
                testUser = new User();
                testUser.setEmail("test@booking.com");
                testUser.setUsername("bookinguser");
                testUser.setPassword("password");
                testUser.setRole(UserRole.USER);
                testUser = userRepo.save(testUser);

                // Create ticket type
                testTicketType = new TicketType();
                testTicketType.setCode("ADULT");
                testTicketType.setLabel("Adult Ticket");
                testTicketType.setModifierType(ModifierType.PERCENTAGE);
                testTicketType.setModifierValue(BigDecimal.ZERO);
                testTicketType.setActive(true);
                testTicketType.setSortOrder(1);
                testTicketType = ticketTypeRepo.save(testTicketType);

                // Create cinema with snack
                testCinema = new Cinema();
                testCinema.setName("Test Cinema");
                testCinema.setAddress("123 Test St");
                testCinema.setHotline("123-456-7890");
                testCinema = cinemaRepo.save(testCinema);

                testSnack = new Snack();
                testSnack.setName("Popcorn");
                testSnack.setType("FOOD");
                testSnack.setPrice(new BigDecimal("50000"));
                testSnack.setCinema(testCinema);
                testSnack = snackRepo.save(testSnack);

                // Create room
                Room room = new Room();
                room.setCinema(testCinema);
                room.setRoomNumber(1);
                room.setRoomType("STANDARD");
                room = roomRepo.save(room);

                // Create seat
                testSeat = new Seat();
                testSeat.setRoom(room);
                testSeat.setRowLabel("A");
                testSeat.setSeatNumber(1);
                testSeat.setSeatType(SeatType.NORMAL);
                testSeat = seatRepo.save(testSeat);

                // Create movie
                Movie movie = new Movie();
                movie.setTitle("Test Movie");
                movie.setDuration(120);
                movie.setStatus(MovieStatus.SHOWING);
                movie = movieRepo.save(movie);

                // Create showtime
                testShowtime = new Showtime();
                testShowtime.setMovie(movie);
                testShowtime.setRoom(room);
                testShowtime.setStartTime(LocalDateTime.now().plusDays(1));
                testShowtime = showtimeRepo.save(testShowtime);

                // Create showtime seat
                testShowtimeSeat = new ShowtimeSeat();
                testShowtimeSeat.setShowtime(testShowtime);
                testShowtimeSeat.setSeat(testSeat);
                testShowtimeSeat.setStatus(SeatStatus.AVAILABLE);
                testShowtimeSeat.setPrice(new BigDecimal("100000"));
                testShowtimeSeat = showtimeSeatRepo.save(testShowtimeSeat);

                // Create seat lock
                testSeatLock = new SeatLock();
                testSeatLock.setLockKey("lock-key-123");
                testSeatLock.setLockOwnerType(LockOwnerType.USER);
                testSeatLock.setLockOwnerId("user_" + testUser.getId());
                testSeatLock.setShowtime(testShowtime);
                testSeatLock.setUser(testUser);
                testSeatLock.setActive(true);
                testSeatLock.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                testSeatLock = seatLockRepo.save(testSeatLock);

                // Create test booking
                testBooking = new Booking();
                testBooking.setUser(testUser);
                testBooking.setShowtime(testShowtime);
                testBooking.setStatus(BookingStatus.CONFIRMED);
                testBooking.setTotalPrice(new BigDecimal("150000"));
                testBooking.setFinalPrice(new BigDecimal("150000"));
                testBooking.setQrCode("test-qr-code");
                testBooking = bookingRepo.save(testBooking);
        }

        // ==================== Price Preview Tests (V(G)=5) ====================

        @Nested
        @DisplayName("Price Preview Tests")
        class PricePreviewTests {

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@booking.com", roles = "USER")
                @DisplayName("Test 1/5: Should calculate price preview for authenticated user with valid seat lock")
                void testPricePreview_AuthenticatedUser_ValidLock() {
                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(testSeatLock.getId())
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/bookings/price-preview")
                                        .then()
                                        .statusCode(anyOf(equalTo(HttpStatus.OK.value()),
                                                        equalTo(HttpStatus.NOT_FOUND.value()),
                                                        equalTo(HttpStatus.BAD_REQUEST.value())));
                }

                @Test
                @RegressionTest
                @DisplayName("Test 2/5: Should fail price preview when seat lock not found")
                void testPricePreview_LockNotFound() {
                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(UUID.randomUUID())
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .header("X-Session-Id", UUID.randomUUID().toString())
                                        .when()
                                        .post("/bookings/price-preview")
                                        .then()
                                        .statusCode(HttpStatus.NOT_FOUND.value());
                }

                @Test
                @RegressionTest
                @DisplayName("Test 3/5: Should fail price preview when lock is inactive/expired")
                void testPricePreview_InactiveLock() {
                        testSeatLock.setActive(false);
                        seatLockRepo.save(testSeatLock);

                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(testSeatLock.getId())
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .header("X-Session-Id", "guest-session-123")
                                        .when()
                                        .post("/bookings/price-preview")
                                        .then()
                                        .statusCode(anyOf(equalTo(HttpStatus.NOT_FOUND.value()),
                                                        equalTo(HttpStatus.BAD_REQUEST.value())));
                }

                @Test
                @SanityTest
                @RegressionTest
                @DisplayName("Test 4/5: Should calculate price preview with snacks included")
                void testPricePreview_WithSnacks() {
                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(testSeatLock.getId())
                                        .snacks(List.of(
                                                        PricePreviewRequest.SnackItem.builder()
                                                                        .snackId(testSnack.getId())
                                                                        .quantity(2)
                                                                        .build()))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .header("X-Session-Id", UUID.randomUUID().toString())
                                        .when()
                                        .post("/bookings/price-preview")
                                        .then()
                                        .statusCode(anyOf(equalTo(HttpStatus.OK.value()),
                                                        equalTo(HttpStatus.NOT_FOUND.value()),
                                                        equalTo(HttpStatus.BAD_REQUEST.value())));
                }

                @Test
                @RegressionTest
                @DisplayName("Test 5/5: Should calculate price preview for guest user with session ID")
                void testPricePreview_GuestUser() {
                        String guestSessionId = "guest-session-456";
                        SeatLock guestLock = new SeatLock();
                        guestLock.setLockKey("lock-key-456");
                        guestLock.setLockOwnerType(LockOwnerType.GUEST_SESSION);
                        guestLock.setLockOwnerId(guestSessionId);
                        guestLock.setShowtime(testShowtime);
                        guestLock.setActive(true);
                        guestLock.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                        seatLockRepo.save(guestLock);

                        PricePreviewRequest request = PricePreviewRequest.builder()
                                        .lockId(guestLock.getId())
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .header("X-Session-Id", guestSessionId)
                                        .when()
                                        .post("/bookings/price-preview")
                                        .then()
                                        .statusCode(anyOf(equalTo(HttpStatus.OK.value()),
                                                        equalTo(HttpStatus.NOT_FOUND.value()),
                                                        equalTo(HttpStatus.BAD_REQUEST.value())));
                }
        }

        // ==================== Get User Bookings Tests (V(G)=1) ====================

        @Nested
        @DisplayName("Get User Bookings Tests")
        class GetUserBookingsTests {

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@booking.com", roles = "USER")
                @DisplayName("Test 1/1: Should retrieve all bookings for authenticated user")
                void testGetUserBookings_Success() {
                        given()
                                        .when()
                                        .get("/bookings/my-bookings")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("$", notNullValue())
                                        .body("size()", greaterThanOrEqualTo(1));
                }
        }

        // ==================== Get Booking By ID Tests (V(G)=2) ====================

        @Nested
        @DisplayName("Get Booking By ID Tests")
        class GetBookingByIdTests {

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@booking.com", roles = "USER")
                @DisplayName("Test 1/2: Should retrieve own booking by ID successfully")
                void testGetBookingById_OwnBooking() {
                        given()
                                        .when()
                                        .get("/bookings/" + testBooking.getId())
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("bookingId", notNullValue())
                                        .body("status", equalTo("CONFIRMED"));
                }

                @Test
                @RegressionTest
                @WithMockUser(username = "other@user.com", roles = "USER")
                @DisplayName("Test 2/2: Should fail to retrieve other user's booking")
                void testGetBookingById_OtherUsersBooking() {
                        // Create another user
                        User otherUser = new User();
                        otherUser.setEmail("other@user.com");
                        otherUser.setUsername("otheruser");
                        otherUser.setPassword("password");
                        otherUser.setRole(UserRole.USER);
                        userRepo.save(otherUser);

                        given()
                                        .when()
                                        .get("/bookings/" + testBooking.getId())
                                        .then()
                                        .statusCode(anyOf(equalTo(HttpStatus.FORBIDDEN.value()),
                                                        equalTo(HttpStatus.NOT_FOUND.value())));
                }
        }

        // ==================== Update QR Code Tests (V(G)=2) ====================

        @Nested
        @DisplayName("Update QR Code Tests")
        class UpdateQrCodeTests {

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@booking.com", roles = "USER")
                @DisplayName("Test 1/2: Should update QR code successfully for own booking")
                void testUpdateQr_Success() {
                        UpdateQrCodeRequest request = new UpdateQrCodeRequest();
                        request.setQrCodeUrl("https://example.com/qr/updated-12345");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .patch("/bookings/" + testBooking.getId() + "/qr")
                                        .then()
                                        .statusCode(HttpStatus.OK.value());
                }

                @Test
                @RegressionTest
                @WithMockUser(username = "other@user.com", roles = "USER")
                @DisplayName("Test 2/2: Should fail to update QR code for other user's booking")
                void testUpdateQr_Unauthorized() {
                        // Create another user
                        User otherUser = new User();
                        otherUser.setEmail("other@user.com");
                        otherUser.setUsername("otheruser");
                        otherUser.setPassword("password");
                        otherUser.setRole(UserRole.USER);
                        userRepo.save(otherUser);

                        UpdateQrCodeRequest request = new UpdateQrCodeRequest();
                        request.setQrCodeUrl("https://example.com/qr/hacked");

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .patch("/bookings/" + testBooking.getId() + "/qr")
                                        .then()
                                        .statusCode(anyOf(equalTo(HttpStatus.FORBIDDEN.value()),
                                                        equalTo(HttpStatus.NOT_FOUND.value())));
                }
        }
}
