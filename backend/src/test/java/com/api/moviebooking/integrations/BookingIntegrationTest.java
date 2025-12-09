package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
import com.api.moviebooking.models.enums.DiscountType;
import com.api.moviebooking.models.enums.ModifierType;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.models.enums.SeatStatus;
import com.api.moviebooking.models.enums.SeatType;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.*;
import com.api.moviebooking.services.BookingService;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Booking Integration Tests")
@RegressionTest
@SmokeTest
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
        private MembershipTierRepo membershipTierRepo;

        @Autowired
        private TicketTypeRepo ticketTypeRepo;

        @Autowired
        private ShowtimeTicketTypeRepo showtimeTicketTypeRepo;

        @Autowired
        private BookingService bookingService;

        @Autowired
        private PriceBaseRepo priceBaseRepo;

        private User testUser1, testUser2;
        private Showtime testShowtime;
        private ShowtimeSeat seat1, seat2, seat3;
        private MembershipTier defaultTier;
        private TicketType standardTicket;

        @BeforeEach
        void setUp() {
                RestAssuredMockMvc.mockMvc(MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build());

                bookingRepo.deleteAll();
                seatLockRepo.deleteAll();
                showtimeSeatRepo.deleteAll();
                showtimeTicketTypeRepo.deleteAll(); // Clean up this too!
                seatRepo.deleteAll();
                showtimeRepo.deleteAll();
                roomRepo.deleteAll();
                movieRepo.deleteAll();
                cinemaRepo.deleteAll();
                userRepo.deleteAll();
                membershipTierRepo.deleteAll();
                userRepo.deleteAll();
                membershipTierRepo.deleteAll();
                ticketTypeRepo.deleteAll();
                priceBaseRepo.deleteAll();

                defaultTier = new com.api.moviebooking.models.entities.MembershipTier();
                defaultTier.setName("Bronze");
                defaultTier.setMinPoints(0);
                defaultTier.setDiscountType(DiscountType.PERCENTAGE);
                defaultTier.setDiscountValue(BigDecimal.ZERO);
                defaultTier = membershipTierRepo.save(defaultTier);

                testUser1 = new User();
                testUser1.setEmail("user1@test.com");
                testUser1.setUsername("user1");
                testUser1.setRole(UserRole.USER);
                testUser1.setMembershipTier(defaultTier);
                testUser1 = userRepo.save(testUser1);

                testUser2 = new User();
                testUser2.setEmail("user2@test.com");
                testUser2.setUsername("user2");
                testUser2.setRole(UserRole.USER);
                testUser2.setMembershipTier(defaultTier);
                testUser2 = userRepo.save(testUser2);

                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("123 Test St");
                cinema.setHotline("1234567");
                cinema = cinemaRepo.save(cinema);

                Room room = new Room();
                room.setCinema(cinema);
                room.setRoomNumber(1);
                room.setRoomType("IMAX");
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
                testShowtime.setFormat("2D Subtitled");
                testShowtime = showtimeRepo.save(testShowtime);

                standardTicket = new TicketType();
                standardTicket.setCode("ADULT");
                standardTicket.setLabel("Adult Standard");
                standardTicket.setModifierType(ModifierType.FIXED_AMOUNT);
                standardTicket.setModifierValue(BigDecimal.ZERO);
                standardTicket = ticketTypeRepo.save(standardTicket);

                // Link TicketType to Showtime
                ShowtimeTicketType stt = new ShowtimeTicketType();
                stt.setShowtime(testShowtime);
                stt.setTicketType(standardTicket);
                stt.setActive(true);
                stt.setActive(true);
                showtimeTicketTypeRepo.save(stt);

                PriceBase priceBase = new PriceBase();
                priceBase.setName("Standard Base Price");
                priceBase.setBasePrice(new BigDecimal("10.00"));
                priceBase.setIsActive(true);
                priceBaseRepo.save(priceBase);

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

        private LockSeatsRequest createLockRequest(UUID userId, UUID... seatIds) {
                List<LockSeatsRequest.SeatWithTicketType> seats = Arrays.stream(seatIds)
                                .map(id -> new LockSeatsRequest.SeatWithTicketType(id, standardTicket.getId()))
                                .toList();
                return new LockSeatsRequest(testShowtime.getId(), seats);
        }

        @Test
        @WithMockUser(username = "user1@test.com", roles = "USER")
        @DisplayName("Should successfully lock seats when authenticated")
        void testLockSeats_Success() {
                LockSeatsRequest request = createLockRequest(testUser1.getId(), seat1.getId(), seat2.getId());

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/seat-locks")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .body("lockId", notNullValue())
                                .body("lockedSeats", hasSize(2))
                                .body("totalPrice", equalTo(20));

                ShowtimeSeat updatedSeat1 = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.LOCKED, updatedSeat1.getStatus());
        }

        @Test
        @WithMockUser(username = "user1@test.com", roles = "USER")
        @DisplayName("Should confirm booking successfully")
        void testConfirmBooking_Success() {
                LockSeatsRequest lockRequest = createLockRequest(testUser1.getId(), seat1.getId(), seat2.getId());

                String lockId = given()
                                .contentType(ContentType.JSON)
                                .body(lockRequest)
                                .when()
                                .post("/seat-locks")
                                .then()
                                .statusCode(HttpStatus.CREATED.value())
                                .extract().path("lockId");

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
                                .body("status", equalTo("PENDING_PAYMENT"))
                                .body("totalPrice", equalTo(20.00f));

                ShowtimeSeat bookedSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.BOOKED, bookedSeat.getStatus());
        }

        @Test
        @WithMockUser(username = "user1@test.com", roles = "USER")
        @DisplayName("Should check seat availability successfully")
        void testCheckAvailability_Success() {
                given()
                                .when()
                                .get("/seat-locks/availability/showtime/" + testShowtime.getId())
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("availableSeats", hasSize(3))
                                .body("lockedSeats", empty())
                                .body("bookedSeats", empty());
        }

        @Test
        @WithMockUser(username = "user1@test.com", roles = "USER")
        @DisplayName("Should release seats successfully")
        void testReleaseSeats_Success() {
                LockSeatsRequest lockRequest = createLockRequest(testUser1.getId(), seat1.getId());

                given()
                                .contentType(ContentType.JSON)
                                .body(lockRequest)
                                .when()
                                .post("/seat-locks")
                                .then()
                                .statusCode(HttpStatus.CREATED.value());

                ShowtimeSeat lockedSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.LOCKED, lockedSeat.getStatus());

                given()
                                .when()
                                .delete("/seat-locks/showtime/" + testShowtime.getId())
                                .then()
                                .statusCode(HttpStatus.OK.value());

                ShowtimeSeat availableSeat = showtimeSeatRepo.findById(seat1.getId()).orElseThrow();
                assertEquals(SeatStatus.AVAILABLE, availableSeat.getStatus());
        }

        @Test
        @WithMockUser(username = "user1@test.com", roles = "USER")
        @DisplayName("Should get user's booking history")
        void testGetMyBookings_Success() {
                given()
                                .when()
                                .get("/bookings/my-bookings")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("$", notNullValue());
        }
}
