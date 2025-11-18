package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.moviebooking.helpers.exceptions.EntityDeletionForbiddenException;
import com.api.moviebooking.helpers.mapstructs.MembershipTierMapper;
import com.api.moviebooking.helpers.mapstructs.UserMapper;
import com.api.moviebooking.models.dtos.auth.LoginRequest;
import com.api.moviebooking.models.dtos.auth.RegisterRequest;
import com.api.moviebooking.models.dtos.membershipTier.MembershipTierDataResponse;
import com.api.moviebooking.models.dtos.user.CreateGuestRequest;
import com.api.moviebooking.models.dtos.user.UpdatePasswordRequest;
import com.api.moviebooking.models.dtos.user.UpdateProfileRequest;
import com.api.moviebooking.models.dtos.user.UserProfileResponse;
import com.api.moviebooking.models.entities.MembershipTier;
import com.api.moviebooking.models.entities.RefreshToken;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.RefreshTokenRepo;
import com.api.moviebooking.repositories.UserRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final MembershipTierService membershipTierService;
    private final MembershipTierMapper membershipTierMapper;
    private final RefreshTokenRepo refreshTokenRepo;
    private final BookingRepo bookingRepo;
    private final JwtService JwtService;
    private final UserMapper userMapper;

    private static final String VN_PHONE_REGEX = "^(03|05|07|08|09)[0-9]{8}$";

    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User findUserById(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Register new user with validation
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: existsByEmail, !equals(password, confirmPassword)
     */
    public void register(RegisterRequest request) {
        String email = request.getEmail();
        String username = request.getUsername();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();
        String phoneNumber = request.getPhoneNumber();

        Optional<User> existingUser = userRepo.findByEmail(email);

        if (existingUser.isPresent() && existingUser.get().getRole() != UserRole.GUEST) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm passwords do not match");
        }

        if (!phoneNumber.matches(VN_PHONE_REGEX)) {
            throw new IllegalArgumentException("Invalid Vietnamese phone number");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = existingUser.orElseGet(User::new);
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setPhoneNumber(phoneNumber);
        user.setRole(UserRole.USER);
        user.setLoyaltyPoints(0);

        // Assign default membership tier
        MembershipTier defaultTier = membershipTierService.getDefaultTier();
        user.setMembershipTier(defaultTier);

        userRepo.save(user);
    }

    /**
     * Login user and generate tokens
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: authenticate (implicit exception throw)
     */
    public Map<String, String> login(LoginRequest request) {
        Authentication authentication = authManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(request.getEmail(), userDetails.getAuthorities());
        String refreshToken = jwtService.generateRefreshToken(request.getEmail());

        // Persist refresh token
        addUserRefreshToken(refreshToken, userDetails.getUsername());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken);
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return findByEmail(email);
    }

    public UUID getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName();
        return findByEmail(email).getId();
    }

    @Transactional
    public void addUserRefreshToken(String refreshToken, String email) {
        User user = findByEmail(email);
        RefreshToken newToken = new RefreshToken();
        newToken.setToken(refreshToken);
        newToken.setUser(user);
        user.getRefreshTokens().add(newToken);
    }

    /**
     * Logout user by revoking refresh token
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: catch
     */
    public void logout(String refreshToken) {
        try {
            jwtService.revokeRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }

    /**
     * Logout user from all sessions
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: catch
     */
    public void logoutAllSessions(String email) {
        try {
            jwtService.revokeAllUserRefreshTokens(email);
        } catch (Exception e) {
            throw new RuntimeException("Error during logout from all sessions");
        }
    }

    /**
     * Refresh access token using valid refresh token
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: !validateRefreshToken
     */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String email = jwtService.extractEmailFromToken(refreshToken);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        String newAccessToken = jwtService.generateAccessToken(email, userDetails.getAuthorities());

        return newAccessToken;
    }

    /**
     * Add loyalty points to user and update tier if necessary
     * Formula: 1 point per 1000 VND spent (configurable)
     */
    @Transactional
    public void addLoyaltyPoints(UUID userId, BigDecimal amountSpent) {
        User user = findUserById(userId);

        // Calculate points: 1 point per 1000 VND
        int pointsToAdd = amountSpent.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.DOWN)
                .intValue();

        int newPoints = user.getLoyaltyPoints() + pointsToAdd;
        user.setLoyaltyPoints(newPoints);

        // Check if user needs tier upgrade
        updateUserTier(user);

        userRepo.save(user);
    }

    @Transactional
    public void revokeLoyaltyPoints(UUID userId, BigDecimal amount) {
        User user = findUserById(userId);
        int pointsToRemove = amount.divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN)
                .intValue();
        int newPoints = Math.max(0, user.getLoyaltyPoints() - pointsToRemove);
        user.setLoyaltyPoints(newPoints);
        updateUserTier(user);
        userRepo.save(user);
    }

    /**
     * Update user's membership tier based on loyalty points
     */
    @Transactional
    public void updateUserTier(User user) {
        MembershipTier appropriateTier = membershipTierService.getApproppriateTier(user.getLoyaltyPoints());

        // Only update if tier is different
        if (user.getMembershipTier() == null ||
                !user.getMembershipTier().getId().equals(appropriateTier.getId())) {
            user.setMembershipTier(appropriateTier);
        }
    }

    public String registerGuest(CreateGuestRequest request) {

        Optional<User> existingUser = userRepo.findByEmail(request.getEmail());

        if (existingUser.isPresent() && existingUser.get().getRole() != UserRole.GUEST) {
            throw new IllegalArgumentException("Email belongs to a registered user. Please log in.");
        }

        if (!request.getPhoneNumber().matches(VN_PHONE_REGEX)) {
            throw new IllegalArgumentException("Invalid Vietnamese phone number");
        }

        User guestUser = existingUser.orElseGet(User::new);
        guestUser.setEmail(request.getEmail());
        guestUser.setUsername(request.getUsername());
        guestUser.setPhoneNumber(request.getPhoneNumber());
        guestUser.setRole(UserRole.GUEST);
        User savedUser = userRepo.save(guestUser);

        return savedUser.getId().toString();
    }

    // ========== User Profile Management ==========

    /**
     * Get current user's profile
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: membershipTier != null
     */
    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return userMapper.toUserProfileResponse(user);
    }

    /**
     * Update user profile
     * Predicate nodes (d): 3 -> V(G) = d + 1 = 4
     * Nodes: username != null, phoneNumber != null, !matches(regex)
     */
    @Transactional
    public UserProfileResponse updateUserProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (!request.getPhoneNumber().matches(VN_PHONE_REGEX)) {
                throw new IllegalArgumentException("Invalid Vietnamese phone number");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepo.save(user);

        return getCurrentUserProfile();
    }

    /**
     * Update user password
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: !matches(currentPassword), !equals(newPassword, confirmPassword)
     */
    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        User user = getCurrentUser();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Verify new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        // Update password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        userRepo.save(user);

        jwtService.revokeAllUserRefreshTokens(user.getEmail());
    }

    // ========== Admin User Management ==========

    /**
     * Get user by ID (Admin)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: findUserById
     */
    public UserProfileResponse getUserById(UUID userId) {
        User user = findUserById(userId);
        return userMapper.toUserProfileResponse(user);
    }

    /**
     * Get all users (Admin)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: for-each loop (stream)
     */
    public List<UserProfileResponse> getAllUsers() {
        return userRepo.findAll().stream()
                .map(userMapper::toUserProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update user role (Admin)
     * Predicate nodes (d): 1 -> V(G) = d + 1 = 2
     * Nodes: try-catch
     */
    @Transactional
    public UserProfileResponse updateUserRole(UUID userId, String role) {
        User user = findUserById(userId);

        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            user.setRole(userRole);
            userRepo.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        return getUserById(userId);
    }

    /**
     * Delete user (Admin)
     * Predicate nodes (d): 2 -> V(G) = d + 1 = 3
     * Nodes: hasBookings, hasRefreshTokens
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = findUserById(userId);

        // Check for existing bookings
        long bookingCount = bookingRepo.countByUserId(userId);
        if (bookingCount > 0) {
            throw new EntityDeletionForbiddenException(
                    "Cannot delete user with existing bookings. Found " + bookingCount + " booking(s).");
        }

        // Check for refresh tokens (active sessions)
        long tokenCount = refreshTokenRepo.countByUserId(userId);
        if (tokenCount > 0) {
            throw new EntityDeletionForbiddenException(
                    "Cannot delete user with active sessions. Found " + tokenCount
                            + " refresh token(s). Please revoke all sessions first.");
        }

        userRepo.delete(user);
    }

}
