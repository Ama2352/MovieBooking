package com.api.moviebooking.services;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.moviebooking.models.dtos.auth.LoginRequest;
import com.api.moviebooking.models.dtos.auth.RegisterRequest;
import com.api.moviebooking.models.entities.RefreshToken;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.MembershipTier;
import com.api.moviebooking.models.enums.UserRole;
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

        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm passwords do not match");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setPhoneNumber(phoneNumber);
        user.setRole(UserRole.USER);
        user.setMembershipTier(MembershipTier.BRONZE);

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

}
