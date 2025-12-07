// k6-tests/scenarios/04-spike-test.js
// =============================================================================
// SPIKE TEST
// =============================================================================
// Purpose: Test system behavior under sudden traffic surge
// Scenario: Flash sale - many users rush to book at the same time
// Expected: System should handle spike gracefully, recover quickly
// =============================================================================
// $env:K6_PROMETHEUS_REMOTE_WRITE_URL="http://localhost:9090/api/v1/write"; k6 run --out experimental-prometheus-rw 04-spike-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { CONFIG, HEADERS, fetchK6TestData } from '../config/config.js';

function generateSessionId() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Custom metrics for spike analysis
const spikeRequests = new Counter('spike_requests');
const spikeSuccesses = new Counter('spike_successes');
const spikeFailures = new Counter('spike_failures');
const spikeLatency = new Trend('spike_latency');
const spikeSuccessRate = new Rate('spike_success_rate');

// Phase tracking (for analysis)
const phaseBaseline = new Counter('phase_baseline_requests');
const phaseSurge = new Counter('phase_surge_requests');
const phaseRecovery = new Counter('phase_recovery_requests');

// =============================================================================
// TEST OPTIONS - SPIKE PATTERN
// =============================================================================
export const options = {
    scenarios: {
        spike_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                // Phase 1: BASELINE (normal traffic)
                { duration: '15s', target: 10 },   // Ramp to baseline
                { duration: '15s', target: 10 },   // Hold baseline
                
                // Phase 2: SPIKE! (sudden surge) 🚀
                { duration: '10s', target: 100 },  // Spike to 100 VUs in 10s!
                { duration: '30s', target: 100 },  // Hold at spike level
                
                // Phase 3: DROP (sudden decrease)
                { duration: '5s', target: 10 },    // Quick drop back to baseline
                
                // Phase 4: RECOVERY (monitor recovery)
                { duration: '30s', target: 10 },   // Recovery period
                
                // Phase 5: COOL DOWN
                { duration: '15s', target: 0 },    // Ramp down
            ],
        },
    },
    thresholds: {
        // Allow higher error rate during spike (system under stress)
        'http_req_failed': ['rate<0.25'],          // Max 25% failures
        
        // Latency expectations (relaxed during spike)
        'spike_latency': ['p(95)<5000'],           // 95% under 5s even during spike
        'http_req_duration': ['p(99)<8000'],       // 99% under 8s
        
        // Business metric
        'spike_success_rate': ['rate>0.5'],        // At least 50% success during spike
    },
};

// =============================================================================
// SETUP
// =============================================================================
export function setup() {
    console.log(`🚀 Starting SPIKE TEST`);
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    
    // Fetch K6 test data (movie, showtime) dynamically
    const testData = fetchK6TestData();
    if (!testData) {
        console.error('❌ Failed to fetch K6 test data. Ensure K6_SEED_ENABLED=true');
        return { availableSeats: [], ticketTypes: [], showtimeId: null };
    }
    
    const showtimeId = testData.showtimeId;
    console.log(`🎫 Showtime: ${showtimeId}`);
    
    console.log('');
    console.log('⚠️  SPIKE PATTERN:');
    console.log('   0-30s:  Baseline (10 VUs)');
    console.log('   30-40s: SPIKE! (10 → 100 VUs in 10s)');
    console.log('   40-70s: Sustained spike (100 VUs)');
    console.log('   70-75s: Drop (100 → 10 VUs)');
    console.log('   75-105s: Recovery monitoring');
    console.log('');
    
    // Fetch test data
    const seatsRes = http.get(
        `${CONFIG.BASE_URL}/showtime-seats/showtime/${showtimeId}/available`,
        { headers: HEADERS }
    );
    
    const availableSeats = seatsRes.status === 200 ? JSON.parse(seatsRes.body) : [];
    console.log(`✅ Found ${availableSeats.length} available seats`);
    
    const ticketTypesRes = http.get(
        `${CONFIG.BASE_URL}/ticket-types?showtimeId=${showtimeId}`,
        { headers: HEADERS }
    );
    
    const ticketTypes = ticketTypesRes.status === 200 ? JSON.parse(ticketTypesRes.body) : [];
    console.log(`✅ Found ${ticketTypes.length} ticket types`);
    
    return { ticketTypes, showtimeId };
}

// =============================================================================
// MAIN TEST - Spike Scenario
// =============================================================================
export default function(data) {
    if (!data.ticketTypes?.length) {
        sleep(1);
        return;
    }
    
    const sessionId = generateSessionId();
    const headers = {
        ...HEADERS,
        'X-Session-Id': sessionId,
    };
    
    spikeRequests.add(1);
    
    // Determine which phase we're in based on time
    // (Note: k6 doesn't have direct access to scenario time,
    // so we use VU count as proxy)
    const vuCount = __VU;
    
    group('Spike Test - Seat Lock Rush', function() {
        const startTime = Date.now();
        
        // =============================================
        // STEP 1: Get Available Seats (fast read)
        // =============================================
        const seatsRes = http.get(
            `${CONFIG.BASE_URL}/showtime-seats/showtime/${data.showtimeId}/available`,
            { 
                headers, 
                tags: { name: 'spike_get_seats' },
                timeout: '10s'
            }
        );
        
        if (seatsRes.status !== 200) {
            spikeFailures.add(1);
            spikeSuccessRate.add(0);
            spikeLatency.add(Date.now() - startTime);
            return;
        }
        
        const seats = JSON.parse(seatsRes.body);
        if (seats.length === 0) {
            // All seats taken - expected during spike
            spikeSuccessRate.add(0);
            spikeLatency.add(Date.now() - startTime);
            sleep(0.5);
            return;
        }
        
        // =============================================
        // STEP 2: Attempt to Lock First Available Seat
        // =============================================
        // Pick a random seat from available ones
        const randomIndex = Math.floor(Math.random() * seats.length);
        const selectedSeat = seats[randomIndex];
        const ticketType = data.ticketTypes[0];
        
        const lockPayload = JSON.stringify({
            showtimeId: data.showtimeId,
            seats: [{
                showtimeSeatId: selectedSeat.showtimeSeatId,
                ticketTypeId: ticketType.ticketTypeId
            }]
        });
        
        const lockRes = http.post(
            `${CONFIG.BASE_URL}/seat-locks`,
            lockPayload,
            { 
                headers, 
                tags: { name: 'spike_lock_seat' },
                timeout: '10s'
            }
        );
        
        const totalLatency = Date.now() - startTime;
        spikeLatency.add(totalLatency);
        
        // =============================================
        // STEP 3: Process Result
        // =============================================
        if (lockRes.status === 201) {
            // SUCCESS - got the lock!
            spikeSuccesses.add(1);
            spikeSuccessRate.add(1);
            
            check(lockRes, {
                'spike lock: status 201': (r) => r.status === 201,
                'spike lock: has lockId': (r) => JSON.parse(r.body).lockId !== undefined,
            });
            
            // Hold lock briefly then release (for other VUs)
            sleep(Math.random() * 0.5 + 0.3);
            
            // Release the lock
            http.del(
                `${CONFIG.BASE_URL}/seat-locks/showtime/${data.showtimeId}`,
                null,
                { 
                    headers, 
                    tags: { name: 'spike_release' } 
                }
            );
            
        } else if (lockRes.status === 409 || lockRes.status === 423) {
            // EXPECTED during spike - conflict
            spikeSuccessRate.add(0);
            
            check(lockRes, {
                'spike conflict: valid response': (r) => r.status === 409 || r.status === 423,
            });
            
        } else {
            // Unexpected error
            spikeFailures.add(1);
            spikeSuccessRate.add(0);
            
            console.error(`Spike error: ${lockRes.status} - ${lockRes.body}`);
        }
    });
    
    // Minimal think time during spike (users are impatient!)
    sleep(Math.random() * 0.5 + 0.2);
}

// =============================================================================
// TEARDOWN
// =============================================================================
export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('🏁 SPIKE TEST COMPLETED');
    console.log('='.repeat(60));
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    console.log('');
    console.log('📈 Key metrics to analyze:');
    console.log('   - spike_success_rate: Did system handle spike gracefully?');
    console.log('   - spike_latency p95/p99: How much did latency increase?');
    console.log('   - http_req_failed rate: Error rate during spike?');
    console.log('   - Check Grafana for recovery time after spike');
    console.log('');
    console.log('🔍 What to look for:');
    console.log('   1. Latency spike during 30-70s (expected)');
    console.log('   2. Error rate during spike (should be <25%)');
    console.log('   3. Recovery speed after 75s (should stabilize quickly)');
    console.log('   4. No cascading failures after spike');
    console.log('='.repeat(60));
}
