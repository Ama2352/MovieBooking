package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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

import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.ConfirmPaymentRequest;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.MembershipTier;
import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.PaymentRepo;
import com.api.moviebooking.repositories.UserRepo;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for Payment API endpoints using Testcontainers and
 * RestAssured.
 * Tests payment validation, database operations, and business logic with real
 * dependencies.
 * 
 * NOTE: External payment gateways (PayPal/momo) require sandbox credentials
 * and manual testing.
 * See PAYMENT_TESTING_GUIDE.md for complete end-to-end testing instructions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Payment Integration Tests")
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

        private User testUser;
        private Booking testBooking;

        @BeforeEach
        void setUp() {
                RestAssuredMockMvc.mockMvc(MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build());

                // Clean up test data
                paymentRepo.deleteAll();
                bookingRepo.deleteAll();
                userRepo.deleteAll();

                // Create test user
                testUser = new User();
                testUser.setEmail("test@payment.com");
                testUser.setUsername("paymentuser");
                testUser.setPassword("password");
                testUser.setRole(UserRole.USER);
                // testUser.setMembershipTier(MembershipTier.BRONZE);
                testUser = userRepo.save(testUser);

                // Create test booking
                testBooking = new Booking();
                testBooking.setUser(testUser);
                testBooking.setStatus(BookingStatus.CONFIRMED);
                testBooking.setTotalPrice(new BigDecimal("100.00"));
                testBooking = bookingRepo.save(testBooking);
        }

        // ========================================================================
        // momo Payment Flow Tests
        // ========================================================================

        @Nested
        @DisplayName("momo Payment Flow")
        class momoPaymentFlowTests {

                @Test
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should successfully initiate momo payment and create PENDING record")
                void testInitiatemomoPayment() {
                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("momo")
                                        .amount(new BigDecimal("100.00"))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("txnRef", notNullValue())
                                        .body("paymentUrl", notNullValue())
                                        .body("paymentUrl", containsString("test-payment.momo.vn"));

                        // Verify payment was created in database
                        Optional<Payment> savedPayment = paymentRepo.findByBookingIdAndMethodAndStatus(
                                        testBooking.getId(), PaymentMethod.MOMO, PaymentStatus.PENDING);
                        assertTrue(savedPayment.isPresent(), "Payment should be created with PENDING status");
                        assertEquals(PaymentStatus.PENDING, savedPayment.get().getStatus());
                        assertEquals(PaymentMethod.MOMO, savedPayment.get().getMethod());
                        assertEquals(new BigDecimal("100.00"), savedPayment.get().getAmount());
                }

                @Test
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should successfully verify completed momo payment")
                void testVerifymomoPayment() {
                        // Create completed payment (simulating IPN callback already processed)
                        Payment payment = new Payment();
                        payment.setBooking(testBooking);
                        payment.setMethod(PaymentMethod.MOMO);
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setAmount(new BigDecimal("100.00"));
                        payment.setCurrency("VND");
                        payment.setCompletedAt(LocalDateTime.now());
                        payment = paymentRepo.save(payment);
                        // Set transactionId to paymentId (as momoService does)
                        payment.setTransactionId(payment.getId().toString());
                        payment = paymentRepo.save(payment);

                        // Prepare request
                        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                                        .paymentMethod("momo")
                                        .transactionId(payment.getId().toString())
                                        .build();

                        // Execute and verify
                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order/capture")
                                        .then()
                                        .statusCode(HttpStatus.OK.value())
                                        .body("status", equalTo("SUCCESS"))
                                        .body("method", equalTo("momo"));

                        // Verify transactionId in DB matches paymentId
                        Payment dbPayment = paymentRepo.findById(payment.getId()).orElseThrow();
                        assertEquals(payment.getId().toString(), dbPayment.getTransactionId(),
                                        "transactionId should match payment id");
                }

                @Test
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should return payment not found for invalid transaction ID")
                void testVerifymomoPaymentNotFound() {
                        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                                        .paymentMethod("momo")
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

        // ========================================================================
        // Validation Tests
        // ========================================================================

        @Nested
        @DisplayName("Payment Validation Tests")
        class PaymentValidationTests {

                @Test
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should reject payment for non-confirmed booking")
                void testRejectPaymentForUnconfirmedBooking() {
                        testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
                        bookingRepo.save(testBooking);

                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("momo")
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
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should reject payment with mismatched amount")
                void testRejectPaymentWithMismatchedAmount() {
                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("momo")
                                        .amount(new BigDecimal("50.00")) // Wrong amount
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
                @DisplayName("Should reject payment without authentication")
                void testRejectPaymentWithoutAuth() {
                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(testBooking.getId())
                                        .paymentMethod("momo")
                                        .amount(new BigDecimal("100.00"))
                                        .build();

                        given()
                                        .contentType(ContentType.JSON)
                                        .body(request)
                                        .when()
                                        .post("/payments/order")
                                        .then()
                                        .statusCode(HttpStatus.FORBIDDEN.value());
                }

                @Test
                @WithMockUser(username = "test@payment.com", roles = "USER")
                @DisplayName("Should reject payment with null booking ID")
                void testRejectPaymentWithNullBookingId() {
                        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                                        .bookingId(null)
                                        .paymentMethod("momo")
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

        // ========================================================================
        // Search Tests
        // ========================================================================

        @Nested
        @DisplayName("Payment Search Tests")
        class PaymentSearchTests {

                @Test
                @WithMockUser(username = "test@payment.com", roles = "ADMIN")
                @DisplayName("Should search payments by booking ID successfully")
                void testSearchPaymentsByBookingId() {
                        // Create test payment
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
                @WithMockUser(username = "test@payment.com", roles = "ADMIN")
                @DisplayName("Should search payments by status successfully")
                void testSearchPaymentsByStatus() {
                        // Create test payments with different statuses
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
                @DisplayName("Should reject search without authentication")
                void testRejectSearchWithoutAuth() {
                        given()
                                        .when()
                                        .get("/payments/search")
                                        .then()
                                        .statusCode(HttpStatus.FORBIDDEN.value());
                }
        }

        // ========================================================================
        // Database State Tests
        // ========================================================================

        @Nested
        @DisplayName("Payment Database State Tests")
        class PaymentDatabaseStateTests {

                @Test
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

                        // Update to completed
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setCompletedAt(LocalDateTime.now());
                        Payment updated = paymentRepo.save(payment);

                        assertEquals(PaymentStatus.SUCCESS, updated.getStatus());
                        assertNotNull(updated.getCompletedAt());
                }

                @Test
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

                        // Simulate payment failure
                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setErrorMessage("Payment declined");
                        paymentRepo.save(payment);

                        testBooking.setStatus(BookingStatus.CANCELLED);
                        bookingRepo.save(testBooking);

                        // Verify
                        Payment updatedPayment = paymentRepo.findById(payment.getId()).orElseThrow();
                        Booking updatedBooking = bookingRepo.findById(testBooking.getId()).orElseThrow();

                        assertEquals(PaymentStatus.FAILED, updatedPayment.getStatus());
                        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());
                }
        }
}
