// k6-tests/config/config.js
// Shared configuration for all k6 test scenarios

export const CONFIG = {
    // Azure VM endpoint (override with: k6 run -e API_URL=http://x.x.x.x:8080)
    BASE_URL: __ENV.API_URL || 'http://localhost:8080',
    
    // Test data IDs - MUST be populated after seeding test data
    TEST_MOVIE_ID: __ENV.MOVIE_ID || 'c1000000-0000-0000-0000-000000000001',
    TEST_SHOWTIME_ID: __ENV.SHOWTIME_ID || 'd1000000-0000-0000-0000-000000000001',
    TEST_CINEMA_ID: __ENV.CINEMA_ID || 'a1000000-0000-0000-0000-000000000001',
    TEST_ROOM_ID: __ENV.ROOM_ID || 'b1000000-0000-0000-0000-000000000001',
    
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
