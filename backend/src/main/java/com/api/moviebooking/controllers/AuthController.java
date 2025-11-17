package com.api.moviebooking.controllers;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.models.dtos.auth.LoginRequest;
import com.api.moviebooking.models.dtos.auth.RegisterRequest;
import com.api.moviebooking.models.dtos.user.CreateGuestRequest;
import com.api.moviebooking.services.CookieService;
import com.api.moviebooking.services.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final UserService userService;
    private final CookieService cookieService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Map<String, String> tokens = userService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieService.createAccessTokenCookie(tokens.get("accessToken")).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieService.createRefreshTokenCookie(tokens.get("refreshToken")).toString())
                .build();
    }

    @SecurityRequirement(name = "bearerToken")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = cookieService.extractRefreshTokenFromCookie(request);
        userService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }

    @SecurityRequirement(name = "bearerToken")
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllSessions(@RequestParam(required = true) String email) {
        userService.logoutAllSessions(email);
        return ResponseEntity.ok().build();
    }

    @SecurityRequirement(name = "bearerToken")
    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String refreshToken = cookieService.extractRefreshTokenFromCookie(request);
        String newAccessToken = userService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieService.createAccessTokenCookie(newAccessToken).toString())
                .build();
    }

    @PostMapping("/guest/register")
    public ResponseEntity<Map<String, String>> registerGuest(@Valid @RequestBody CreateGuestRequest request) {
        String userId = userService.registerGuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("userId", userId));
    }

}
