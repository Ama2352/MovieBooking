package com.api.moviebooking.configs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.api.moviebooking.models.entities.CustomUserDetails;
import com.api.moviebooking.models.entities.User;
import com.api.moviebooking.models.enums.UserRole;
import com.api.moviebooking.repositories.UserRepo;
import com.api.moviebooking.services.CookieService;
import com.api.moviebooking.services.JwtService;
import com.api.moviebooking.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

        private final JwtService jwtService;
        private final UserRepo userRepo;
        private final UserService userService;
        private final CookieService cookieService;

        @Value("${frontend.redirect.url:http://localhost:5173/oauth2/success}")
        private String frontendRedirectUrl;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) throws IOException, ServletException {

                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                String email = oAuth2User.getAttribute("email");
                String username = oAuth2User.getAttribute("name");

                User user = userRepo.findByEmail(email)
                                .orElseGet(() -> createNewUser(email, username));

                if (user.getRole() == UserRole.GUEST) {
                        user.setRole(UserRole.USER);
                        userRepo.save(user);
                }

                CustomUserDetails userDetails = new CustomUserDetails(user, oAuth2User.getAttributes());

                Authentication newAuth = new OAuth2AuthenticationToken(
                                userDetails,
                                userDetails.getAuthorities(),
                                authentication.getName());

                SecurityContextHolder.getContext().setAuthentication(newAuth);

                String accessToken = jwtService.generateAccessToken(user.getEmail(), userDetails.getAuthorities());
                String refreshToken = jwtService.generateRefreshToken(user.getEmail());

                userService.addUserRefreshToken(refreshToken, user.getEmail());

                response.addHeader(HttpHeaders.SET_COOKIE,
                                cookieService.createAccessTokenCookie(accessToken).toString());

                response.addHeader(HttpHeaders.SET_COOKIE,
                                cookieService.createRefreshTokenCookie(refreshToken).toString());

                response.sendRedirect(frontendRedirectUrl);
        }

        private User createNewUser(String email, String username) {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(username != null ? username : email);
                newUser.setProvider("google");
                newUser.setRole(UserRole.USER);
                return userRepo.save(newUser);
        }
}