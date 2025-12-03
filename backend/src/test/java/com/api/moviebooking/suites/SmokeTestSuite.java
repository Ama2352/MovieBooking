package com.api.moviebooking.suites;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * SMOKE TEST SUITE
 * 
 * Runs critical path tests to verify system is stable
 * Execute: mvn test -Dgroups="smoke"
 * 
 * Test Coverage:
 * 1. Authentication Flow (Login/Register)
 * 2. Movie Browsing (List movies, view details)
 * 3. Seat Selection & Locking
 * 4. Booking Creation
 * 5. Payment Processing
 * 6. QR Code Generation
 * 
 * Expected Duration: 5-10 minutes
 * Pass Criteria: 100% pass rate required for deployment
 */
@Suite
@SuiteDisplayName("Smoke Test Suite - Critical Path Validation")
@SelectPackages({
    "com.api.moviebooking.services",
    "com.api.moviebooking.integrations"
})
@IncludeTags("smoke")
public class SmokeTestSuite {
    // Test configuration only - no code needed
}
