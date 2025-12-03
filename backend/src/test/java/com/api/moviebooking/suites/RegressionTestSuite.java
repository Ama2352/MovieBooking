package com.api.moviebooking.suites;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * REGRESSION TEST SUITE
 * 
 * Comprehensive validation of all system functionality
 * Execute: mvn test -Dgroups="regression"
 * 
 * Test Coverage:
 * 1. All happy path scenarios
 * 2. Error handling & validation
 * 3. Edge cases & boundary conditions
 * 4. Concurrency & race conditions
 * 5. Integration scenarios
 * 6. Performance thresholds
 * 
 * Expected Duration: 30-60 minutes
 * Pass Criteria: 98%+ pass rate required for release
 */
@Suite
@SuiteDisplayName("Regression Test Suite - Full System Validation")
@SelectPackages({
    "com.api.moviebooking.services",
    "com.api.moviebooking.integrations"
})
@IncludeTags("regression")
public class RegressionTestSuite {
    // Test configuration only - no code needed
}
