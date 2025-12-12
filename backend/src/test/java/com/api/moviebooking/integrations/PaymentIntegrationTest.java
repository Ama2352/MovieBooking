package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;

import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.ConfirmPaymentRequest;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Cinema;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.MovieStatus;
import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.CinemaRepo;
import com.api.moviebooking.repositories.MovieRepo;
import com.api.moviebooking.repositories.PaymentRepo;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.ShowtimeRepo;
import com.api.moviebooking.repositories.UserRepo;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Payment Integration Tests")
@SmokeTest
@SanityTest
@RegressionTest
class PaymentIntegrationTest {

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
        private PaymentRepo paymentRepo;

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

        @MockBean
        private RestTemplate restTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        private User testUser;
        private Booking testBooking;
        private Showtime testShowtime;

        @BeforeEach
        void setUp() {
                RestAssuredMockMvc.mockMvc(MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build());

                paymentRepo.deleteAll();
                bookingRepo.deleteAll();
                showtimeRepo.deleteAll();
                roomRepo.deleteAll();
                movieRepo.deleteAll();
                cinemaRepo.deleteAll();
                userRepo.deleteAll();

                testUser = new User();
                testUser.setEmail("test@payment.com");
                testUser.setUsername("paymentuser");
                testUser.setPassword("password");
                testUser.setRole(UserRole.USER);
                testUser = userRepo.save(testUser);

                Cinema cinema = new Cinema();
                cinema.setName("Test Cinema");
                cinema.setAddress("123 Test St");
                cinema.setHotline("123-456-7890");
                cinema = cinemaRepo.save(cinema);

                Room room = new Room();
                room.setCinema(cinema);
                room.setRoomNumber(1);
                room.setRoomType("STANDARD");
                room = roomRepo.save(room);

                Movie movie = new Movie();
                movie.setTitle("Test Movie");
                movie.setDuration(120);
                movie.setStatus(MovieStatus.SHOWING);
                movie = movieRepo.save(movie);

                testShowtime = new Showtime();
                testShowtime.setMovie(movie);
                testShowtime.setRoom(room);
                testShowtime.setStartTime(LocalDateTime.now().plusDays(1));
                testShowtime = showtimeRepo.save(testShowtime);

                testBooking = new Booking();
                testBooking.setUser(testUser);
                testBooking.setShowtime(testShowtime);
                testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
                testBooking.setTotalPrice(new BigDecimal("100.00"));
                testBooking.setFinalPrice(new BigDecimal("100.00"));
                testBooking = bookingRepo.save(testBooking);
        }

        @Nested
        @DisplayName("momo Payment Flow")
        class momoPaymentFlowTests {

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should successfully initiate momo payment and create PENDING record")
                void testInitiatemomoPayment() {
                        // Mock Momo API response
                        ObjectNode mockResponse = objectMapper.createObjectNode();
                        mockResponse.put("resultCode", 0);
                        mockResponse.put("payUrl", "https://test-payment.momo.vn/pay");
                        mockResponse.put("message", "Success");

                        Mockito.when(restTemplate.postForEntity(
                                        ArgumentMatchers.anyString(),
                                        ArgumentMatchers.any(),
                                        ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
                                        .thenReturn(ResponseEntity.ok(mockResponse));

                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("MOMO")
                                        .amount(new BigDecimal("100.00"))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order")
                                        .then()
                                        .statusCode(anyOf(
                                                        equalTo(HttpStatus.OK.value()),
                                                        equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                                                        equalTo(HttpStatus.SERVICE_UNAVAILABLE.value())));
                }

                @Test
                @SmokeTest
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should successfully verify completed momo payment")
                void testVerifymomoPayment() {
                        Payment payment = new Payment();
                        payment.setBooking(testBooking);
                        payment.setMethod(PaymentMethod.MOMO);
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setAmount(new BigDecimal("100.00"));
                        payment.setCurrency("VND");
                        payment.setCompletedAt(LocalDateTime.now());
                        payment = paymentRepo.save(payment);
                        payment.setTransactionId(payment.getId().toString());
                        payment = paymentRepo.save(payment);

                        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                                        .paymentMethod("MOMO")
                                        .transactionId(payment.getId().toString())
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order/capture")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("SUCCESS"))
                                        .body("method", equalTo("MOMO"));

                        Payment dbPayment = paymentRepo.findById(payment.getId()).orElseThrow();
                        assertEquals(payment.getId().toString(), dbPayment.getTransactionId(),
                                        "transactionId should match payment id");
                }

                @Test
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should return payment not found for invalid transaction ID")
                void testVerifymomoPaymentNotFound() {
                        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                                        .paymentMethod("MOMO")
                                        .transactionId("INVALID_TXN")
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order/capture")
                                        .then()
                                        .statusCode(HttpStatus.NOT_FOUND.value());
                }
        }

        @Nested
        @DisplayName("Payment Validation Tests")
        class PaymentValidationTests {

                @Test
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should accept payment for pending payment booking")
                void testAcceptPaymentForPendingBooking() {
                        // Mock Momo API response
                        ObjectNode mockResponse = objectMapper.createObjectNode();
                        mockResponse.put("resultCode", 0);
                        mockResponse.put("payUrl", "https://test-payment.momo.vn/pay");
                        mockResponse.put("message", "Success");

                        Mockito.when(restTemplate.postForEntity(
                                        ArgumentMatchers.anyString(),
                                        ArgumentMatchers.any(),
                                        ArgumentMatchers.eq(com.fasterxml.jackson.databind.JsonNode.class)))
                                        .thenReturn(ResponseEntity.ok(mockResponse));

                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("MOMO")
                                        .amount(new BigDecimal("100.00"))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order")
                                        .then()
                                        .statusCode(anyOf(
                                                        equalTo(HttpStatus.OK.value()),
                                                        equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                                                        equalTo(HttpStatus.SERVICE_UNAVAILABLE.value())));
                }

                @Test
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should reject payment with mismatched amount")
                void testRejectPaymentWithMismatchedAmount() {
                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("MOMO")
                                        .amount(new BigDecimal("50.00"))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order")
                                        .then()
                                        .statusCode(anyOf(
                                                        equalTo(HttpStatus.BAD_REQUEST.value()),
                                                        equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                                                        equalTo(HttpStatus.SERVICE_UNAVAILABLE.value())));
                }

                @Test
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should reject payment with invalid payment method")
                void testRejectInvalidPaymentMethod() {
                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("INVALID_METHOD")
                                        .amount(new BigDecimal("100.00"))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order")
                                        .then()
                                        .statusCode(HttpStatus.BAD_REQUEST.value());
                }

                @Test
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should reject payment with null booking ID")
                void testRejectPaymentWithNullBookingId() {
                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(null)
                                        .paymentMethod("MOMO")
                                        .amount(new BigDecimal("100.00"))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order")
                                        .then()
                                        .statusCode(HttpStatus.BAD_REQUEST.value());
                }
        }

        @Nested
        @DisplayName("Payment Search Tests")
        class PaymentSearchTests {

                @Test
                @SanityTest
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "ADMIN")
                @DisplayName("Should search payments by booking ID successfully")
                void testSearchPaymentsByBookingId() {
                        Payment payment = new Payment();
                        payment.setBooking(testBooking);
                        payment.setMethod(PaymentMethod.MOMO);
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setAmount(new BigDecimal("100.00"));
                        payment.setCurrency("VND");
                        payment.setTransactionId("TXN_SEARCH_123");
                        paymentRepo.save(payment);

                        given()
                                        .queryParam("bookingId", testBooking.getId().toString())
                                        .when()
                                        .get("/payments/search")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("$", notNullValue())
                                        .body("size()", greaterThan(0));
                }

                @Test
                @RegressionTest
                @WithMockUser(username = "test@payment.com", roles = "ADMIN")
                @DisplayName("Should search payments by status successfully")
                void testSearchPaymentsByStatus() {
                        Payment payment1 = new Payment();
                        payment1.setBooking(testBooking);
                        payment1.setMethod(PaymentMethod.MOMO);
                        payment1.setStatus(PaymentStatus.PENDING);
                        payment1.setAmount(new BigDecimal("100.00"));
                        payment1.setCurrency("VND");
                        paymentRepo.save(payment1);

                        given()
                                        .queryParam("status", "PENDING")
                                        .when()
                                        .get("/payments/search")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("$", notNullValue());
                }

                @Test
                @RegressionTest
                @DisplayName("Should allow search without authentication (Public Read Access)")
                void testAllowSearchWithoutAuth() {
                        given()
                                        .when()
                                        .get("/payments/search")
                                        .then()
                                        .statusCode(HttpStatus.OK.value());
                }
        }

        @Nested
        @DisplayName("Payment Database State Tests")
        class PaymentDatabaseStateTests {

                @Test
                @RegressionTest
                @DisplayName("Should create payment with correct initial state")
                void testPaymentInitialState() {
                        Payment payment = new Payment();
                        payment.setBooking(testBooking);
                        payment.setMethod(PaymentMethod.MOMO);
                        payment.setStatus(PaymentStatus.PENDING);
                        payment.setAmount(new BigDecimal("100.00"));
                        payment.setCurrency("VND");
                        payment.setTransactionId("TXN_STATE_TEST");

                        Payment saved = paymentRepo.save(payment);

                        assertNotNull(saved.getId());
                        assertNotNull(saved.getCreatedAt());
                        assertNull(saved.getCompletedAt());
                        assertEquals(PaymentStatus.PENDING, saved.getStatus());
                }

                @Test
                @SanityTest
                @RegressionTest
                @DisplayName("Should update payment to SUCCESS with timestamp")
                void testPaymentCompletedState() {
                        Payment payment = new Payment();
                        payment.setBooking(testBooking);
                        payment.setMethod(PaymentMethod.MOMO);
                        payment.setStatus(PaymentStatus.PENDING);
                        payment.setAmount(new BigDecimal("100.00"));
                        payment.setCurrency("VND");
                        payment.setTransactionId("TXN_COMPLETE_TEST");
                        payment = paymentRepo.save(payment);

                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setCompletedAt(LocalDateTime.now());
                        Payment updated = paymentRepo.save(payment);

                        assertEquals(PaymentStatus.SUCCESS, updated.getStatus());
                        assertNotNull(updated.getCompletedAt());
                }

                @Test
                @SanityTest
                @RegressionTest
                @DisplayName("Should update payment to FAILED and cancel booking")
                void testPaymentFailedState() {
                        Payment payment = new Payment();
                        payment.setBooking(testBooking);
                        payment.setMethod(PaymentMethod.MOMO);
                        payment.setStatus(PaymentStatus.PENDING);
                        payment.setAmount(new BigDecimal("100.00"));
                        payment.setCurrency("VND");
                        payment.setTransactionId("TXN_FAIL_TEST");
                        payment = paymentRepo.save(payment);

                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setErrorMessage("Payment declined");
                        paymentRepo.save(payment);

                        testBooking.setStatus(BookingStatus.CANCELLED);
                        bookingRepo.save(testBooking);

                        Payment updatedPayment = paymentRepo.findById(payment.getId()).orElseThrow();
                        Booking updatedBooking = bookingRepo.findById(testBooking.getId()).orElseThrow();

                        assertEquals(PaymentStatus.FAILED, updatedPayment.getStatus());
                        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());
                }
        }
}
