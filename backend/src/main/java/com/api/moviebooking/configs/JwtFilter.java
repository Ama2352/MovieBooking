package com.api.moviebooking.configs;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.api.moviebooking.services.CustomUserDetailsService;
import com.api.moviebooking.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.api.moviebooking.services.CookieService;

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final CookieService cookieService;

    @SuppressWarnings("NullableProblems")
    // @Override
    // protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    //         throws ServletException, IOException {
    //     String authHeader = request.getHeader("Authorization");
    //     String token = null;
    //     String email = null;

    //     if (authHeader != null && authHeader.startsWith("Bearer ")) {
    //         token = authHeader.substring(7);
    //         email = jwtService.extractEmailFromToken(token);
    //     }

    //     if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
    //         UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

    //         if (jwtService.validateAccessToken(token, userDetails)) {
    //             UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
    //                     null, userDetails.getAuthorities());
    //             authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    //             SecurityContextHolder.getContext().setAuthentication(authToken);
    //         }
    //     }

    //     filterChain.doFilter(request, response);
    // }

    @Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

    String token = null;

    // 1️⃣ Ưu tiên lấy từ header Authorization
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
    } else {
        // 2️⃣ Nếu không có header thì lấy từ cookie access_token
        token = cookieService.extractAccessTokenFromCookie(request);
    }

    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        String email = jwtService.extractEmailFromToken(token);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        if (jwtService.validateAccessToken(token, userDetails)) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    filterChain.doFilter(request, response);
}

}
