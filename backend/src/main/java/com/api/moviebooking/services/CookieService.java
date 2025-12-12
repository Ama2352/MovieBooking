package com.api.moviebooking.services;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final Duration ACCESS_TOKEN_EXPIRY = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

    private static final String SAME_SITE_POLICY = "Strict";
    private static final boolean SECURE = false; // true in prod (HTTPS)

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(SECURE)
                .path("/")
                .maxAge(ACCESS_TOKEN_EXPIRY)
                .sameSite(SAME_SITE_POLICY)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(SECURE)
                .path("/auth")
                .maxAge(REFRESH_TOKEN_EXPIRY)
                .sameSite(SAME_SITE_POLICY)
                .build();
    }

    public String extractAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies()) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(SECURE)
                .path("/")
                .maxAge(0)
                .sameSite(SAME_SITE_POLICY)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(SECURE)
                .path("/auth")
                .maxAge(0)
                .sameSite(SAME_SITE_POLICY)
                .build();
    }
}
