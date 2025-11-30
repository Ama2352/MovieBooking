package com.api.moviebooking.tags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * SANITY TEST - Verify specific functionality after code changes
 * 
 * Purpose: Quick check after bug fixes or minor changes
 * When to run: After specific feature changes or bug fixes
 * Duration: 10-15 minutes
 * 
 * Covers:
 * - Recently modified features
 * - Related dependent features
 * - Business logic validation
 * - Edge cases for new code
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("sanity")
public @interface SanityTest {
}
