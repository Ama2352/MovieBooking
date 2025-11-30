package com.api.moviebooking.tags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * REGRESSION TEST - Comprehensive validation of all functionality
 * 
 * Purpose: Ensure no existing functionality is broken
 * When to run: Before major releases, nightly builds
 * Duration: 30-60 minutes
 * 
 * Covers:
 * - All functional test cases
 * - Integration scenarios
 * - Error handling
 * - Boundary conditions
 * - Performance validation
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("regression")
public @interface RegressionTest {
}
