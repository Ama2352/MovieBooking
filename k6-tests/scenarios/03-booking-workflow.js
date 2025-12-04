// k6-tests/scenarios/03-booking-workflow.js
// =============================================================================
// COMPLETE BOOKING WORKFLOW TEST
// =============================================================================
// Purpose: Test full booking flow end-to-end
// Flow: Lock → Preview → Confirm → (Mock) Payment
// Expected: Validate entire booking pipeline under load
// =============================================================================

import http from 'k6/http';
import { check, sleep, group, fail } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';

// Configuration
const CONFIG = {
    BASE_URL: __ENV.API_URL || 'http://localhost:8080',
    TEST_SHOWTIME_ID: __ENV.SHOWTIME_ID || 'd1000000-0000-0000-0000-000000000001',
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

// Custom metrics
const bookingStarted = new Counter('booking_started');
const bookingCompleted = new Counter('booking_completed');
const bookingFailed = new Counter('booking_failed');
const bookingConflict = new Counter('booking_conflict');

const lockStepDuration = new Trend('step_lock_duration');
const previewStepDuration = new Trend('step_preview_duration');
const confirmStepDuration = new Trend('step_confirm_duration');
const totalBookingDuration = new Trend('total_booking_duration');

const bookingSuccessRate = new Rate('booking_success_rate');

// =============================================================================
// TEST OPTIONS
// =============================================================================
export const options = {
    scenarios: {
        booking_workflow: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 15 },   // Start slow
                { duration: '30s', target: 30 },   // Normal load
                { duration: '40s', target: 50 },   // Peak at 50 VUs
                { duration: '30s', target: 30 },   // Wind down
                { duration: '20s', target: 0 },    // Cool down
            ],
        },
    },
    thresholds: {
        // Step latencies
        'step_lock_duration': ['p(95)<2000'],
        'step_preview_duration': ['p(95)<1000'],
        'step_confirm_duration': ['p(95)<3000'],
        'total_booking_duration': ['p(95)<8000'],
        
        // Business metrics
        'booking_completed': ['count>10'],           // At least 10 completed bookings
        'booking_success_rate': ['rate>0.3'],        // At least 30% success rate
        
        // Error rate (some conflicts expected)
        'http_req_failed': ['rate<0.3'],
    },
};

// =============================================================================
// SETUP
// =============================================================================
export function setup() {
    console.log(`🎬 Starting Booking Workflow Test`);
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    console.log(`🎫 Showtime: ${CONFIG.TEST_SHOWTIME_ID}`);
    
    // Fetch available seats
    const seatsRes = http.get(
        `${CONFIG.BASE_URL}/showtime-seats/showtime/${CONFIG.TEST_SHOWTIME_ID}/available`,
        { headers: HEADERS }
    );
    
    if (seatsRes.status !== 200) {
        console.error(`❌ Failed to fetch seats: ${seatsRes.status}`);
        return { availableSeats: [], ticketTypes: [] };
    }
    
    const availableSeats = JSON.parse(seatsRes.body);
    console.log(`✅ Found ${availableSeats.length} available seats`);
    
    // Fetch ticket types
    const ticketTypesRes = http.get(
        `${CONFIG.BASE_URL}/ticket-types?showtimeId=${CONFIG.TEST_SHOWTIME_ID}`,
        { headers: HEADERS }
    );
    
    const ticketTypes = ticketTypesRes.status === 200 ? JSON.parse(ticketTypesRes.body) : [];
    console.log(`✅ Found ${ticketTypes.length} ticket types`);
    
    return { availableSeats, ticketTypes };
}

// =============================================================================
// MAIN TEST - Complete Booking Flow
// =============================================================================
export default function(data) {
    // Validate test data
    if (!data.availableSeats?.length || !data.ticketTypes?.length) {
        console.error('Missing test data - skipping');
        sleep(1);
        return;
    }
    
    const sessionId = generateSessionId();
    const headers = {
        ...HEADERS,
        'X-Session-Id': sessionId,
    };
    
    const bookingStartTime = Date.now();
    bookingStarted.add(1);
    
    let lockId = null;
    let bookingId = null;
    
    group('Complete Booking Flow', function() {
        
        // =============================================
        // STEP 1: LOCK SEATS
        // =============================================
        group('Step 1: Lock Seats', function() {
            const stepStart = Date.now();
            
            // Select 1-2 random seats
            const numSeats = Math.floor(Math.random() * 2) + 1;
            const selectedSeats = [];
            const usedIndexes = new Set();
            
            for (let i = 0; i < numSeats && i < data.availableSeats.length; i++) {
                let idx;
                do {
                    idx = Math.floor(Math.random() * data.availableSeats.length);
                } while (usedIndexes.has(idx));
                usedIndexes.add(idx);
                
                const seat = data.availableSeats[idx];
                const ticketType = data.ticketTypes[Math.floor(Math.random() * data.ticketTypes.length)];
                
                selectedSeats.push({
                    showtimeSeatId: seat.showtimeSeatId,
                    ticketTypeId: ticketType.id
                });
            }
            
            const lockPayload = JSON.stringify({
                showtimeId: CONFIG.TEST_SHOWTIME_ID,
                seats: selectedSeats
            });
            
            const lockRes = http.post(
                `${CONFIG.BASE_URL}/seat-locks`,
                lockPayload,
                { 
                    headers, 
                    tags: { name: 'workflow_lock_seats' },
                    timeout: '10s'
                }
            );
            
            lockStepDuration.add(Date.now() - stepStart);
            
            if (lockRes.status === 201) {
                const lockData = JSON.parse(lockRes.body);
                lockId = lockData.lockId;
                
                check(lockRes, {
                    'lock: status 201': (r) => r.status === 201,
                    'lock: has lockId': (r) => lockData.lockId !== undefined,
                });
            } else if (lockRes.status === 409 || lockRes.status === 423) {
                bookingConflict.add(1);
                bookingFailed.add(1);
                bookingSuccessRate.add(0);
                return; // Exit early - conflict
            } else {
                console.error(`Lock failed: ${lockRes.status} - ${lockRes.body}`);
                bookingFailed.add(1);
                bookingSuccessRate.add(0);
                return;
            }
        });
        
        if (!lockId) return;
        
        // User thinks about selection
        sleep(Math.random() * 2 + 1);
        
        // =============================================
        // STEP 2: PRICE PREVIEW
        // =============================================
        group('Step 2: Price Preview', function() {
            const stepStart = Date.now();
            
            const previewPayload = JSON.stringify({
                lockId: lockId,
                snacks: []  // No snacks for load test simplicity
            });
            
            const previewRes = http.post(
                `${CONFIG.BASE_URL}/bookings/price-preview`,
                previewPayload,
                { 
                    headers, 
                    tags: { name: 'workflow_price_preview' } 
                }
            );
            
            previewStepDuration.add(Date.now() - stepStart);
            
            if (previewRes.status === 200) {
                const previewData = JSON.parse(previewRes.body);
                
                check(previewRes, {
                    'preview: status 200': (r) => r.status === 200,
                    'preview: has total': (r) => previewData.total !== undefined,
                    'preview: has subtotal': (r) => previewData.subtotal !== undefined,
                });
            } else {
                console.error(`Preview failed: ${previewRes.status}`);
            }
        });
        
        // User reviews price
        sleep(Math.random() * 2 + 1);
        
        // =============================================
        // STEP 3: CONFIRM BOOKING (as guest)
        // =============================================
        group('Step 3: Confirm Booking', function() {
            const stepStart = Date.now();
            
            const timestamp = Date.now();
            const random = Math.random().toString(36).substring(2, 8);
            
            const confirmPayload = JSON.stringify({
                lockId: lockId,
                guestInfo: {
                    email: `k6_${timestamp}_${random}@loadtest.local`,
                    username: `K6 User ${random}`,
                    phoneNumber: '+84900000000'
                },
                snackCombos: []
            });
            
            const confirmRes = http.post(
                `${CONFIG.BASE_URL}/bookings/confirm`,
                confirmPayload,
                { 
                    headers, 
                    tags: { name: 'workflow_confirm_booking' },
                    timeout: '15s'
                }
            );
            
            confirmStepDuration.add(Date.now() - stepStart);
            
            if (confirmRes.status === 200) {
                const bookingData = JSON.parse(confirmRes.body);
                bookingId = bookingData.bookingId;
                
                check(confirmRes, {
                    'confirm: status 200': (r) => r.status === 200,
                    'confirm: has bookingId': (r) => bookingData.bookingId !== undefined,
                    'confirm: status PENDING_PAYMENT': (r) => bookingData.status === 'PENDING_PAYMENT',
                    'confirm: has finalPrice': (r) => bookingData.finalPrice !== undefined,
                });
                
                bookingCompleted.add(1);
                bookingSuccessRate.add(1);
                
            } else {
                console.error(`Confirm failed: ${confirmRes.status} - ${confirmRes.body}`);
                bookingFailed.add(1);
                bookingSuccessRate.add(0);
                
                // Release lock on failure
                http.del(
                    `${CONFIG.BASE_URL}/seat-locks/showtime/${CONFIG.TEST_SHOWTIME_ID}`,
                    null,
                    { headers }
                );
            }
        });
        
        // =============================================
        // STEP 4: (Optional) Mock Payment
        // =============================================
        // Note: In real test with payment.mock.enabled=true,
        // you would call the payment endpoint here.
        // For now, we consider PENDING_PAYMENT as success.
        
        if (bookingId) {
            // Booking created successfully
            // In production, user would be redirected to payment gateway
            sleep(1);
        }
    });
    
    // Record total duration
    totalBookingDuration.add(Date.now() - bookingStartTime);
    
    // Think time before next iteration
    sleep(Math.random() * 3 + 2);
}

// =============================================================================
// TEARDOWN
// =============================================================================
export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('🏁 BOOKING WORKFLOW TEST COMPLETED');
    console.log('='.repeat(60));
    console.log(`📍 Target: ${CONFIG.BASE_URL}`);
    console.log(`🎫 Showtime: ${CONFIG.TEST_SHOWTIME_ID}`);
    console.log(`📊 Available seats at start: ${data.availableSeats?.length || 0}`);
    console.log('');
    console.log('📈 Key metrics to review:');
    console.log('   - booking_completed (successful bookings)');
    console.log('   - booking_conflict (expected conflicts)');
    console.log('   - booking_success_rate (completion ratio)');
    console.log('   - total_booking_duration (end-to-end latency)');
    console.log('   - step_*_duration (per-step latency)');
    console.log('='.repeat(60));
}
