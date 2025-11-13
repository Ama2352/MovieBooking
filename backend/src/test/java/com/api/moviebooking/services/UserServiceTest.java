package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.api.moviebooking.models.dtos.auth.LoginRequest;
import com.api.moviebooking.models.dtos.auth.RegisterRequest;
import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.UserRepo;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private MembershipTierService membershipTierService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private MembershipTier mockTier;
    private String testEmail;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testUserId = UUID.randomUUID();

        // Create mock membership tier
        mockTier = new MembershipTier();
        mockTier.setId(UUID.randomUUID());
        mockTier.setName("SILVER");
        mockTier.setMinPoints(0);
        mockTier.setIsActive(true);

        mockUser = new User();
        mockUser.setId(testUserId);
        mockUser.setEmail(testEmail);
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setRole(UserRole.USER);
        mockUser.setLoyaltyPoints(0);
        mockUser.setMembershipTier(mockTier);
        mockUser.setRefreshTokens(new HashSet<>());
    }

    @Nested
    @DisplayName("register() - V(G)=3, Min Tests=3")
    class RegisterTests {

        @Test
        @DisplayName("TC-1: Should successfully register new user with valid data")
        void testRegister_Success() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("newuser@example.com")
                    .username("newuser")
                    .password("password123")
                    .confirmPassword("password123")
                    .phoneNumber("1234567890")
                    .build();

            when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword123");
            when(membershipTierService.getDefaultTier()).thenReturn(mockTier);
            when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.register(request);

            verify(userRepo).existsByEmail(request.getEmail());
            verify(passwordEncoder).encode(request.getPassword());
            verify(membershipTierService).getDefaultTier();
            verify(userRepo).save(argThat(user -> user.getEmail().equals(request.getEmail()) &&
                    user.getUsername().equals(request.getUsername()) &&
                    user.getPassword().equals("encodedPassword123") &&
                    user.getPhoneNumber().equals(request.getPhoneNumber()) &&
                    user.getRole() == UserRole.USER &&
                    user.getLoyaltyPoints() == 0 &&
                    user.getMembershipTier() != null));
        }

        @Test
        @DisplayName("TC-2: Should throw exception when email already exists")
        void testRegister_EmailAlreadyExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email(testEmail)
                    .username("testuser")
                    .password("password123")
                    .confirmPassword("password123")
                    .phoneNumber("1234567890")
                    .build();

            when(userRepo.existsByEmail(testEmail)).thenReturn(true);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.register(request));
            assertEquals("Email already in use", exception.getMessage());
            verify(userRepo).existsByEmail(testEmail);
            verify(passwordEncoder, never()).encode(any());
            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("TC-3: Should throw exception when passwords don't match")
        void testRegister_PasswordsDoNotMatch() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("newuser@example.com")
                    .username("newuser")
                    .password("password123")
                    .confirmPassword("differentPassword")
                    .phoneNumber("1234567890")
                    .build();

            when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.register(request));
            assertEquals("Confirm passwords do not match", exception.getMessage());
            verify(userRepo).existsByEmail(request.getEmail());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login() - V(G)=2, Min Tests=2")
    class LoginTests {

        @Test
        @DisplayName("TC-1: Should return access and refresh tokens with valid credentials")
        void testLogin_Success() {
            LoginRequest request = LoginRequest.builder()
                    .email(testEmail)
                    .password("password123")
                    .build();

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(testEmail)
                    .password("encodedPassword")
                    .authorities("ROLE_USER")
                    .build();

            String accessToken = "access-token-123";
            String refreshToken = "refresh-token-456";

            when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtService.generateAccessToken(eq(testEmail), any())).thenReturn(accessToken);
            when(jwtService.generateRefreshToken(testEmail)).thenReturn(refreshToken);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));

            Map<String, String> result = userService.login(request);

            assertNotNull(result);
            assertEquals(accessToken, result.get("accessToken"));
            assertEquals(refreshToken, result.get("refreshToken"));
            verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService).generateAccessToken(eq(testEmail), any());
            verify(jwtService).generateRefreshToken(testEmail);
        }

        @Test
        @DisplayName("TC-2: Should throw exception when authentication fails")
        void testLogin_InvalidCredentials() {
            LoginRequest request = LoginRequest.builder()
                    .email(testEmail)
                    .password("wrongpassword")
                    .build();

            when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException(
                            "Invalid credentials"));

            org.springframework.security.authentication.BadCredentialsException exception = assertThrows(
                    org.springframework.security.authentication.BadCredentialsException.class,
                    () -> userService.login(request));
            assertEquals("Invalid credentials", exception.getMessage());
            verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService, never()).generateAccessToken(any(), any());
            verify(jwtService, never()).generateRefreshToken(any());
        }
    }

    @Nested
    @DisplayName("logout() - V(G)=2, Min Tests=2")
    class LogoutTests {

        @Test
        @DisplayName("TC-1: Should successfully revoke refresh token")
        void testLogout_Success() {
            String refreshToken = "refresh-token-to-revoke";
            doNothing().when(jwtService).revokeRefreshToken(refreshToken);

            userService.logout(refreshToken);

            verify(jwtService).revokeRefreshToken(refreshToken);
        }

        @Test
        @DisplayName("TC-2: Should throw exception when token revocation fails")
        void testLogout_TokenRevocationFails() {
            String refreshToken = "invalid-token";
            doThrow(new RuntimeException("Token not found"))
                    .when(jwtService).revokeRefreshToken(refreshToken);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.logout(refreshToken));
            assertEquals("Invalid refresh token", exception.getMessage());
            verify(jwtService).revokeRefreshToken(refreshToken);
        }
    }

    @Nested
    @DisplayName("logoutAllSessions() - V(G)=2, Min Tests=2")
    class LogoutAllSessionsTests {

        @Test
        @DisplayName("TC-1: Should successfully revoke all user refresh tokens")
        void testLogoutAllSessions_Success() {
            doNothing().when(jwtService).revokeAllUserRefreshTokens(testEmail);

            userService.logoutAllSessions(testEmail);

            verify(jwtService).revokeAllUserRefreshTokens(testEmail);
        }

        @Test
        @DisplayName("TC-2: Should throw exception when revocation fails")
        void testLogoutAllSessions_RevocationFails() {
            doThrow(new RuntimeException("Database error"))
                    .when(jwtService).revokeAllUserRefreshTokens(testEmail);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> userService.logoutAllSessions(testEmail));
            assertEquals("Error during logout from all sessions", exception.getMessage());
            verify(jwtService).revokeAllUserRefreshTokens(testEmail);
        }
    }

    @Nested
    @DisplayName("refreshAccessToken() - V(G)=2, Min Tests=2")
    class RefreshAccessTokenTests {

        @Test
        @DisplayName("TC-1: Should return new access token with valid refresh token")
        void testRefreshAccessToken_Success() {
            String refreshToken = "valid-refresh-token";
            String newAccessToken = "new-access-token-123";

            Collection<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(testEmail)
                    .password("encodedPassword")
                    .authorities(authorities)
                    .build();

            when(jwtService.validateRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractEmailFromToken(refreshToken)).thenReturn(testEmail);
            when(customUserDetailsService.loadUserByUsername(testEmail)).thenReturn(userDetails);
            when(jwtService.generateAccessToken(eq(testEmail), any())).thenReturn(newAccessToken);

            String result = userService.refreshAccessToken(refreshToken);

            assertNotNull(result);
            assertEquals(newAccessToken, result);
            verify(jwtService).validateRefreshToken(refreshToken);
            verify(jwtService).extractEmailFromToken(refreshToken);
            verify(customUserDetailsService).loadUserByUsername(testEmail);
            verify(jwtService).generateAccessToken(eq(testEmail), any());
        }

        @Test
        @DisplayName("TC-2: Should throw exception with invalid refresh token")
        void testRefreshAccessToken_InvalidToken() {
            String invalidToken = "invalid-refresh-token";
            when(jwtService.validateRefreshToken(invalidToken)).thenReturn(false);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.refreshAccessToken(invalidToken));
            assertEquals("Invalid refresh token", exception.getMessage());
            verify(jwtService).validateRefreshToken(invalidToken);
            verify(jwtService, never()).extractEmailFromToken(any());
            verify(customUserDetailsService, never()).loadUserByUsername(any());
            verify(jwtService, never()).generateAccessToken(any(), any());
        }
    }
}
