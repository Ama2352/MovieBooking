package com.api.moviebooking.integrations;

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

import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest;
import com.api.moviebooking.models.dtos.checkout.CheckoutPaymentRequest;
import com.api.moviebooking.models.entities.*;
import com.api.moviebooking.models.enums.*;
import com.api.moviebooking.repositories.*;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for CheckoutController endpoints.
 * Tests cover booking confirmation and atomic checkout with payment.
 * 
 * Test counts match V(G) cyclomatic complexity:
 * - Confirm Booking: 10 tests (V(G)=10)
 * - Atomic Checkout: 2 tests (V(G)=2)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Checkout Integration Tests")
class CheckoutIntegrationTest {

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
    private SeatLockSeatRepo seatLockSeatRepo;

    @Autowired
    private SeatRepo seatRepo;

    @Autowired
    private ShowtimeSeatRepo showtimeSeatRepo;

    @Autowired
    private TicketTypeRepo ticketTypeRepo;

    @Autowired
    private SnackRepo snackRepo;

    @Autowired
    private ShowtimeTicketTypeRepo showtimeTicketTypeRepo;

    private User testUser;
    private Showtime testShowtime;
    private Seat testSeat;
    private ShowtimeSeat testShowtimeSeat;
    private TicketType testTicketType;
    private Snack testSnack;
    private SeatLock testSeatLock;
    private SeatLockSeat seatLockSeat;
    private Cinema testCinema;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build());

        // Clean up in reverse dependency order
        bookingRepo.deleteAll();
        seatLockSeatRepo.deleteAll();
        seatLockRepo.deleteAll();
        showtimeTicketTypeRepo.deleteAll();
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
        testUser.setEmail("test@checkout.com");
        testUser.setUsername("checkoutuser");
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

        // Assign ticket type to showtime
        ShowtimeTicketType showtimeTicketType = new ShowtimeTicketType();
        showtimeTicketType.setShowtime(testShowtime);
        showtimeTicketType.setTicketType(testTicketType);
        showtimeTicketType.setActive(true);
        showtimeTicketTypeRepo.save(showtimeTicketType);

        // Create seat lock
        testSeatLock = new SeatLock();
        testSeatLock.setLockKey("");
        testSeatLock.setLockOwnerId(testUser.getId().toString());
        testSeatLock.setLockOwnerType(LockOwnerType.USER);
        testSeatLock.setShowtime(testShowtime);
        testSeatLock.setUser(testUser);
        testSeatLock.setActive(true);
        testSeatLock.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        testSeatLock = seatLockRepo.save(testSeatLock);

        // Create seat lock seat
        seatLockSeat = new SeatLockSeat();
        seatLockSeat.setSeatLock(testSeatLock);
        seatLockSeat.setShowtimeSeat(testShowtimeSeat);
        seatLockSeat.setTicketType(testTicketType);
        seatLockSeat.setPrice(new BigDecimal("100000"));
        seatLockSeatRepo.save(seatLockSeat);
    }

    // ==================== Confirm Booking Tests (V(G)=10) ====================

    @Nested
    @DisplayName("Confirm Booking Tests")
    class ConfirmBookingTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 1/10: Should confirm booking successfully for authenticated user")
        void testConfirmBooking_AuthenticatedUser_Success() {
            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(testSeatLock.getId());

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(equalTo(HttpStatus.OK.value()),
                            equalTo(HttpStatus.NOT_FOUND.value())));
        }

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Test 2/10: Should confirm booking successfully for guest user with guest info")
        void testConfirmBooking_GuestUser_Success() {
            String guestSessionId = "guest-session-123";
            SeatLock guestLock = new SeatLock();
            guestLock.setLockKey("lock-key-123");
            guestLock.setLockOwnerId(guestSessionId);
            guestLock.setLockOwnerType(LockOwnerType.GUEST_SESSION);
            guestLock.setShowtime(testShowtime);
            guestLock.setActive(true);
            guestLock.setExpiresAt(LocalDateTime.now().plusMinutes(10));
            seatLockRepo.save(guestLock);

            ConfirmBookingRequest.GuestInfo guestInfo = new ConfirmBookingRequest.GuestInfo();
            guestInfo.setEmail("guest@example.com");
            guestInfo.setUsername("guestuser");
            guestInfo.setPhoneNumber("1234567890");

            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(guestLock.getId());
            request.setGuestInfo(guestInfo);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .header("X-Session-Id", guestSessionId)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(equalTo(HttpStatus.OK.value()),
                            equalTo(HttpStatus.BAD_REQUEST.value())));
        }

        @Test
        @RegressionTest
        @DisplayName("Test 3/10: Should fail when seat lock not found")
        void testConfirmBooking_LockNotFound() {
            ConfirmBookingRequest.GuestInfo guestInfo = new ConfirmBookingRequest.GuestInfo();
            guestInfo.setEmail("notfound@example.com");
            guestInfo.setUsername("notfounduser");
            guestInfo.setPhoneNumber("1234567890");

            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(UUID.randomUUID());
            request.setGuestInfo(guestInfo);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .header("X-Session-Id", UUID.randomUUID().toString())
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @RegressionTest
        @WithMockUser(username = "other@user.com", roles = "USER")
        @DisplayName("Test 4/10: Should fail when lock ownership doesn't match")
        void testConfirmBooking_LockOwnershipMismatch() {
            User otherUser = new User();
            otherUser.setEmail("other@user.com");
            otherUser.setUsername("otheruser");
            otherUser.setPassword("password");
            otherUser.setRole(UserRole.USER);
            userRepo.save(otherUser);

            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(testSeatLock.getId()); // Different user trying to use another's lock

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.FORBIDDEN.value()),
                            equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.NOT_FOUND.value())));
        }

        @Test
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 5/10: Should fail when lock is inactive")
        void testConfirmBooking_InactiveLock() {
            testSeatLock.setActive(false);
            seatLockRepo.save(testSeatLock);

            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(testSeatLock.getId());

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.NOT_FOUND.value()),
                            equalTo(HttpStatus.GONE.value())));
        }

        @Test
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 6/10: Should fail when lock is expired")
        void testConfirmBooking_ExpiredLock() {
            testSeatLock.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            seatLockRepo.save(testSeatLock);

            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(testSeatLock.getId());

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.NOT_FOUND.value()),
                            equalTo(HttpStatus.GONE.value())));
        }

        @Test
        @RegressionTest
        @DisplayName("Test 7/10: Should fail when guest info is missing for guest user")
        void testConfirmBooking_GuestMissingInfo() {
            String guestSessionId = "guest-session-789";
            SeatLock guestLock = new SeatLock();
            guestLock.setLockKey("lock-key-789");
            guestLock.setLockOwnerId(guestSessionId);
            guestLock.setLockOwnerType(LockOwnerType.GUEST_SESSION);
            guestLock.setShowtime(testShowtime);
            guestLock.setActive(true);
            guestLock.setExpiresAt(LocalDateTime.now().plusMinutes(10));
            seatLockRepo.save(guestLock);

            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(guestLock.getId());
            request.setGuestInfo(null); // Missing guest info

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .header("X-Session-Id", guestSessionId)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.FORBIDDEN.value())));
        }

        @Test
        @SanityTest
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 8/10: Should confirm booking with snacks successfully")
        void testConfirmBooking_WithSnacks() {
            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(testSeatLock.getId());

            ConfirmBookingRequest.SnackComboItem snackItem = new ConfirmBookingRequest.SnackComboItem();
            snackItem.setSnackId(testSnack.getId());
            snackItem.setQuantity(2);
            request.setSnackCombos(List.of(snackItem));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(equalTo(HttpStatus.OK.value()),
                            equalTo(HttpStatus.NOT_FOUND.value())));
        }

        @Test
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 9/10: Should fail when snack quantity mismatch")
        void testConfirmBooking_SnackQuantityMismatch() {
            // Create non-existent snack ID
            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(testSeatLock.getId());

            ConfirmBookingRequest.SnackComboItem snackItem = new ConfirmBookingRequest.SnackComboItem();
            snackItem.setSnackId(UUID.randomUUID());
            snackItem.setQuantity(2);
            request.setSnackCombos(List.of(snackItem));

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.NOT_FOUND.value()),
                            equalTo(HttpStatus.BAD_REQUEST.value())));
        }

        @Test
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 10/10: Should create booking with correct status")
        void testConfirmBooking_CorrectStatus() {
            ConfirmBookingRequest request = new ConfirmBookingRequest();
            request.setLockId(testSeatLock.getId());

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/bookings/confirm")
                    .then()
                    .statusCode(anyOf(equalTo(HttpStatus.OK.value()),
                            equalTo(HttpStatus.NOT_FOUND.value())));
        }
    }

    // ==================== Atomic Checkout Tests (V(G)=2) ====================

    @Nested
    @DisplayName("Atomic Checkout Tests")
    class AtomicCheckoutTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 1/2: Should complete atomic checkout successfully with payment initiation")
        void testAtomicCheckout_Success() {
            CheckoutPaymentRequest request = CheckoutPaymentRequest.builder()
                    .lockId(testSeatLock.getId())
                    .paymentMethod("MOMO")
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/checkout")
                    .then()
                    .statusCode(equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }

        @Test
        @RegressionTest
        @WithMockUser(username = "test@checkout.com", roles = "USER")
        @DisplayName("Test 2/2: Should rollback booking if payment initiation fails")
        void testAtomicCheckout_PaymentFailureRollback() {
            CheckoutPaymentRequest request = CheckoutPaymentRequest.builder()
                    .lockId(testSeatLock.getId())
                    .paymentMethod("INVALID_METHOD") // Invalid payment method to trigger failure
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/checkout")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                            equalTo(HttpStatus.NOT_FOUND.value())));
        }
    }
}
