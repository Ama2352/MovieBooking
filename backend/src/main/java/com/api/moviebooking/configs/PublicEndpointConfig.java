package com.api.moviebooking.configs;

public final class PublicEndpointConfig {
        private PublicEndpointConfig() {
        }

        // OpenAPI/Swagger docs
        public static final String[] DOCS = {
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
        };

        // Auth endpoints that are public (no authentication)
        public static final String[] AUTH = {
                        "/auth/register",
                        "/auth/login"
        };

        public static final String[] MAKE_PAYMENT = {
                        "/payments/order",
                        "/payments/order/capture",
                        "/payments/vnpay/ipn"
        };

        // Public read-only resources (GET only)
        public static final String[] MOVIES = { "/movies/**" };
        public static final String[] SHOWTIMES = { "/showtimes/**" };
        public static final String[] PROMOTIONS = { "/promotions/**" };
        public static final String[] MEMBERSHIP_TIERS = { "/membership-tiers/**" };
        public static final String[] SEATS = { "/seats/**" };
        public static final String[] SHOWTIME_SEATS = { "/showtime-seats/**" };
        public static final String[] PRICE_BASE = { "/price-base/**" };
        public static final String[] PRICE_MODIFIERS = { "/price-modifiers/**" };
        public static final String[] CINEMAS = { "/cinemas/**" };
        public static final String[] PAYMENTS = { "/payments/**" };
}
