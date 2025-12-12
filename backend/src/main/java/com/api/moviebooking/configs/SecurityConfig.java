package com.api.moviebooking.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(
            HttpSecurity httpSecurity,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        return httpSecurity
                .securityMatcher("/oauth2/**", "/login/**")
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, JwtFilter filter) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PublicEndpointConfig.DOCS).permitAll()
                        .requestMatchers(PublicEndpointConfig.SEAT_LOCKS).permitAll()
                        .requestMatchers(PublicEndpointConfig.MAKE_PAYMENT).permitAll()
                        .requestMatchers(PublicEndpointConfig.CHECKOUT).permitAll()
                        .requestMatchers(PublicEndpointConfig.TESTS).permitAll()
                        .requestMatchers(PublicEndpointConfig.REFUNDS).permitAll()
                        .requestMatchers(PublicEndpointConfig.BOOKING_PUBLIC).permitAll()
                        .requestMatchers(PublicEndpointConfig.ACTUATORS).permitAll()
                        .requestMatchers(HttpMethod.POST, PublicEndpointConfig.AUTH).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.MOVIES).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.SHOWTIMES).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.PROMOTIONS).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.MEMBERSHIP_TIERS).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.SEATS).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.SHOWTIME_SEATS).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.PRICE_BASE).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.PRICE_MODIFIERS).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.CINEMAS).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.PAYMENTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PublicEndpointConfig.TICKET_TYPES).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
