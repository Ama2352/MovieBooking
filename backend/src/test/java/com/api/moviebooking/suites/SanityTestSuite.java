package com.api.moviebooking.suites;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * SANITY TEST SUITE
 * 
 * Validates specific functionality after changes
 * Execute: mvn test -Dgroups="sanity"
 * 
 * Test Coverage:
 * 1. Modified features verification
 * 2. Business logic validation
 * 3. Pricing calculations
 * 4. Promotion application
 * 5. Membership benefits
 * 6. Refund processing
 * 
 * Expected Duration: 10-15 minutes
 * Pass Criteria: 95%+ pass rate required
 */
@Suite
@SuiteDisplayName("Sanity Test Suite - Feature Validation")
@SelectPackages({
    "com.api.moviebooking.services",
    "com.api.moviebooking.integrations"
})
@IncludeTags("sanity")
public class SanityTestSuite {
    // Test configuration only - no code needed
}
