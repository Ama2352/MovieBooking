// k6-tests/scenarios/02-browsing-load.js
// =============================================================================
// BROWSING LOAD TEST
// =============================================================================
// Purpose: Test read-heavy endpoints under high user load
// Simulates: Users browsing movies, viewing showtimes, checking seat maps
// Expected: Fast response times, zero errors for read operations
// =============================================================================

// $env:K6_PROMETHEUS_REMOTE_WRITE_URL="http://localhost:9090/api/v1/write"; k6 run --out experimental-prometheus-rw 02-browsing-load.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { CONFIG, HEADERS, fetchK6TestData } from '../config/config.js';

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
            // Example stages for 1000 VUs
            stages: [
                { duration: '30s', target: 200 },  // 1. Warm up (Ramp to 200)
                { duration: '1m', target: 500 },   // 2. Medium Load (Ramp to 500)
                { duration: '2m', target: 1000 },  // 3. Peak Load (Ramp to 1000)
                { duration: '1m', target: 1000 },  // 4. Hold Peak Load (Sustain 1000 VUs for 1 min)
                { duration: '30s', target: 0 },    // 5. Cool down
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
    
    // Fetch K6 test data (movie, showtime) dynamically
    const testData = fetchK6TestData();
    if (!testData) {
        console.error('❌ Failed to fetch K6 test data. Ensure K6_SEED_ENABLED=true');
        return { movieCount: 0, movieId: null, showtimeId: null };
    }
    
    return { 
        movieCount: 1,
        movieId: testData.movieId,
        showtimeId: testData.showtimeId
    };
}

// =============================================================================
// MAIN TEST - User Browsing Journey
// =============================================================================
export default function(data) {
    // Skip if no test data
    if (!data.movieId || !data.showtimeId) {
        console.error('No test data - skipping iteration');
        sleep(1);
        return;
    }
    
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
                `${CONFIG.BASE_URL}/showtimes/movie/${data.movieId}/upcoming`,
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
                `${CONFIG.BASE_URL}/showtime-seats/showtime/${data.showtimeId}/available`,
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
                `${CONFIG.BASE_URL}/ticket-types?showtimeId=${data.showtimeId}`,
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
            // Note: This endpoint might not exist or might be different. 
            // Using the seat map endpoint again as a proxy for checking availability
            const res = http.get(
                `${CONFIG.BASE_URL}/showtime-seats/showtime/${data.showtimeId}/available`,
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
                        return Array.isArray(data);
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
