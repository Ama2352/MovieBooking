package com.api.moviebooking.integrations;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.entities.RefreshToken;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.MembershipTierRepo;
import com.api.moviebooking.repositories.RefreshTokenRepo;
import com.api.moviebooking.repositories.UserRepo;
import com.api.moviebooking.services.JwtService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

/**
 * Integration tests for AuthController authentication endpoints.
 * Tests logout and token refresh functionality using actual API endpoints.
 * 
 * WHITE-BOX ANALYSIS:
 * - AuthController uses /auth prefix, NOT /users
 * - logout: POST /auth/logout (extracts refreshToken from cookie)
 * - logout-all: POST /auth/logout-all?email={email}
 * - refresh: GET /auth/refresh (extracts refreshToken from cookie, returns
 * access token in Set-Cookie header)
 * - All methods return void (200 OK) or String, exceptions caught by
 * GlobalExceptionHandler
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("User Authentication Integration Tests")
class UserIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"));

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private MembershipTierRepo membershipTierRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String validRefreshToken;
    private MembershipTier defaultTier;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build());

        // Clean up test data
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
        membershipTierRepo.deleteAll();

        // Create default membership tier
        defaultTier = new MembershipTier();
        defaultTier.setName("TestBronze");
        defaultTier.setMinPoints(0);
        defaultTier.setIsActive(true);
        defaultTier = membershipTierRepo.save(defaultTier);

        // Create test user
        testUser = new User();
        testUser.setEmail("testuser@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setPhoneNumber("0123456789");
        testUser.setRole(UserRole.USER);
        testUser.setLoyaltyPoints(0);
        testUser.setMembershipTier(defaultTier);
        testUser = userRepo.save(testUser);

        // Generate and persist a valid refresh token
        validRefreshToken = jwtService.generateRefreshToken(testUser.getEmail());
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(validRefreshToken);
        refreshToken.setUser(testUser);
        refreshTokenRepo.save(refreshToken);
    }

    // ========================================================================
    // Logout Tests
    // ========================================================================

    @Nested
    @DisplayName("Logout Operations")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully with valid refresh token cookie")
        @WithMockUser
        void testLogout_Success() {
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .post("/auth/logout")
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // Verify token is revoked
            RefreshToken token = refreshTokenRepo.findByToken(validRefreshToken).orElseThrow();
            assertNotNull(token.getRevokedAt(), "Token should be revoked");
        }

        @Test
        @DisplayName("Should return 200 with invalid refresh token (service catches exception)")
        @WithMockUser
        void testLogout_InvalidToken() {
            given()
                    .header("Cookie", "refreshToken=invalid.token")
                    .when()
                    .post("/auth/logout")
                    .then()
                    .statusCode(HttpStatus.OK.value()); // Service catches exception, returns void
        }

        @Test
        @DisplayName("Should fail logout without authentication")
        void testLogout_Unauthorized() {
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .post("/auth/logout")
                    .then()
                    .statusCode(HttpStatus.FORBIDDEN.value()); // Spring Security -> 403
        }

        @Test
        @DisplayName("Should handle missing refresh token cookie")
        @WithMockUser
        void testLogout_MissingCookie() {
            given()
                    .when()
                    .post("/auth/logout")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.OK.value()),
                            equalTo(HttpStatus.BAD_REQUEST.value())));
        }
    }

    // ========================================================================
    // Logout All Sessions Tests
    // ========================================================================

    @Nested
    @DisplayName("Logout All Sessions Operations")
    class LogoutAllTests {

        @Test
        @DisplayName("Should logout all sessions successfully")
        @WithMockUser
        void testLogoutAllSessions_Success() {
            given()
                    .param("email", testUser.getEmail())
                    .when()
                    .post("/auth/logout-all")
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // Verify all user's tokens are revoked
            RefreshToken token = refreshTokenRepo.findByToken(validRefreshToken).orElseThrow();
            assertNotNull(token.getRevokedAt(), "All tokens should be revoked");
        }

        @Test
        @DisplayName("Should return 200 with non-existent user (service catches exception)")
        @WithMockUser
        void testLogoutAllSessions_UserNotFound() {
            given()
                    .param("email", "nonexistent@example.com")
                    .when()
                    .post("/auth/logout-all")
                    .then()
                    .statusCode(HttpStatus.OK.value()); // Service catches exception
        }

        @Test
        @DisplayName("Should fail without authentication")
        void testLogoutAllSessions_Unauthorized() {
            given()
                    .param("email", testUser.getEmail())
                    .when()
                    .post("/auth/logout-all")
                    .then()
                    .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @DisplayName("Should require email parameter")
        @WithMockUser
        void testLogoutAllSessions_MissingParameter() {
            given()
                    .when()
                    .post("/auth/logout-all")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.BAD_REQUEST.value()), // Expected: required param missing
                            equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()))); // Spring error handling varies
        }
    }

    // ========================================================================
    // Refresh Token Tests
    // ========================================================================

    @Nested
    @DisplayName("Token Refresh Operations")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh access token successfully")
        @WithMockUser
        void testRefreshToken_Success() {
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("accessToken")); // Returns new access token in cookie
        }

        @Test
        @DisplayName("Should fail with invalid refresh token")
        @WithMockUser
        void testRefreshToken_InvalidToken() {
            given()
                    .header("Cookie", "refreshToken=invalid.token.here")
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        @DisplayName("Should fail with revoked refresh token")
        @WithMockUser
        void testRefreshToken_RevokedToken() {
            // Revoke the token
            jwtService.revokeRefreshToken(validRefreshToken);

            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.UNAUTHORIZED.value())));
        }

        @Test
        @DisplayName("Should fail without authentication")
        void testRefreshToken_Unauthorized() {
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @DisplayName("Should handle missing refresh token cookie")
        @WithMockUser
        void testRefreshToken_MissingCookie() {
            given()
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.UNAUTHORIZED.value())));
        }
    }

    // ========================================================================
    // Multiple User Scenarios
    // ========================================================================

    @Nested
    @DisplayName("Multi-User Scenarios")
    class MultiUserTests {

        @Test
        @DisplayName("Should handle logout for different users independently")
        @WithMockUser
        void testLogout_MultipleUsers() {
            // Create another user
            User user2 = new User();
            user2.setEmail("user2@example.com");
            user2.setUsername("user2");
            user2.setPassword(passwordEncoder.encode("password456"));
            user2.setPhoneNumber("0987654321");
            user2.setRole(UserRole.USER);
            user2.setLoyaltyPoints(0);
            user2.setMembershipTier(defaultTier);
            user2 = userRepo.save(user2);

            // Generate token for user2
            String user2Token = jwtService.generateRefreshToken(user2.getEmail());
            RefreshToken refreshToken2 = new RefreshToken();
            refreshToken2.setToken(user2Token);
            refreshToken2.setUser(user2);
            refreshTokenRepo.save(refreshToken2);

            // Logout user1
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .post("/auth/logout")
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // Verify user1's token is revoked but user2's is not
            RefreshToken user1Token = refreshTokenRepo.findByToken(validRefreshToken).orElseThrow();
            assertNotNull(user1Token.getRevokedAt());

            RefreshToken user2ActiveToken = refreshTokenRepo.findByToken(user2Token).orElseThrow();
            assertNull(user2ActiveToken.getRevokedAt());
        }

        @Test
        @DisplayName("Should refresh token multiple times")
        @WithMockUser
        void testRefreshToken_MultipleTimes() throws InterruptedException {
            // First refresh
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("accessToken"));

            Thread.sleep(1000);

            // Second refresh with same token (should still work)
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .header("Set-Cookie", containsString("accessToken"));
        }

        @Test
        @DisplayName("Should revoke all sessions and prevent refresh")
        @WithMockUser
        void testLogoutAllThenRefresh() {
            // Logout all sessions
            given()
                    .param("email", testUser.getEmail())
                    .when()
                    .post("/auth/logout-all")
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // Try to refresh with revoked token
            given()
                    .header("Cookie", "refreshToken=" + validRefreshToken)
                    .when()
                    .get("/auth/refresh")
                    .then()
                    .statusCode(anyOf(
                            equalTo(HttpStatus.BAD_REQUEST.value()),
                            equalTo(HttpStatus.UNAUTHORIZED.value())));
        }
    }
}
