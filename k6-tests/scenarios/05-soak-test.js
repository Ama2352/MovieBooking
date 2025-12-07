// k6-tests/scenarios/05-soak-test.js
// =============================================================================
// SOAK TEST (Endurance Test)
// =============================================================================
// Purpose: Test system stability over extended period
// Duration: 20 minutes with constant moderate load
// Expected: Find memory leaks, connection issues, gradual degradation
// =============================================================================
// $env:K6_PROMETHEUS_REMOTE_WRITE_URL="http://localhost:9090/api/v1/write"; k6 run --out experimental-prometheus-rw 05-soak-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend, Rate, Gauge } from 'k6/metrics';
import { fetchK6TestData } from '../config/config.js';

// Configuration
const CONFIG = {
    BASE_URL: __ENV.API_URL || 'http://localhost:8080',
};

const HEADERS = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
};

function generateSessionId() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// =============================================================================
// CUSTOM METRICS FOR SOAK ANALYSIS
// =============================================================================
const soakIterations = new Counter('soak_iterations');
const soakErrors = new Counter('soak_errors');
const soakSuccessRate = new Rate('soak_success_rate');

// Operation-specific metrics
const readOperations = new Counter('soak_read_operations');
const writeOperations = new Counter('soak_write_operations');
const readLatency = new Trend('soak_read_latency');
const writeLatency = new Trend('soak_write_latency');

// For detecting degradation over time
const iterationLatency = new Trend('soak_iteration_latency');

// =============================================================================
// TEST OPTIONS - 20 MINUTE SOAK
// =============================================================================
export const options = {
    scenarios: {
        soak_test: {
            executor: 'constant-vus',
            vus: 15,                    // Moderate, constant load
            duration: '20m',            // 20 minutes
        },
    },
    thresholds: {
        // Very low error rate expected in soak test
        'http_req_failed': ['rate<0.02'],          // Less than 2% errors
        'soak_errors': ['count<100'],              // Max 100 errors in 20 min
        
        // Latency should remain stable
        'soak_read_latency': ['p(99)<2000'],       // Reads always fast
        'soak_write_latency': ['p(99)<4000'],      // Writes stable
        'soak_iteration_latency': ['p(99)<5000'], // Overall iteration
        
        // High success rate
        'soak_success_rate': ['rate>0.95'],        // 95%+ success
    },
};

// =============================================================================
// SETUP
// =============================================================================
export function setup() {
    console.log(`⏱️  Starting SOAK TEST (20 minutes)`);
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    
    // Fetch K6 test data (movie, showtime) dynamically
    const testData = fetchK6TestData();
    if (!testData) {
        console.error('❌ Failed to fetch K6 test data. Ensure K6_SEED_ENABLED=true');
        return { availableSeats: [], ticketTypes: [], showtimeId: null, movieId: null };
    }
    
    const { showtimeId, movieId } = testData;
    console.log(`🎫 Showtime: ${showtimeId}`);
    console.log(`🎬 Movie: ${movieId}`);
    
    console.log(`👥 Virtual Users: 15 (constant)`);
    console.log('');
    console.log('🔍 This test monitors for:');
    console.log('   - Memory leaks (check JVM heap in Grafana)');
    console.log('   - Connection pool exhaustion');
    console.log('   - Gradual latency increase');
    console.log('   - Error rate trends');
    console.log('');
    console.log('📊 Open Grafana dashboard to monitor in real-time!');
    console.log('');
    
    // Verify system is healthy before starting soak
    const healthRes = http.get(`${CONFIG.BASE_URL}/actuator/health`);
    if (healthRes.status !== 200) {
        console.error(`❌ System not healthy: ${healthRes.status}`);
        console.error('Soak test may produce unreliable results');
    } else {
        console.log('✅ System health check passed');
    }
    
    // Get test data
    const seatsRes = http.get(
        `${CONFIG.BASE_URL}/showtime-seats/showtime/${showtimeId}/available`,
        { headers: HEADERS }
    );
    const availableSeats = seatsRes.status === 200 ? JSON.parse(seatsRes.body) : [];
    
    const ticketTypesRes = http.get(
        `${CONFIG.BASE_URL}/ticket-types?showtimeId=${showtimeId}`,
        { headers: HEADERS }
    );
    const ticketTypes = ticketTypesRes.status === 200 ? JSON.parse(ticketTypesRes.body) : [];
    
    console.log(`✅ Found ${availableSeats.length} seats, ${ticketTypes.length} ticket types`);
    console.log('');
    console.log('='.repeat(60));
    console.log('🚀 SOAK TEST STARTING NOW - 20 MINUTES');
    console.log('='.repeat(60));
    
    return { availableSeats, ticketTypes, showtimeId, movieId };
}

// =============================================================================
// MAIN TEST - Mixed Operations
// =============================================================================
export default function(data) {
    if (!data.ticketTypes?.length) {
        sleep(1);
        return;
    }

    const iterationStart = Date.now();
    soakIterations.add(1);
    
    const sessionId = generateSessionId();
    const headers = {
        ...HEADERS,
        'X-Session-Id': sessionId,
    };
    
    // Mix of operations to simulate realistic traffic
    const operation = Math.random();
    let success = false;
    
    group('Soak Test Iteration', function() {
        
        if (operation < 0.40) {
            // =============================================
            // 40%: READ - Get Seat Map (most common)
            // =============================================
            const startTime = Date.now();
            readOperations.add(1);
            
            const res = http.get(
                `${CONFIG.BASE_URL}/showtime-seats/showtime/${data.showtimeId}`,
                { 
                    headers, 
                    tags: { name: 'soak_seatmap' },
                    timeout: '10s'
                }
            );
            
            readLatency.add(Date.now() - startTime);
            
            success = check(res, {
                'soak seatmap: status 200': (r) => r.status === 200,
            });
            
        } else if (operation < 0.60) {
            // =============================================
            // 20%: READ - Get Movies
            // =============================================
            const startTime = Date.now();
            readOperations.add(1);
            
            const res = http.get(
                `${CONFIG.BASE_URL}/movies`,
                { 
                    headers, 
                    tags: { name: 'soak_movies' },
                    timeout: '10s'
                }
            );
            
            readLatency.add(Date.now() - startTime);
            
            success = check(res, {
                'soak movies: status 200': (r) => r.status === 200,
            });
            
        } else if (operation < 0.80) {
            // =============================================
            // 20%: READ - Check Availability
            // =============================================
            const startTime = Date.now();
            readOperations.add(1);
            
            const res = http.get(
                `${CONFIG.BASE_URL}/seat-locks/availability/${data.showtimeId}`,
                { 
                    headers, 
                    tags: { name: 'soak_availability' },
                    timeout: '10s'
                }
            );
            
            readLatency.add(Date.now() - startTime);
            
            success = check(res, {
                'soak availability: status 200': (r) => r.status === 200,
            });
            
        } else if (operation < 0.95) {
            // =============================================
            // 15%: WRITE - Lock & Release (stress Redis)
            // =============================================
            if (!data.availableSeats?.length || !data.ticketTypes?.length) {
                success = true; // Skip gracefully
            } else {
                const startTime = Date.now();
                writeOperations.add(1);
                
                // Get fresh available seats
                const seatsRes = http.get(
                    `${CONFIG.BASE_URL}/showtime-seats/showtime/${data.showtimeId}/available`,
                    { headers }
                );
                
                if (seatsRes.status === 200) {
                    const seats = JSON.parse(seatsRes.body);
                    
                    if (seats.length > 0) {
                        const idx = Math.floor(Math.random() * seats.length);
                        const ticketType = data.ticketTypes[0];
                        
                        const lockPayload = JSON.stringify({
                            showtimeId: data.showtimeId,
                            seats: [{
                                showtimeSeatId: seats[idx].showtimeSeatId,
                                ticketTypeId: ticketType.ticketTypeId
                            }]
                        });
                        
                        const lockRes = http.post(
                            `${CONFIG.BASE_URL}/seat-locks`,
                            lockPayload,
                            { 
                                headers, 
                                tags: { name: 'soak_lock' },
                                timeout: '10s'
                            }
                        );
                        
                        // Release immediately
                        if (lockRes.status === 201) {
                            http.del(
                                `${CONFIG.BASE_URL}/seat-locks/showtime/${data.showtimeId}`,
                                null,
                                { headers, tags: { name: 'soak_release' } }
                            );
                            success = true;
                        } else if (lockRes.status === 409 || lockRes.status === 423) {
                            // Conflict is OK in soak test
                            success = true;
                        }
                    } else {
                        success = true; // No seats, but not an error
                    }
                }
                
                writeLatency.add(Date.now() - startTime);
            }
            
        } else {
            // =============================================
            // 5%: READ - Get Showtimes
            // =============================================
            const startTime = Date.now();
            readOperations.add(1);
            
            const res = http.get(
                `${CONFIG.BASE_URL}/showtimes/movie/${data.movieId}/upcoming`,
                { 
                    headers, 
                    tags: { name: 'soak_showtimes' },
                    timeout: '10s'
                }
            );
            
            readLatency.add(Date.now() - startTime);
            
            success = check(res, {
                'soak showtimes: status 200': (r) => r.status === 200,
            });
        }
    });
    
    // Record metrics
    iterationLatency.add(Date.now() - iterationStart);
    
    if (success) {
        soakSuccessRate.add(1);
    } else {
        soakErrors.add(1);
        soakSuccessRate.add(0);
    }
    
    // Consistent think time for predictable load
    sleep(Math.random() * 2 + 2); // 2-4 seconds
}

// =============================================================================
// TEARDOWN
// =============================================================================
export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('🏁 SOAK TEST COMPLETED (20 minutes)');
    console.log('='.repeat(60));
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    console.log('');
    console.log('📈 Key metrics to analyze:');
    console.log('');
    console.log('1️⃣  Error Rate Trend:');
    console.log('   - soak_errors: Should be low and stable');
    console.log('   - No upward trend = system stable');
    console.log('');
    console.log('2️⃣  Latency Stability:');
    console.log('   - soak_iteration_latency: Should remain flat');
    console.log('   - Increasing trend = memory leak or pool exhaustion');
    console.log('');
    console.log('3️⃣  In Grafana, check:');
    console.log('   - JVM Heap usage (should not continuously grow)');
    console.log('   - PostgreSQL connection pool (should be stable)');
    console.log('   - Redis memory (should be bounded)');
    console.log('   - Container CPU/Memory (should be stable)');
    console.log('');
    console.log('⚠️  WARNING SIGNS:');
    console.log('   - Latency increasing over time');
    console.log('   - Error rate climbing');
    console.log('   - Memory usage climbing without GC');
    console.log('   - Connection pool warnings in logs');
    console.log('='.repeat(60));
}
