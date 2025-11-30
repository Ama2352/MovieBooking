# Test Tagging Guide - Hướng dẫn đánh tag cho test cases

## Đã tạo sẵn
✅ 3 annotation classes: `@SmokeTest`, `@SanityTest`, `@RegressionTest`
✅ 3 test suites: `SmokeTestSuite`, `SanityTestSuite`, `RegressionTestSuite`
✅ 3 scripts chạy test: `run-smoke-tests.ps1`, `run-sanity-tests.ps1`, `run-regression-tests.ps1`
✅ Documentation: `TEST_STRATEGY.md`

## Cách áp dụng tags

### Bước 1: Import annotations vào test class
```java
import com.api.moviebooking.tags.SmokeTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.RegressionTest;
```

### Bước 2: Thêm annotation trước mỗi test method

**Ví dụ test QUAN TRỌNG (Critical Path)**:
```java
@Test
@SmokeTest          // Chạy trong smoke test
@SanityTest         // Chạy trong sanity test
@RegressionTest     // Chạy trong regression test
@DisplayName("Should successfully complete booking")
void testCompleteBooking_Success() {
    // Test implementation
}
```

**Ví dụ test BUSINESS LOGIC**:
```java
@Test
@SanityTest         // Chỉ chạy trong sanity và regression
@RegressionTest
@DisplayName("Should apply promotion discount correctly")
void testPromotionDiscount() {
    // Test implementation
}
```

**Ví dụ test EDGE CASE**:
```java
@Test
@RegressionTest     // Chỉ chạy trong regression
@DisplayName("Should handle concurrent lock expiration")
void testConcurrentLockExpiration() {
    // Test implementation
}
```

---

## Checklist: Test files cần tag

### 🔥 SMOKE TESTS (20 tests) - Critical Path Only

#### UserServiceTest.java
- [ ] `testRegister_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testLogin_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testRefreshAccessToken_Success` → @SmokeTest @SanityTest @RegressionTest

#### MovieServiceTest.java
- [ ] `testGetAllMovies_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testGetMovieById_Success` → @SmokeTest @SanityTest @RegressionTest

#### BookingServiceTest.java
- [ ] `testLockSeats_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testConfirmBooking_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testReleaseSeats_Success` → @SmokeTest @RegressionTest

#### ShowtimeServiceTest.java
- [ ] `testGetAllShowtimes_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testGetShowtimeById_Success` → @SmokeTest @RegressionTest

#### CinemaServiceTest.java
- [ ] `testGetAllCinemas_Success` → @SmokeTest @SanityTest @RegressionTest

#### BookingIntegrationTest.java
- [ ] `testCompleteBookingFlow_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testSeatLockingFlow_Success` → @SmokeTest @RegressionTest

#### PaymentIntegrationTest.java
- [ ] `testCreateMoMoPayment_Success` → @SmokeTest @SanityTest @RegressionTest
- [ ] `testPaymentCallback_Success` → @SmokeTest @SanityTest @RegressionTest

#### CheckoutIntegrationTest.java
- [ ] `testCheckoutAndPayment_Success` → @SmokeTest @SanityTest @RegressionTest

---

### 🔍 SANITY TESTS (50 tests) - Business Logic & Modified Features

#### PromotionServiceTest.java - ALL TESTS
- [ ] Tag tất cả tests → @SanityTest @RegressionTest

#### PricingIntegrationTest.java - ALL TESTS
- [ ] Tag tất cả tests → @SanityTest @RegressionTest

#### RefundServiceTest.java - ALL TESTS
- [ ] Tag tất cả tests → @SanityTest @RegressionTest

#### ShowtimeSeatServiceTest.java
- [ ] `testGenerateShowtimeSeats_Success` → @SanityTest @RegressionTest
- [ ] `testRecalculatePrices_Success` → @SanityTest @RegressionTest
- [ ] `testUpdateShowtimeSeat_Success` → @SanityTest @RegressionTest

#### SeatServiceTest.java
- [ ] Happy path tests → @SanityTest @RegressionTest

#### MembershipTierIntegrationTest.java - ALL TESTS
- [ ] Tag tất cả tests → @SanityTest @RegressionTest

---

### 🔄 REGRESSION TESTS (150+ tests) - All Tests

#### Các test còn lại trong:
- [ ] **BookingServiceTest.java** - Error cases, edge cases → @RegressionTest
- [ ] **UserServiceTest.java** - Error cases → @RegressionTest
- [ ] **MovieServiceTest.java** - Error cases → @RegressionTest
- [ ] **ShowtimeServiceTest.java** - Error cases → @RegressionTest
- [ ] **CinemaServiceTest.java** - All remaining tests → @RegressionTest
- [ ] **RedisLockServiceTest.java** - ALL TESTS → @RegressionTest
- [ ] **SeatLockIntegrationTest.java** - ALL TESTS → @RegressionTest
- [ ] **SeatIntegrationTest.java** - ALL TESTS → @RegressionTest
- [ ] **ShowtimeIntegrationTest.java** - ALL TESTS → @RegressionTest
- [ ] **ShowtimeSeatIntegrationTest.java** - ALL TESTS → @RegressionTest
- [ ] **CinemaIntegrationTest.java** - ALL TESTS → @RegressionTest
- [ ] **MovieIntegrationTest.java** - ALL TESTS → @RegressionTest
- [ ] **UserIntegrationTest.java** - ALL TESTS → @RegressionTest
- [ ] **RefundIntegrationTest.java** - ALL TESTS → @RegressionTest

---

## Decision Matrix: Khi nào dùng tag nào?

| Test Type | @SmokeTest | @SanityTest | @RegressionTest |
|-----------|------------|-------------|-----------------|
| Critical user flow (login, booking, payment) | ✅ | ✅ | ✅ |
| Business logic validation (pricing, promotion) | ❌ | ✅ | ✅ |
| Error handling & validation | ❌ | ❌ | ✅ |
| Edge cases & race conditions | ❌ | ❌ | ✅ |
| Performance tests | ❌ | ❌ | ✅ |

---

## Cách chạy tests

### Option 1: Maven command
```bash
# Smoke tests
mvn test -Dgroups="smoke"

# Sanity tests
mvn test -Dgroups="sanity"

# Regression tests
mvn test -Dgroups="regression"

# Multiple tags
mvn test -Dgroups="smoke | sanity"
```

### Option 2: PowerShell scripts
```powershell
# Smoke tests
.\run-smoke-tests.ps1

# Sanity tests
.\run-sanity-tests.ps1

# Regression tests
.\run-regression-tests.ps1
```

### Option 3: IDE (IntelliJ IDEA)
1. Right click on test class/method
2. Run with tags: `smoke`, `sanity`, or `regression`

---

## Expected Results

### Smoke Tests
- **Count**: ~20 tests
- **Duration**: 5-10 minutes
- **Pass Rate**: Must be 100%
- **When**: After every build

### Sanity Tests
- **Count**: ~50 tests
- **Duration**: 10-15 minutes
- **Pass Rate**: Should be 95%+
- **When**: After feature changes

### Regression Tests
- **Count**: ~150 tests
- **Duration**: 30-60 minutes
- **Pass Rate**: Should be 98%+
- **When**: Before release

---

## Tips

1. **Start with critical tests first**: Tag smoke tests trước
2. **Be selective**: Không phải test nào cũng cần là smoke test
3. **Update regularly**: Khi có feature mới, update tags phù hợp
4. **Document decisions**: Ghi chú lý do chọn tag trong test comment
5. **Review periodically**: Định kỳ review lại phân loại test

---

## Example: Full Test Class với Tags

```java
package com.api.moviebooking.services;

import com.api.moviebooking.tags.SmokeTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.RegressionTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class BookingServiceTest {

    // CRITICAL PATH - All tags
    @Test
    @SmokeTest
    @SanityTest
    @RegressionTest
    @DisplayName("Should successfully lock seats")
    void testLockSeats_Success() {
        // Test implementation
    }

    // BUSINESS LOGIC - Sanity + Regression
    @Test
    @SanityTest
    @RegressionTest
    @DisplayName("Should calculate price with modifiers")
    void testPriceCalculation() {
        // Test implementation
    }

    // ERROR HANDLING - Regression only
    @Test
    @RegressionTest
    @DisplayName("Should throw exception when seats unavailable")
    void testLockSeats_SeatsUnavailable() {
        // Test implementation
    }

    // EDGE CASE - Regression only
    @Test
    @RegressionTest
    @DisplayName("Should handle concurrent lock attempts")
    void testConcurrentLocking() {
        // Test implementation
    }
}
```
