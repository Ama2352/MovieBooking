// k6-tests/scenarios/01-seat-lock-concurrency.js
// =============================================================================
// SEAT LOCK CONCURRENCY TEST
// =============================================================================
// Purpose: Test Redis distributed lock under high concurrency
// Critical Flow: Multiple VUs trying to lock the same seats simultaneously
// Expected: Redis should prevent double-booking, graceful conflict handling
// =============================================================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { CONFIG, HEADERS, fetchK6TestData, generateSessionId } from '../config/config.js';

// =============================================================================
// CUSTOM METRICS
// =============================================================================
const lockSuccessCounter = new Counter('seat_lock_success');
const lockConflictCounter = new Counter('seat_lock_conflict');
const lockErrorCounter = new Counter('seat_lock_error');
const lockDuration = new Trend('seat_lock_duration_ms');
const lockSuccessRate = new Rate('seat_lock_success_rate');

// =============================================================================
// COMMAND TO RUN TEST WITH PROMETHEUS OUTPUT
// =============================================================================
// $env:K6_PROMETHEUS_REMOTE_WRITE_URL="http://localhost:9090/api/v1/write"; k6 run --out experimental-prometheus-rw 01-seat-lock-concurrency.js

// =============================================================================
// TEST OPTIONS
// =============================================================================
export const options = {
    scenarios: {
        // Main concurrency test
        seat_lock_concurrency: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 30 },   // (1) Warm up to 30 VUs
                { duration: '30s', target: 100 },  // (2) Push to 100 VUs
                { duration: '40s', target: 200 },  // (3) Push to 200 VUs
                { duration: '40s', target: 300 },  // (4) Peak at 300 VUs
                { duration: '30s', target: 300 },  // (5) Sustain peak
                { duration: '30s', target: 0 },    // (6) Ramp down
            ]
        },
    },
    thresholds: {
        // Performance thresholds
        'http_req_duration{name:lock_seats}': ['p(95)<2000', 'p(99)<4000'],
        'http_req_duration{name:release_seats}': ['p(95)<1000'],
        
        // Business metrics
        'seat_lock_success': ['count>50'],           // At least 50 successful locks
        'seat_lock_success_rate': ['rate>0.3'],      // At least 30% success (conflicts expected)
        
        // Error rate
        'http_req_failed': ['rate<0.3'],             // Allow up to 30% failures (conflicts)
    },
};

// =============================================================================
// SETUP - Fetch test data dynamically
// =============================================================================
export function setup() {
    console.log(`🎬 Starting Seat Lock Concurrency Test`);
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    
    // Fetch K6 test data (movie, showtime) dynamically
    const testData = fetchK6TestData();
    if (!testData) {
        console.error('❌ Failed to fetch K6 test data. Ensure K6_SEED_ENABLED=true');
        return { availableSeats: [], ticketTypes: [], showtimeId: null };
    }
    
    const showtimeId = testData.showtimeId;
    console.log(`🎫 Showtime: ${showtimeId}`);
    
    // Fetch ticket types
    const ticketTypesRes = http.get(
        `${CONFIG.BASE_URL}/ticket-types?showtimeId=${showtimeId}`,
        { headers: HEADERS }
    );
    
    if (ticketTypesRes.status !== 200) {
        console.error(`❌ Failed to fetch ticket types: ${ticketTypesRes.status}`);
        return { ticketTypes: [], showtimeId };
    }
    
    const ticketTypes = JSON.parse(ticketTypesRes.body);
    console.log(`✅ Found ${ticketTypes.length} ticket types`);
    
    return { 
        ticketTypes,
        showtimeId,
    };
}

// =============================================================================
// MAIN TEST FUNCTION
// =============================================================================
export default function(data) {
    // Skip if no test data
    if (!data.ticketTypes || data.ticketTypes.length === 0) {
        console.error('No ticket types - skipping iteration');
        sleep(1);
        return;
    }
    
    // Generate unique session for this VU
    const sessionId = generateSessionId();
    const headers = {
        ...HEADERS,
        'X-Session-Id': sessionId,
    };
    
    group('Seat Lock Concurrency Test', function() {
        
        // === STEP 1: Fetch Fresh Available Seats ===
        const seatsRes = http.get(
            `${CONFIG.BASE_URL}/showtime-seats/showtime/${data.showtimeId}/available`,
            { 
                headers,
                tags: { name: 'get_available_seats' }
            }
        );

        if (seatsRes.status !== 200) {
            lockErrorCounter.add(1);
            return;
        }

        const availableSeats = JSON.parse(seatsRes.body);
        
        if (availableSeats.length === 0) {
            // No seats available - this is a valid "conflict" state in a concurrency test
            lockConflictCounter.add(1);
            return;
        }

        // === STEP 2: Select random seats ===
        // Pick 1-3 random seats (simulates real user behavior)
        const numSeats = Math.min(Math.floor(Math.random() * 3) + 1, availableSeats.length);
        const selectedSeats = [];
        const usedIndexes = new Set();
        
        for (let i = 0; i < numSeats; i++) {
            let idx;
            do {
                idx = Math.floor(Math.random() * availableSeats.length);
            } while (usedIndexes.has(idx));
            
            usedIndexes.add(idx);
            const seat = availableSeats[idx];
            const ticketType = data.ticketTypes[Math.floor(Math.random() * data.ticketTypes.length)];
            
            selectedSeats.push({
                showtimeSeatId: seat.showtimeSeatId,
                ticketTypeId: ticketType.ticketTypeId
            });
        }
        
        // === STEP 3: Attempt to lock seats ===
        const lockPayload = JSON.stringify({
            showtimeId: data.showtimeId,
            seats: selectedSeats
        });
        
        const startTime = Date.now();
        
        const lockRes = http.post(
            `${CONFIG.BASE_URL}/seat-locks`,
            lockPayload,
            { 
                headers, 
                tags: { name: 'lock_seats' },
                timeout: '10s'
            }
        );
        
        const duration = Date.now() - startTime;
        lockDuration.add(duration);
        
        // === STEP 4: Process result ===
        if (lockRes.status === 201) {
            // SUCCESS: Lock acquired
            lockSuccessCounter.add(1);
            lockSuccessRate.add(1);
            
            const lockData = JSON.parse(lockRes.body);
            
            check(lockRes, {
                'lock has lockId': (r) => lockData.lockId !== undefined,
                'lock has expiresAt': (r) => lockData.expiresAt !== undefined,
                'lock has correct seat count': (r) => lockData.lockedSeats?.length === selectedSeats.length,
            });
            
            // Simulate user thinking time before next action
            sleep(Math.random() * 3 + 1);
            
            // === STEP 5: Release lock (cleanup for other VUs) ===
            const releaseRes = http.del(
                `${CONFIG.BASE_URL}/seat-locks/showtime/${data.showtimeId}`,
                null,
                { 
                    headers, 
                    tags: { name: 'release_seats' } 
                }
            );
            
            check(releaseRes, {
                'release successful': (r) => r.status === 200,
            });
            
        } else if (lockRes.status === 409 || lockRes.status === 423) {
            // EXPECTED: Conflict - seats already locked by another VU
            lockConflictCounter.add(1);
            lockSuccessRate.add(0);
            
            check(lockRes, {
                'conflict response valid': (r) => r.status === 409 || r.status === 423,
            });
            
        } else {
            // UNEXPECTED: Error
            lockErrorCounter.add(1);
            lockSuccessRate.add(0);
            
            console.error(`Unexpected response: ${lockRes.status} - ${lockRes.body}`);
        }
    });
    
    // Think time between iterations
    sleep(Math.random() * 2 + 0.5);
}

// =============================================================================
// TEARDOWN - Print summary
// =============================================================================
export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('🏁 SEAT LOCK CONCURRENCY TEST COMPLETED');
    console.log('='.repeat(60));
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    console.log(`🎫 Showtime: ${data.showtimeId}`);
    console.log(`📊 Available seats at start: ${data.availableSeats?.length || 0}`);
    console.log('');
    console.log('📈 Check k6 output above for:');
    console.log('   - seat_lock_success (total successful locks)');
    console.log('   - seat_lock_conflict (expected conflicts)');
    console.log('   - seat_lock_error (unexpected errors)');
    console.log('   - seat_lock_success_rate (success ratio)');
    console.log('='.repeat(60));
}
