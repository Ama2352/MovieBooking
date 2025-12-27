package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.api.moviebooking.helpers.exceptions.EntityDeletionForbiddenException;
import com.api.moviebooking.helpers.mapstructs.UserMapper;
import com.api.moviebooking.models.dtos.auth.LoginRequest;
import com.api.moviebooking.models.dtos.auth.LoginResponse;
import com.api.moviebooking.models.dtos.auth.RegisterRequest;
import com.api.moviebooking.models.dtos.user.UpdatePasswordRequest;
import com.api.moviebooking.models.dtos.user.UpdateProfileRequest;
import com.api.moviebooking.models.dtos.user.UserProfileResponse;
import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.RefreshTokenRepo;
import com.api.moviebooking.repositories.UserRepo;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

/**
 * Unit tests for UserService.
 * Test counts match V(G) cyclomatic complexity:
 * - Auth: register(3) + login(2) + logout(2) + logoutAll(2) + refresh(2) = 11
 * - Profile: getProfile(2) + updateProfile(4) + updatePassword(3) = 9
 * - Admin: getUserById(2) + getAllUsers(2) + updateRole(2) + deleteUser(3) = 9
 * Total: 29 tests (but some overlap, actual = 27)
 */
@ExtendWith(MockitoExtension.class)
@RegressionTest
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
    private RefreshTokenRepo refreshTokenRepo;

    @Mock
    private BookingRepo bookingRepo;

    @Mock
    private Authentication authentication;

    @Mock
    private UserMapper userMapper;

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
        mockUser.setPhoneNumber("0123456789");
        mockUser.setRole(UserRole.USER);
        mockUser.setLoyaltyPoints(0);
        mockUser.setMembershipTier(mockTier);
        mockUser.setRefreshTokens(new HashSet<>());
    }

    // ==================== register() - V(G)=3 ====================

    @Nested
    @DisplayName("register() - V(G)=3, Min Tests=3")
    class RegisterTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should successfully register new user with valid data")
        void testRegister_Success() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("newuser@example.com")
                    .username("newuser")
                    .password("password123")
                    .confirmPassword("password123")
                    .phoneNumber("0912345678")
                    .build();

            when(userRepo.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword123");
            when(membershipTierService.getDefaultTier()).thenReturn(mockTier);
            when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.register(request);

            verify(userRepo).findByEmail(request.getEmail());
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
        @RegressionTest
        @DisplayName("TC-2: Should throw exception when email already exists")
        void testRegister_EmailAlreadyExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email(testEmail)
                    .username("testuser")
                    .password("password123")
                    .confirmPassword("password123")
                    .phoneNumber("0912345678")
                    .build();

            User existingUser = new User();
            existingUser.setEmail(testEmail);
            existingUser.setRole(UserRole.USER);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.register(request));
            assertEquals("Email already in use", exception.getMessage());
            verify(userRepo).findByEmail(testEmail);
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
                    .phoneNumber("0912345678")
                    .build();

            when(userRepo.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.register(request));
            assertEquals("Confirm passwords do not match", exception.getMessage());
            verify(userRepo).findByEmail(request.getEmail());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepo, never()).save(any());
        }
    }

    // ==================== login() - V(G)=2 ====================

    @Nested
    @DisplayName("login() - V(G)=2, Min Tests=2")
    class LoginTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
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
            when(userMapper.toLoginResponse(any(User.class))).thenReturn(mock(LoginResponse.class));

            Map<String, Object> result = userService.login(request);

            assertNotNull(result);
            assertEquals(accessToken, (String) result.get("accessToken"));
            assertEquals(refreshToken, (String) result.get("refreshToken"));
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

    // ==================== logout() - V(G)=2 ====================

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

    // ==================== logoutAllSessions() - V(G)=2 ====================

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

    // ==================== refreshAccessToken() - V(G)=2 ====================

    @Nested
    @DisplayName("refreshAccessToken() - V(G)=2, Min Tests=2")
    class RefreshAccessTokenTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
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

    // ==================== getCurrentUserProfile() - V(G)=2 ====================

    @Nested
    @DisplayName("getCurrentUserProfile() - V(G)=2, Min Tests=2")
    class GetCurrentUserProfileTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should return profile for authenticated user")
        void testGetCurrentUserProfile_Success() {
            UserProfileResponse mockResponse = new UserProfileResponse();
            mockResponse.setUserId(testUserId);
            mockResponse.setEmail(testEmail);

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserProfileResponse(mockUser)).thenReturn(mockResponse);

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                UserProfileResponse result = userService.getCurrentUserProfile();

                assertNotNull(result);
                assertEquals(testUserId, result.getUserId());
                assertEquals(testEmail, result.getEmail());
            }
        }

        @Test
        @DisplayName("TC-2: Should throw exception when user not found")
        void testGetCurrentUserProfile_UserNotFound() {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn("nonexistent@example.com");
            when(userRepo.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                assertThrows(IllegalArgumentException.class,
                        () -> userService.getCurrentUserProfile());
            }
        }
    }

    // ==================== updateUserProfile() - V(G)=4 ====================

    @Nested
    @DisplayName("updateUserProfile() - V(G)=4, Min Tests=4")
    class UpdateUserProfileTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should update username successfully")
        void testUpdateProfile_Username() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("newusername")
                    .phoneNumber("")
                    .build();

            UserProfileResponse mockResponse = new UserProfileResponse();
            mockResponse.setUsername("newusername");

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserProfileResponse(any())).thenReturn(mockResponse);

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                UserProfileResponse result = userService.updateUserProfile(request);

                assertEquals("newusername", result.getUsername());
                verify(userRepo).save(mockUser);
            }
        }

        @Test
        @RegressionTest
        @DisplayName("TC-2: Should update phone number successfully")
        void testUpdateProfile_PhoneNumber() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("")
                    .phoneNumber("0999888777")
                    .build();

            UserProfileResponse mockResponse = new UserProfileResponse();
            mockResponse.setPhoneNumber("0999888777");

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserProfileResponse(any())).thenReturn(mockResponse);

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                UserProfileResponse result = userService.updateUserProfile(request);

                assertEquals("0999888777", result.getPhoneNumber());
                verify(userRepo).save(mockUser);
            }
        }

        @Test
        @RegressionTest
        @DisplayName("TC-3: Should update avatar URL successfully")
        void testUpdateProfile_AvatarUrl() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("")
                    .phoneNumber("")
                    .avatarUrl("https://example.com/avatar.jpg")
                    .build();

            UserProfileResponse mockResponse = new UserProfileResponse();
            mockResponse.setAvatarUrl("https://example.com/avatar.jpg");

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserProfileResponse(any())).thenReturn(mockResponse);

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                UserProfileResponse result = userService.updateUserProfile(request);

                assertEquals("https://example.com/avatar.jpg", result.getAvatarUrl());
                verify(userRepo).save(mockUser);
            }
        }

        @Test
        @RegressionTest
        @DisplayName("TC-4: Should update multiple fields successfully")
        void testUpdateProfile_MultipleFields() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .username("updatedname")
                    .phoneNumber("0111222333")
                    .avatarUrl("https://example.com/new-avatar.jpg")
                    .build();

            UserProfileResponse mockResponse = new UserProfileResponse();
            mockResponse.setUsername("updatedname");
            mockResponse.setPhoneNumber("0111222333");
            mockResponse.setAvatarUrl("https://example.com/new-avatar.jpg");

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserProfileResponse(any())).thenReturn(mockResponse);

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                UserProfileResponse result = userService.updateUserProfile(request);

                assertEquals("updatedname", result.getUsername());
                assertEquals("0111222333", result.getPhoneNumber());
                verify(userRepo).save(mockUser);
            }
        }
    }

    // ==================== updatePassword() - V(G)=3 ====================

    @Nested
    @DisplayName("updatePassword() - V(G)=3, Min Tests=3")
    class UpdatePasswordTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should update password successfully")
        void testUpdatePassword_Success() {
            UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                    .currentPassword("password123")
                    .newPassword("newpassword456")
                    .confirmPassword("newpassword456")
                    .build();

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("password123", mockUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newpassword456")).thenReturn("encodedNewPassword");

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                userService.updatePassword(request);

                verify(userRepo).save(mockUser);
                verify(jwtService).revokeAllUserRefreshTokens(testEmail);
            }
        }

        @Test
        @RegressionTest
        @DisplayName("TC-2: Should fail when current password is incorrect")
        void testUpdatePassword_WrongCurrentPassword() {
            UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                    .currentPassword("wrongpassword")
                    .newPassword("newpassword456")
                    .confirmPassword("newpassword456")
                    .build();

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrongpassword", mockUser.getPassword())).thenReturn(false);

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> userService.updatePassword(request));
                assertEquals("Current password is incorrect", exception.getMessage());
                verify(userRepo, never()).save(any());
            }
        }

        @Test
        @RegressionTest
        @DisplayName("TC-3: Should fail when new passwords don't match")
        void testUpdatePassword_PasswordMismatch() {
            UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                    .currentPassword("password123")
                    .newPassword("newpassword456")
                    .confirmPassword("different789")
                    .build();

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.getName()).thenReturn(testEmail);
            when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("password123", mockUser.getPassword())).thenReturn(true);

            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> userService.updatePassword(request));
                assertEquals("New passwords do not match", exception.getMessage());
                verify(userRepo, never()).save(any());
            }
        }
    }

    // ==================== getUserById() - V(G)=2 ====================

    @Nested
    @DisplayName("getUserById() - V(G)=2, Min Tests=2")
    class GetUserByIdTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should return user profile by ID")
        void testGetUserById_Success() {
            UserProfileResponse mockResponse = new UserProfileResponse();
            mockResponse.setUserId(testUserId);
            mockResponse.setEmail(testEmail);

            when(userRepo.findById(testUserId)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserProfileResponse(mockUser)).thenReturn(mockResponse);

            UserProfileResponse result = userService.getUserById(testUserId);

            assertNotNull(result);
            assertEquals(testUserId, result.getUserId());
            verify(userRepo).findById(testUserId);
        }

        @Test
        @RegressionTest
        @DisplayName("TC-2: Should throw exception when user not found")
        void testGetUserById_UserNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(userRepo.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.getUserById(nonExistentId));
            verify(userRepo).findById(nonExistentId);
        }
    }

    // ==================== getAllUsers() - V(G)=2 ====================

    @Nested
    @DisplayName("getAllUsers() - V(G)=2, Min Tests=2")
    class GetAllUsersTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should return list of all users")
        void testGetAllUsers_Success() {
            User user2 = new User();
            user2.setId(UUID.randomUUID());
            user2.setEmail("user2@example.com");

            UserProfileResponse response1 = new UserProfileResponse();
            response1.setEmail(testEmail);
            UserProfileResponse response2 = new UserProfileResponse();
            response2.setEmail("user2@example.com");

            when(userRepo.findAll()).thenReturn(List.of(mockUser, user2));
            when(userMapper.toUserProfileResponse(mockUser)).thenReturn(response1);
            when(userMapper.toUserProfileResponse(user2)).thenReturn(response2);

            List<UserProfileResponse> result = userService.getAllUsers();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(userRepo).findAll();
        }

        @Test
        @RegressionTest
        @DisplayName("TC-2: Should return empty list when no users exist")
        void testGetAllUsers_EmptyList() {
            when(userRepo.findAll()).thenReturn(List.of());

            List<UserProfileResponse> result = userService.getAllUsers();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(userRepo).findAll();
        }
    }

    // ==================== updateUserRole() - V(G)=2 ====================

    @Nested
    @DisplayName("updateUserRole() - V(G)=2, Min Tests=2")
    class UpdateUserRoleTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should update user role successfully")
        void testUpdateUserRole_Success() {
            UserProfileResponse mockResponse = new UserProfileResponse();
            mockResponse.setUserId(testUserId);
            mockResponse.setRole("ADMIN");

            when(userRepo.findById(testUserId)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserProfileResponse(mockUser)).thenReturn(mockResponse);

            UserProfileResponse result = userService.updateUserRole(testUserId, "ADMIN");

            assertNotNull(result);
            assertEquals("ADMIN", result.getRole());
            verify(userRepo).save(mockUser);
        }

        @Test
        @RegressionTest
        @DisplayName("TC-2: Should throw exception with invalid role")
        void testUpdateUserRole_InvalidRole() {
            when(userRepo.findById(testUserId)).thenReturn(Optional.of(mockUser));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUserRole(testUserId, "INVALID_ROLE"));
            assertTrue(exception.getMessage().contains("Invalid role"));
        }
    }

    // ==================== deleteUser() - V(G)=3 ====================

    @Nested
    @DisplayName("deleteUser() - V(G)=3, Min Tests=3")
    class DeleteUserTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("TC-1: Should delete user successfully when no constraints")
        void testDeleteUser_Success() {
            when(userRepo.findById(testUserId)).thenReturn(Optional.of(mockUser));
            when(bookingRepo.countByUserId(testUserId)).thenReturn(0L);
            when(refreshTokenRepo.countByUserId(testUserId)).thenReturn(0L);

            userService.deleteUser(testUserId);

            verify(userRepo).delete(mockUser);
        }

        @Test
        @RegressionTest
        @DisplayName("TC-2: Should fail when user has existing bookings")
        void testDeleteUser_HasBookings() {
            when(userRepo.findById(testUserId)).thenReturn(Optional.of(mockUser));
            when(bookingRepo.countByUserId(testUserId)).thenReturn(5L);

            EntityDeletionForbiddenException exception = assertThrows(
                    EntityDeletionForbiddenException.class,
                    () -> userService.deleteUser(testUserId));
            assertTrue(exception.getMessage().contains("Cannot delete user with existing bookings"));
            verify(userRepo, never()).delete(any());
        }

        @Test
        @RegressionTest
        @DisplayName("TC-3: Should fail when user has active sessions")
        void testDeleteUser_HasActiveSessions() {
            when(userRepo.findById(testUserId)).thenReturn(Optional.of(mockUser));
            when(bookingRepo.countByUserId(testUserId)).thenReturn(0L);
            when(refreshTokenRepo.countByUserId(testUserId)).thenReturn(3L);

            EntityDeletionForbiddenException exception = assertThrows(
                    EntityDeletionForbiddenException.class,
                    () -> userService.deleteUser(testUserId));
            assertTrue(exception.getMessage().contains("Cannot delete user with active sessions"));
            verify(userRepo, never()).delete(any());
        }
    }
}
