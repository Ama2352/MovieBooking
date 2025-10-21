package com.api.moviebooking.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.moviebooking.services.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@SecurityRequirement(name = "bearerToken")
@Tag(name = "User Management")
public class UserController {

    private final UserService userService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam(required = true) String refreshToken) {
        userService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllSessions(@RequestParam(required = true) String email) {
        userService.logoutAllSessions(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam(required = true) String refreshToken) {
        String newAccessToken = userService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok().body(Map.of("accessToken", newAccessToken));
    }
}
