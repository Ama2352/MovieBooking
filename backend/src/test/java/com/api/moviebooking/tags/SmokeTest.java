package com.api.moviebooking.tags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * SMOKE TEST - Quick verification that critical system functions work
 * 
 * Purpose: Verify build stability and core functionality
 * When to run: After every build, before deployment
 * Duration: 5-10 minutes
 * 
 * Covers:
 * - Critical user flows (login, booking, payment)
 * - Essential API endpoints
 * - Database connectivity
 * - Basic integration points
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("smoke")
public @interface SmokeTest {
}
