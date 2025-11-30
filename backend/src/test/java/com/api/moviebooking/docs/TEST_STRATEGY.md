# Test Strategy Documentation

## Overview
Hệ thống test được phân chia thành 3 loại chính theo chiến lược kiểm thử:

## 1. SMOKE TESTS 🔥
**Mục đích**: Kiểm tra nhanh các chức năng cốt lõi của hệ thống

**Khi nào chạy**:
- Sau mỗi lần build
- Trước khi deploy
- Khi pull code mới về

**Thời gian**: 5-10 phút

**Test Coverage**:
```
✅ Authentication Flow
   - User registration
   - User login
   - Token refresh

✅ Movie Browsing
   - Get movie list
   - Get movie details
   - Get showtimes

✅ Booking Critical Path
   - Check seat availability
   - Lock seats
   - Confirm booking
   - Create payment

✅ Payment Processing
   - MoMo payment creation
   - Payment callback handling
   - QR code generation
```

**Chạy smoke tests**:
```bash
mvn test -Dgroups="smoke"
```

---

## 2. SANITY TESTS 🔍
**Mục đích**: Kiểm tra tính đúng đắn của chức năng sau khi có thay đổi

**Khi nào chạy**:
- Sau khi fix bug
- Sau khi thêm feature mới
- Trước khi merge PR

**Thời gian**: 10-15 phút

**Test Coverage**:
```
✅ Business Logic Validation
   - Pricing calculations
   - Promotion application
   - Membership discount
   - Seat type pricing

✅ Modified Features
   - Ticket type modifiers
   - Booking seat structure
   - Price breakdown

✅ Edge Cases
   - Lock expiration handling
   - Concurrent booking prevention
   - Payment timeout scenarios
   - Refund processing
```

**Chạy sanity tests**:
```bash
mvn test -Dgroups="sanity"
```

---

## 3. REGRESSION TESTS 🔄
**Mục đích**: Đảm bảo không có chức năng cũ bị hỏng

**Khi nào chạy**:
- Trước release chính thức
- Nightly builds
- Sau major refactoring

**Thời gian**: 30-60 phút

**Test Coverage**:
```
✅ All Happy Paths
   - Toàn bộ user journeys thành công

✅ Error Handling
   - Validation errors
   - Business rule violations
   - System exceptions

✅ Edge Cases & Boundaries
   - Max seats per booking
   - Lock duration limits
   - Payment expiry
   - Concurrent operations

✅ Integration Scenarios
   - Database transactions
   - Redis locking
   - External payment gateways
   - Email notifications

✅ Performance Validation
   - Response time checks
   - Concurrent user simulation
```

**Chạy regression tests**:
```bash
mvn test -Dgroups="regression"
```

---

## Test Classification Guidelines

### SMOKE Test Criteria:
- Critical business flows ONLY
- Happy path scenarios
- Fast execution (<1 second per test)
- No external dependencies if possible
- Must pass 100% for deployment

### SANITY Test Criteria:
- Core business logic validation
- Related dependent features
- Recently modified code paths
- Important edge cases
- 95%+ pass rate required

### REGRESSION Test Criteria:
- All functional scenarios
- Error handling & validation
- Boundary conditions
- Complex integration flows
- 98%+ pass rate for release

---

## Example Test Annotations

```java
// SMOKE TEST - Critical authentication flow
@Test
@SmokeTest
@DisplayName("Should successfully login with valid credentials")
void testLogin_Success() {
    // Test implementation
}

// SANITY TEST - Business logic validation
@Test
@SanityTest
@DisplayName("Should apply ticket type modifier correctly")
void testTicketTypeModifier_Percentage() {
    // Test implementation
}

// REGRESSION TEST - Complex edge case
@Test
@RegressionTest
@DisplayName("Should handle concurrent booking with race condition")
void testConcurrentBooking_RaceCondition() {
    // Test implementation
}

// Multiple tags for important tests
@Test
@SmokeTest
@SanityTest
@RegressionTest
@DisplayName("Should complete full booking flow end-to-end")
void testCompleteBookingFlow() {
    // Test implementation
}
```

---

## CI/CD Integration

### GitHub Actions Workflow:
```yaml
# Run smoke tests on every push
- name: Smoke Tests
  run: mvn test -Dgroups="smoke"
  
# Run sanity tests on PR
- name: Sanity Tests
  run: mvn test -Dgroups="sanity"
  
# Run regression tests nightly
- name: Regression Tests
  run: mvn test -Dgroups="regression"
  if: github.event_name == 'schedule'
```

---

## Test Metrics

| Test Type | Count | Duration | Pass Rate Required |
|-----------|-------|----------|-------------------|
| Smoke     | ~20   | 5-10 min | 100%              |
| Sanity    | ~50   | 10-15 min| 95%               |
| Regression| ~150  | 30-60 min| 98%               |

---

## Recommended Test Files to Tag

### SMOKE TESTS (Critical Path):
1. **UserServiceTest**
   - `testRegister_Success`
   - `testLogin_Success`
   - `testRefreshAccessToken_Success`

2. **MovieServiceTest**
   - `testGetAllMovies_Success`
   - `testGetMovieById_Success`

3. **BookingServiceTest**
   - `testLockSeats_Success`
   - `testConfirmBooking_Success`

4. **BookingIntegrationTest**
   - `testCompleteBookingFlow_Success`

5. **PaymentIntegrationTest**
   - `testCreatePayment_Success`
   - `testPaymentCallback_Success`

### SANITY TESTS (Business Logic):
1. **PromotionServiceTest** (All tests)
2. **PricingIntegrationTest** (All tests)
3. **TicketTypeServiceTest** (All tests)
4. **RefundServiceTest** (All tests)

### REGRESSION TESTS (Comprehensive):
- **All remaining tests** in both unit and integration folders

---

## Benefits

✅ **Faster Feedback**: Smoke tests catch critical issues in minutes
✅ **Targeted Testing**: Sanity tests focus on changed areas
✅ **Comprehensive Coverage**: Regression ensures nothing breaks
✅ **CI/CD Optimization**: Run appropriate tests at each stage
✅ **Clear Test Purpose**: Easy to understand what each test validates
