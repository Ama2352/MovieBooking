package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.api.moviebooking.models.dtos.auth.LoginRequest;
import com.api.moviebooking.models.dtos.auth.RegisterRequest;
import com.api.moviebooking.models.entities.RefreshToken;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.MembershipTier;
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
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private String testEmail;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testUserId = UUID.randomUUID();

        mockUser = new User();
        mockUser.setId(testUserId);
        mockUser.setEmail(testEmail);
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setRole(UserRole.USER);
        mockUser.setMembershipTier(MembershipTier.BRONZE);
        mockUser.setRefreshTokens(new HashSet<>());
    }

    // ==================== findByEmail Tests ====================

    @Test
    @DisplayName("findByEmail - Success: Returns user when email exists")
    void testFindByEmail_Success() {
        // Arrange
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));

        // Act
        User result = userService.findByEmail(testEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertEquals(testUserId, result.getId());
        verify(userRepo).findByEmail(testEmail);
    }

    @Test
    @DisplayName("findByEmail - Failure: Throws exception when user not found")
    void testFindByEmail_UserNotFound() {
        // Arrange
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.findByEmail(testEmail));
        assertEquals("User not found", exception.getMessage());
        verify(userRepo).findByEmail(testEmail);
    }

    // ==================== findUserById Tests ====================

    @Test
    @DisplayName("findUserById - Success: Returns user when ID exists")
    void testFindUserById_Success() {
        // Arrange
        when(userRepo.findById(testUserId)).thenReturn(Optional.of(mockUser));

        // Act
        User result = userService.findUserById(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals(testEmail, result.getEmail());
        verify(userRepo).findById(testUserId);
    }

    @Test
    @DisplayName("findUserById - Failure: Throws exception when user not found")
    void testFindUserById_UserNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepo.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.findUserById(nonExistentId));
        assertEquals("User not found", exception.getMessage());
        verify(userRepo).findById(nonExistentId);
    }

    // ==================== register Tests ====================

    @Test
    @DisplayName("register - Success: Creates new user with valid data")
    void testRegister_Success() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .username("newuser")
                .password("password123")
                .confirmPassword("password123")
                .phoneNumber("1234567890")
                .build();

        when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword123");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.register(request);

        // Assert
        verify(userRepo).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepo).save(argThat(user -> user.getEmail().equals(request.getEmail()) &&
                user.getUsername().equals(request.getUsername()) &&
                user.getPassword().equals("encodedPassword123") &&
                user.getPhoneNumber().equals(request.getPhoneNumber()) &&
                user.getRole() == UserRole.USER &&
                user.getMembershipTier() == MembershipTier.BRONZE));
    }

    @Test
    @DisplayName("register - Failure: Throws exception when email already exists")
    void testRegister_EmailAlreadyExists() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email(testEmail)
                .username("testuser")
                .password("password123")
                .confirmPassword("password123")
                .phoneNumber("1234567890")
                .build();

        when(userRepo.existsByEmail(testEmail)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request));
        assertEquals("Email already in use", exception.getMessage());
        verify(userRepo).existsByEmail(testEmail);
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("register - Failure: Throws exception when passwords don't match")
    void testRegister_PasswordsDoNotMatch() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .username("newuser")
                .password("password123")
                .confirmPassword("differentPassword")
                .phoneNumber("1234567890")
                .build();

        when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request));
        assertEquals("Confirm passwords do not match", exception.getMessage());
        verify(userRepo).existsByEmail(request.getEmail());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepo, never()).save(any());
    }

    // ==================== login Tests ====================

    @Test
    @DisplayName("login - Success: Returns access and refresh tokens")
    void testLogin_Success() {
        // Arrange
        LoginRequest request = LoginRequest.builder().email(testEmail).password("password123").build();

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

        // Act
        Map<String, String> result = userService.login(request);

        // Assert
        assertNotNull(result);
        assertEquals(accessToken, result.get("accessToken"));
        assertEquals(refreshToken, result.get("refreshToken"));
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateAccessToken(eq(testEmail), any());
        verify(jwtService).generateRefreshToken(testEmail);
    }

    @Test
    @DisplayName("login - Failure: Authentication fails with invalid credentials")
    void testLogin_InvalidCredentials() {
        // Arrange
        LoginRequest request = LoginRequest.builder().email(testEmail).password("wrongPassword").build();

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.login(request));
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateAccessToken(any(), any());
        verify(jwtService, never()).generateRefreshToken(any());
    }

    // ==================== getCurrentUser Tests ====================

    @Test
    @DisplayName("getCurrentUser - Success: Returns currently authenticated user")
    void testGetCurrentUser_Success() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testEmail);
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));
        SecurityContextHolder.setContext(securityContext);

        // Act
        User result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertEquals(testUserId, result.getId());
        verify(userRepo).findByEmail(testEmail);
    }

    @Test
    @DisplayName("getCurrentUser - Failure: Throws exception when user not found")
    void testGetCurrentUser_UserNotFound() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testEmail);
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.empty());
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.getCurrentUser());
        verify(userRepo).findByEmail(testEmail);
    }

    // ==================== getUserIdFromPrincipal Tests ====================

    @Test
    @DisplayName("getUserIdFromPrincipal - Success: Extracts user ID from principal")
    void testGetUserIdFromPrincipal_Success() {
        // Arrange
        when(principal.getName()).thenReturn(testEmail);
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));

        // Act
        UUID result = userService.getUserIdFromPrincipal(principal);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result);
        verify(principal).getName();
        verify(userRepo).findByEmail(testEmail);
    }

    @Test
    @DisplayName("getUserIdFromPrincipal - Failure: Throws exception when user not found")
    void testGetUserIdFromPrincipal_UserNotFound() {
        // Arrange
        when(principal.getName()).thenReturn(testEmail);
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> userService.getUserIdFromPrincipal(principal));
        verify(principal).getName();
        verify(userRepo).findByEmail(testEmail);
    }

    // ==================== addUserRefreshToken Tests ====================

    @Test
    @DisplayName("addUserRefreshToken - Success: Adds refresh token to user")
    void testAddUserRefreshToken_Success() {
        // Arrange
        String refreshToken = "refresh-token-789";
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.of(mockUser));

        // Act
        userService.addUserRefreshToken(refreshToken, testEmail);

        // Assert
        assertEquals(1, mockUser.getRefreshTokens().size());
        RefreshToken addedToken = mockUser.getRefreshTokens().iterator().next();
        assertEquals(refreshToken, addedToken.getToken());
        assertEquals(mockUser, addedToken.getUser());
        verify(userRepo).findByEmail(testEmail);
    }

    @Test
    @DisplayName("addUserRefreshToken - Failure: Throws exception when user not found")
    void testAddUserRefreshToken_UserNotFound() {
        // Arrange
        String refreshToken = "refresh-token-789";
        when(userRepo.findByEmail(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> userService.addUserRefreshToken(refreshToken, testEmail));
        verify(userRepo).findByEmail(testEmail);
    }

    // ==================== logout Tests ====================

    @Test
    @DisplayName("logout - Success: Revokes refresh token")
    void testLogout_Success() {
        // Arrange
        String refreshToken = "refresh-token-to-revoke";
        doNothing().when(jwtService).revokeRefreshToken(refreshToken);

        // Act
        userService.logout(refreshToken);

        // Assert
        verify(jwtService).revokeRefreshToken(refreshToken);
    }

    @Test
    @DisplayName("logout - Failure: Throws exception when token revocation fails")
    void testLogout_TokenRevocationFails() {
        // Arrange
        String refreshToken = "invalid-token";
        doThrow(new RuntimeException("Token not found"))
                .when(jwtService).revokeRefreshToken(refreshToken);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.logout(refreshToken));
        assertEquals("Invalid refresh token", exception.getMessage());
        verify(jwtService).revokeRefreshToken(refreshToken);
    }

    // ==================== logoutAllSessions Tests ====================

    @Test
    @DisplayName("logoutAllSessions - Success: Revokes all user refresh tokens")
    void testLogoutAllSessions_Success() {
        // Arrange
        doNothing().when(jwtService).revokeAllUserRefreshTokens(testEmail);

        // Act
        userService.logoutAllSessions(testEmail);

        // Assert
        verify(jwtService).revokeAllUserRefreshTokens(testEmail);
    }

    @Test
    @DisplayName("logoutAllSessions - Failure: Throws exception when revocation fails")
    void testLogoutAllSessions_RevocationFails() {
        // Arrange
        doThrow(new RuntimeException("Database error"))
                .when(jwtService).revokeAllUserRefreshTokens(testEmail);

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.logoutAllSessions(testEmail));
        assertEquals("Error during logout from all sessions", exception.getMessage());
        verify(jwtService).revokeAllUserRefreshTokens(testEmail);
    }

    // ==================== refreshAccessToken Tests ====================

    @Test
    @DisplayName("refreshAccessToken - Success: Returns new access token")
    void testRefreshAccessToken_Success() {
        // Arrange
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

        // Act
        String result = userService.refreshAccessToken(refreshToken);

        // Assert
        assertNotNull(result);
        assertEquals(newAccessToken, result);
        verify(jwtService).validateRefreshToken(refreshToken);
        verify(jwtService).extractEmailFromToken(refreshToken);
        verify(customUserDetailsService).loadUserByUsername(testEmail);
        verify(jwtService).generateAccessToken(eq(testEmail), any());
    }

    @Test
    @DisplayName("refreshAccessToken - Failure: Throws exception with invalid token")
    void testRefreshAccessToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid-refresh-token";
        when(jwtService.validateRefreshToken(invalidToken)).thenReturn(false);

        // Act & Assert
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
