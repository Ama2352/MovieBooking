// k6-tests/config/config.js
// =============================================================================
// Shared configuration for all k6 test scenarios
// =============================================================================
// 
// The K6TestDataSeeder creates test data with auto-generated UUIDs.
// k6 tests should fetch IDs dynamically from the API in their setup() phase.
//
// To enable K6 test data seeding, set environment variable:
//   K6_SEED_ENABLED=true
//
// Local (Docker Compose): Add to backend/.env or docker-compose.yml
// Azure VM (prod):        Set in environment or .env file
//
// Test Data Created by Seeder:
// - Movie: "K6 Performance Test Movie"
// - Cinema: "K6 Test Cinema" with Room 99 (IMAX)
// - 3 Showtimes with 100 available seats each
// - 3 Ticket Types: k6_adult, k6_student, k6_senior
//
// =============================================================================

import http from 'k6/http';

export const CONFIG = {
    // Azure VM endpoint (override with: k6 run -e API_URL=http://x.x.x.x:8080)
    BASE_URL: __ENV.API_URL || 'http://localhost:8080',
    
    // K6 Test Movie title (used to find the movie dynamically)
    K6_MOVIE_TITLE: 'K6 Performance Test Movie',
    
    // Default thresholds (can be overridden per scenario)
    THRESHOLDS: {
        http_req_failed: ['rate<0.05'],      // Error rate < 5%
        http_req_duration: ['p(95)<3000'],   // 95% requests < 3s
    },
    
    // Test settings
    THINK_TIME_MIN: 1,  // Minimum think time between requests (seconds)
    THINK_TIME_MAX: 3,  // Maximum think time
};

// Standard headers for all requests
export const HEADERS = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
};

/**
 * Fetch K6 test data IDs dynamically from the API
 * Call this in setup() to get the movie, showtime, and other IDs
 */
export function fetchK6TestData() {
    console.log('🔍 Fetching K6 test data from API...');
    
    // 1. Get movies and find "K6 Performance Test Movie"
    const moviesRes = http.get(`${CONFIG.BASE_URL}/movies`, { headers: HEADERS });
    if (moviesRes.status !== 200) {
        console.error(`❌ Failed to fetch movies: ${moviesRes.status}`);
        return null;
    }
    
    const movies = JSON.parse(moviesRes.body);
    const k6Movie = movies.find(m => m.title === CONFIG.K6_MOVIE_TITLE);
    
    if (!k6Movie) {
        console.error(`❌ K6 test movie not found. Make sure K6_SEED_ENABLED=true`);
        console.error(`   Looking for: "${CONFIG.K6_MOVIE_TITLE}"`);
        return null;
    }
    
    console.log(`✅ Found movie: ${k6Movie.title} (ID: ${k6Movie.movieId})`);
    
    // 2. Get showtimes for this movie
    const showtimesRes = http.get(
        `${CONFIG.BASE_URL}/showtimes/movie/${k6Movie.movieId}/upcoming`,
        { headers: HEADERS }
    );
    
    if (showtimesRes.status !== 200) {
        console.error(`❌ Failed to fetch showtimes: ${showtimesRes.status}`);
        return null;
    }
    
    const showtimes = JSON.parse(showtimesRes.body);
    if (showtimes.length === 0) {
        console.error(`❌ No showtimes found for K6 test movie`);
        return null;
    }
    
    // Use the first showtime
    const showtime = showtimes[0];
    console.log(`✅ Found ${showtimes.length} showtimes, using: ${showtime.showtimeId}`);
    
    return {
        movieId: k6Movie.movieId,
        movie: k6Movie,
        showtimeId: showtime.showtimeId,
        showtime: showtime,
        allShowtimes: showtimes,
    };
}

/**
 * Generate a UUID v4 for guest session IDs
 * k6 doesn't have crypto.randomUUID(), so we implement it
 */
export function generateSessionId() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/**
 * Generate random think time
 */
export function randomThinkTime() {
    return Math.random() * (CONFIG.THINK_TIME_MAX - CONFIG.THINK_TIME_MIN) + CONFIG.THINK_TIME_MIN;
}

/**
 * Pick random item from array
 */
export function randomItem(array) {
    return array[Math.floor(Math.random() * array.length)];
}

/**
 * Generate random test email
 */
export function generateTestEmail(prefix = 'k6test') {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8);
    return `${prefix}_${timestamp}_${random}@loadtest.local`;
}

/**
 * Parse JSON response safely
 */
export function safeParseJson(response) {
    try {
        return JSON.parse(response.body);
    } catch (e) {
        console.error(`Failed to parse JSON: ${response.body}`);
        return null;
    }
}

/**
 * Log test step for debugging
 */
export function logStep(step, message) {
    if (__ENV.DEBUG === 'true') {
        console.log(`[${step}] ${message}`);
    }
}
