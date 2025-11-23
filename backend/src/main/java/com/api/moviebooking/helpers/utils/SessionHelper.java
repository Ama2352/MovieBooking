package com.api.moviebooking.helpers.utils;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.models.dtos.SessionContext;
import com.api.moviebooking.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionHelper {

    private static final String SESSION_HEADER = "X-Session-Id";
    private final UserService userService;

    /**
     * Extract session context from request
     * Priority:
     * 1. JWT authentication (if present) → USER
     * 2. X-Session-Id header (if present) → GUEST_SESSION
     * 3. Error if neither present
     * 
     * @param request HTTP request
     * @return SessionContext with lockOwnerId and type
     * @throws CustomException if no valid session identifier found
     */
    public SessionContext extractSessionContext(HttpServletRequest request) {

        // Check authentication first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            // Only call userService if authenticated
            UUID userId = userService.getCurrentUser().getId();
            log.debug("Authenticated user session from UserService: {}", userId);
            return SessionContext.forUser(userId);
        }

        // Not authenticated - try guest session header
        String sessionId = request.getHeader(SESSION_HEADER);
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            // Validate UUID format
            try {
                UUID.fromString(sessionId);
                log.debug("Guest session: {}", sessionId);
                return SessionContext.forGuest(sessionId);
            } catch (IllegalArgumentException e) {
                throw new CustomException(
                        "Invalid session ID format. Must be a valid UUID.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Neither JWT nor session header present
        throw new CustomException(
                "Session identifier required. Please provide either authentication token or X-Session-Id header.",
                HttpStatus.UNAUTHORIZED);
    }

    /**
     * Extract session context from request, allowing optional authentication
     * Returns null if neither auth nor session ID present
     * 
     * @param request HTTP request
     * @return SessionContext or null
     */
    public SessionContext extractSessionContextOptional(HttpServletRequest request) {
        try {
            return extractSessionContext(request);
        } catch (CustomException e) {
            log.debug("No session context found: {}", e.getMessage());
            return null;
        }
    }
}
