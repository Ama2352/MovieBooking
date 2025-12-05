// k6-tests/scenarios/02-browsing-load.js
// =============================================================================
// BROWSING LOAD TEST
// =============================================================================
// Purpose: Test read-heavy endpoints under high user load
// Simulates: Users browsing movies, viewing showtimes, checking seat maps
// Expected: Fast response times, zero errors for read operations
// =============================================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// Configuration
const CONFIG = {
    BASE_URL: __ENV.API_URL || 'http://localhost:8080',
    TEST_MOVIE_ID: __ENV.MOVIE_ID || 'c1000000-0000-0000-0000-000000000001',
    TEST_SHOWTIME_ID: __ENV.SHOWTIME_ID || 'd1000000-0000-0000-0000-000000000001',
    TEST_CINEMA_ID: __ENV.CINEMA_ID || 'a1000000-0000-0000-0000-000000000001',
};

const HEADERS = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
};

// Custom metrics
const moviesLatency = new Trend('movies_list_duration');
const showtimesLatency = new Trend('showtimes_list_duration');
const seatmapLatency = new Trend('seatmap_load_duration');
const ticketTypesLatency = new Trend('ticket_types_duration');
const pageViews = new Counter('page_views');

// =============================================================================
// TEST OPTIONS
// =============================================================================
export const options = {
    scenarios: {
        browsing_simulation: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 40 },   // Warm up
                { duration: '30s', target: 80 },   // Normal load
                { duration: '40s', target: 120 },  // Peak load (120 VUs)
                { duration: '30s', target: 80 },   // Scale down
                { duration: '20s', target: 0 },    // Cool down
            ],
        },
    },
    thresholds: {
        // Response time thresholds (read operations should be fast)
        'movies_list_duration': ['p(95)<500', 'p(99)<1000'],
        'showtimes_list_duration': ['p(95)<500', 'p(99)<1000'],
        'seatmap_load_duration': ['p(95)<800', 'p(99)<1500'],
        'ticket_types_duration': ['p(95)<300', 'p(99)<500'],
        
        // Error rate (read operations should never fail)
        'http_req_failed': ['rate<0.01'],  // Less than 1% errors
        
        // Overall latency
        'http_req_duration': ['p(95)<1000'],
    },
};

// =============================================================================
// SETUP
// =============================================================================
export function setup() {
    console.log(`🎬 Starting Browsing Load Test`);
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    
    // Verify endpoints are accessible
    const healthCheck = http.get(`${CONFIG.BASE_URL}/actuator/health`);
    if (healthCheck.status !== 200) {
        console.error(`❌ Health check failed: ${healthCheck.status}`);
    } else {
        console.log(`✅ Health check passed`);
    }
    
    // Fetch movie list to ensure data exists
    const moviesRes = http.get(`${CONFIG.BASE_URL}/movies`);
    const movies = JSON.parse(moviesRes.body || '[]');
    console.log(`✅ Found ${movies.length} movies`);
    
    return { movieCount: movies.length };
}

// =============================================================================
// MAIN TEST - User Browsing Journey
// =============================================================================
export default function(data) {
    
    group('User Browsing Journey', function() {
        
        // =========================================
        // STEP 1: Browse Movies List (Home Page)
        // =========================================
        group('1. Browse Movies', function() {
            const startTime = Date.now();
            
            const res = http.get(
                `${CONFIG.BASE_URL}/movies`,
                { 
                    headers: HEADERS, 
                    tags: { name: 'get_movies' } 
                }
            );
            
            moviesLatency.add(Date.now() - startTime);
            pageViews.add(1);
            
            check(res, {
                'movies: status 200': (r) => r.status === 200,
                'movies: has content': (r) => r.body.length > 2,  // Not empty array
                'movies: valid JSON': (r) => {
                    try {
                        JSON.parse(r.body);
                        return true;
                    } catch (e) {
                        return false;
                    }
                },
            });
        });
        
        // User browses movies (think time)
        sleep(Math.random() * 2 + 1);
        
        // =========================================
        // STEP 2: View Upcoming Showtimes
        // =========================================
        group('2. View Showtimes', function() {
            const startTime = Date.now();
            
            const res = http.get(
                `${CONFIG.BASE_URL}/showtimes/movie/${CONFIG.TEST_MOVIE_ID}/upcoming`,
                { 
                    headers: HEADERS, 
                    tags: { name: 'get_showtimes' } 
                }
            );
            
            showtimesLatency.add(Date.now() - startTime);
            pageViews.add(1);
            
            check(res, {
                'showtimes: status 200': (r) => r.status === 200,
                'showtimes: valid JSON': (r) => {
                    try {
                        JSON.parse(r.body);
                        return true;
                    } catch (e) {
                        return false;
                    }
                },
            });
        });
        
        // User picks a showtime (think time)
        sleep(Math.random() * 2 + 1);
        
        // =========================================
        // STEP 3: Load Seat Map
        // =========================================
        group('3. Load Seat Map', function() {
            const startTime = Date.now();
            
            const res = http.get(
                `${CONFIG.BASE_URL}/showtime-seats/showtime/${CONFIG.TEST_SHOWTIME_ID}`,
                { 
                    headers: HEADERS, 
                    tags: { name: 'get_seatmap' } 
                }
            );
            
            seatmapLatency.add(Date.now() - startTime);
            pageViews.add(1);
            
            const seats = res.status === 200 ? JSON.parse(res.body) : [];
            
            check(res, {
                'seatmap: status 200': (r) => r.status === 200,
                'seatmap: has seats': (r) => seats.length > 0,
                'seatmap: seats have required fields': (r) => {
                    if (seats.length === 0) return true;
                    const seat = seats[0];
                    return seat.showtimeSeatId && seat.status && seat.rowLabel !== undefined;
                },
            });
        });
        
        // User studies seat map (longer think time)
        sleep(Math.random() * 3 + 2);
        
        // =========================================
        // STEP 4: Get Ticket Types
        // =========================================
        group('4. Get Ticket Types', function() {
            const startTime = Date.now();
            
            const res = http.get(
                `${CONFIG.BASE_URL}/ticket-types?showtimeId=${CONFIG.TEST_SHOWTIME_ID}`,
                { 
                    headers: HEADERS, 
                    tags: { name: 'get_ticket_types' } 
                }
            );
            
            ticketTypesLatency.add(Date.now() - startTime);
            pageViews.add(1);
            
            check(res, {
                'ticket types: status 200': (r) => r.status === 200,
                'ticket types: valid JSON': (r) => {
                    try {
                        const types = JSON.parse(r.body);
                        return Array.isArray(types);
                    } catch (e) {
                        return false;
                    }
                },
            });
        });
        
        // =========================================
        // STEP 5: Check Seat Availability
        // =========================================
        group('5. Check Availability', function() {
            const res = http.get(
                `${CONFIG.BASE_URL}/seat-locks/availability/${CONFIG.TEST_SHOWTIME_ID}`,
                { 
                    headers: HEADERS, 
                    tags: { name: 'check_availability' } 
                }
            );
            
            pageViews.add(1);
            
            check(res, {
                'availability: status 200': (r) => r.status === 200,
                'availability: has seat arrays': (r) => {
                    try {
                        const data = JSON.parse(r.body);
                        return data.availableSeats && data.lockedSeats && data.bookedSeats;
                    } catch (e) {
                        return false;
                    }
                },
            });
        });
    });
    
    // Random think time before next user journey
    sleep(Math.random() * 2 + 1);
}

// =============================================================================
// TEARDOWN
// =============================================================================
export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('🏁 BROWSING LOAD TEST COMPLETED');
    console.log('='.repeat(60));
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    console.log(`🎬 Movies in database: ${data.movieCount}`);
    console.log('');
    console.log('📈 Key metrics to review:');
    console.log('   - movies_list_duration (should be <500ms p95)');
    console.log('   - seatmap_load_duration (should be <800ms p95)');
    console.log('   - http_req_failed (should be <1%)');
    console.log('   - page_views (total pages served)');
    console.log('='.repeat(60));
}
