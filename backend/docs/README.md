# Movie Booking API Documentation

Complete API documentation for the Movie Booking System backend.

---

## Table of Contents

### Core Booking Flow
1. [Authentication API](./API-Authentication.md) - User registration, login, tokens
2. [Bookings API](./API-Bookings.md) - Seat locking, booking confirmation, history
3. [Checkout API](./API-Checkout.md) - Atomic booking + payment initiation
4. [Payments API](./API-Payments.md) - Payment processing, refunds, multi-currency

### Content Management
5. [Movies API](./API-Movies.md) - Movie CRUD, search, filtering
6. [Cinemas API](./API-Cinemas.md) - Cinema locations, rooms, snacks
7. [Showtimes API](./API-Showtimes.md) - Showtime scheduling and queries
8. [Seats API](./API-Seats.md) - Seat templates and showtime seat instances

### Pricing & Promotions
9. [Pricing API](./API-Pricing.md) - Base prices and dynamic modifiers
10. [Promotions API](./API-Promotions.md) - Discount codes and campaigns
11. [Membership Tiers API](./API-MembershipTiers.md) - Loyalty program tiers

---

## Quick Start Guide

### Base URL
```
http://localhost:8080/api
```

### Authentication
Most endpoints require authentication using Bearer tokens:
```
Authorization: Bearer <your-access-token>
```

Tokens are obtained via login and stored in HTTP-only cookies.

---

## Complete User Booking Flow

### Step-by-Step Integration

#### 1. User Registration/Login
```javascript
// Register new user
POST /auth/register
{
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "phoneNumber": "+84 123456789",
  "password": "securepass123"
}

// Or login existing user
POST /auth/login
{
  "email": "john@example.com",
  "password": "securepass123"
}
// Returns access/refresh tokens in cookies
```

#### 2. Browse Movies
```javascript
// Get currently showing movies
GET /movies?status=SHOWING

// Get movie details
GET /movies/{movieId}

// Search movies
GET /movies?title=spider&genre=action
```

#### 3. Select Showtime
```javascript
// Get upcoming showtimes for a movie
GET /showtimes/movie/{movieId}/upcoming

// Get showtime details
GET /showtimes/{showtimeId}
```

#### 4. View Available Seats
```javascript
// Get all seats for the showtime with availability
GET /showtime-seats/showtime/{showtimeId}

// Or check availability with user cleanup
GET /bookings/lock/availability/{showtimeId}?userId={userId}
```

#### 5. Lock Selected Seats
```javascript
POST /bookings/lock
{
  "showtimeId": "uuid",
  "userId": "uuid",
  "seatIds": ["seat-uuid-1", "seat-uuid-2"]
}
// Seats locked for 10 minutes
```

#### 6. Apply Promotion Code (Optional)
```javascript
// Validate promotion
GET /promotions/code/WINTER2024

// Get valid promotions
GET /promotions/valid
```

#### 7A. Two-Step Checkout (Traditional)
```javascript
// Step 1: Confirm booking
POST /bookings/confirm
{
  "showtimeId": "uuid",
  "userId": "uuid",
  "promotionCode": "WINTER2024" // optional
}
// Returns booking with PENDING_PAYMENT status

// Step 2: Initiate payment
POST /payments/order
{
  "bookingId": "uuid",
  "method": "PAYPAL", // or "MOMO"
  "amount": 216000.00
}
// Returns payment URL, redirect user
```

#### 7B. Atomic Checkout (Recommended)
```javascript
// One-step: Confirm booking + Initiate payment
POST /checkout
{
  "showtimeId": "uuid",
  "userId": "uuid",
  "promotionCode": "WINTER2024", // optional
  "paymentMethod": "PAYPAL" // or "MOMO"
}
// Returns payment URL, redirect user
// If payment fails, booking is not created (atomic transaction)
```

#### 8. Payment Gateway
```javascript
// User completes payment on PayPal/Momo
// Gateway redirects back to your return URL with order ID

// Confirm/capture payment
POST /payments/order/capture
{
  "orderId": "gateway-order-id",
  "method": "PAYPAL" // or "MOMO"
}
// Returns payment result
// On success: Booking status → CONFIRMED, Seats → BOOKED
```

#### 9. View Booking
```javascript
// Get user's bookings
GET /bookings/my-bookings

// Get specific booking
GET /bookings/{bookingId}
```

#### 10. Generate QR Code (Frontend)
```javascript
// Frontend generates QR code from booking ID
// Then updates booking with QR code URL
PATCH /bookings/{bookingId}/qr
{
  "qrCodeUrl": "https://cloudinary.com/qr-code.png"
}
```

---

## Admin Operations Flow

### Cinema Setup
```javascript
// 1. Create cinema
POST /cinemas
{
  "name": "CGV Vincom Center",
  "location": "72 Le Thanh Ton, District 1, HCMC",
  "hotline": "1900 6017"
}

// 2. Create rooms
POST /cinemas/rooms
{
  "cinemaId": "cinema-uuid",
  "name": "Room 1 - IMAX",
  "totalSeats": 150
}

// 3. Generate seats for room
POST /seats/generate
{
  "roomId": "room-uuid",
  "rows": 10,
  "seatsPerRow": 15,
  "vipRows": ["A", "B"],
  "coupleRows": ["J"]
}
```

### Movie & Showtime Setup
```javascript
// 1. Add movie
POST /movies
{
  "title": "Oppenheimer",
  "description": "...",
  "genre": "Biography, Drama",
  "duration": 180,
  "releaseYear": 2023,
  "director": "Christopher Nolan",
  "cast": "Cillian Murphy, Emily Blunt",
  "language": "English",
  "subtitles": "Vietnamese",
  "posterUrl": "https://...",
  "trailerUrl": "https://...",
  "status": "SHOWING"
}

// 2. Create showtimes
POST /showtimes
{
  "movieId": "movie-uuid",
  "roomId": "room-uuid",
  "format": "IMAX",
  "startTime": "2024-11-17T19:30:00"
}
// System automatically creates showtime seats with calculated prices
```

### Pricing Configuration
```javascript
// 1. Set base price
POST /price-base
{
  "name": "Standard Base Price 2024",
  "basePrice": 80000.00
}

// 2. Add price modifiers
POST /price-modifiers
{
  "name": "VIP Seat Premium",
  "conditionType": "SEAT_TYPE",
  "conditionValue": "VIP",
  "modifierType": "ADD",
  "modifierValue": 20000.00
}

POST /price-modifiers
{
  "name": "Weekend Surcharge",
  "conditionType": "DAY_TYPE",
  "conditionValue": "WEEKEND",
  "modifierType": "MULTIPLY",
  "modifierValue": 1.2
}

// 3. Recalculate existing showtime prices (if needed)
POST /showtime-seats/showtime/{showtimeId}/recalculate-prices
```

### Promotions Setup
```javascript
// Create promotion
POST /promotions
{
  "code": "WINTER2024",
  "name": "Winter Sale 2024",
  "description": "Special winter discount",
  "discountType": "PERCENTAGE",
  "discountValue": 15.00,
  "startDate": "2024-11-17T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "maxUsage": 1000,
  "maxUsagePerUser": 2,
  "isActive": true
}
```

### Membership Tiers Setup
```javascript
// Create tier system
POST /membership-tiers
{
  "name": "Bronze",
  "pointsRequired": 0,
  "discountType": "PERCENTAGE",
  "discountValue": 5.00,
  "description": "Starter tier",
  "isActive": true
}

POST /membership-tiers
{
  "name": "Gold",
  "pointsRequired": 5000,
  "discountType": "PERCENTAGE",
  "discountValue": 15.00,
  "description": "Premium tier",
  "isActive": true
}
```

### Refund Processing
```javascript
// Admin refunds a payment
POST /payments/{paymentId}/refund
{
  "reason": "Customer request due to scheduling conflict"
}
// Processes refund through gateway
// Updates booking status to REFUNDED
// Releases seats back to AVAILABLE
```

---

## Key Concepts

### Seat Status Lifecycle
```
AVAILABLE → LOCKED (user selects, 10 min timeout)
         ↓
      BOOKED (payment confirmed)
         ↓
      AVAILABLE (refund/cancel)
```

### Booking Status Lifecycle
```
PENDING_PAYMENT (booking confirmed, awaiting payment)
       ↓
   CONFIRMED (payment successful)
       ↓
   USED (tickets scanned at cinema)

Or:
PENDING_PAYMENT → CANCELLED (payment failed/timeout)
CONFIRMED → REFUNDED (admin refund)
```

### Payment Status Lifecycle
```
PENDING (payment initiated)
   ↓
COMPLETED (payment successful)

Or:
PENDING → FAILED (payment rejected)
PENDING → CANCELLED (user cancelled)
COMPLETED → REFUNDED (admin refunded)
```

---

## Multi-Currency Support

### PayPal Integration
- Booking amount in VND (e.g., 216,000 VND)
- Auto-converted to USD for PayPal (e.g., 8.64 USD)
- Exchange rate cached for 1 hour
- All amounts and rates stored in payment record

### Currency Fields
- `amount`: Original amount in VND
- `currency`: "VND"
- `gatewayAmount`: Converted amount in gateway currency
- `gatewayCurrency`: "USD" for PayPal, "VND" for Momo
- `exchangeRate`: Conversion rate used

---

## Error Handling

All endpoints return standard error responses:

### 400 Bad Request
Invalid input or validation failed
```json
{
  "message": "Validation failed",
  "errors": ["field1 error", "field2 error"]
}
```

### 401 Unauthorized
Missing or invalid authentication
```json
{
  "message": "Unauthorized"
}
```

### 403 Forbidden
Insufficient permissions
```json
{
  "message": "Admin access required"
}
```

### 404 Not Found
Resource not found
```json
{
  "message": "Resource not found with id: {id}"
}
```

### 409 Conflict
Business logic conflict
```json
{
  "message": "Seats already booked"
}
```

### 500 Internal Server Error
Server error
```json
{
  "message": "Internal server error"
}
```

---

## Best Practices

### For Frontend Developers

1. **Token Management**
   - Store tokens securely (HTTP-only cookies preferred)
   - Refresh tokens before expiry
   - Handle 401 errors with automatic re-authentication

2. **Seat Selection**
   - Always lock seats before checkout
   - Show countdown timer (10 minutes)
   - Handle lock expiration gracefully
   - Call release endpoint when user navigates away

3. **Price Display**
   - Fetch real-time prices from showtime-seats endpoint
   - Display price breakdown for transparency
   - Update totals when applying promotions/discounts

4. **Payment Flow**
   - Use atomic checkout for simplicity
   - Store booking ID before redirect
   - Handle payment callbacks correctly
   - Show clear success/failure messages

5. **Error Handling**
   - Display user-friendly error messages
   - Implement retry logic for network errors
   - Provide alternative actions on failure

### For Backend Developers

1. **Transaction Safety**
   - Use database transactions for multi-step operations
   - Implement idempotency for payment endpoints
   - Handle concurrent requests with proper locking

2. **Performance**
   - Cache frequently accessed data (prices, modifiers)
   - Use database indexes for search queries
   - Implement pagination for large result sets

3. **Security**
   - Validate all inputs
   - Sanitize user data
   - Verify payment gateway signatures
   - Use HTTPS for all communications

4. **Data Integrity**
   - Never delete bookings (soft delete)
   - Maintain audit trails
   - Validate business rules before commits

---

## Testing

### Recommended Test Scenarios

1. **Complete Booking Flow**
   - Register → Browse → Select → Lock → Checkout → Pay → Confirm

2. **Concurrent Bookings**
   - Multiple users selecting same seats simultaneously

3. **Payment Failures**
   - User cancels payment
   - Payment gateway errors
   - Network timeouts

4. **Lock Timeouts**
   - Seats automatically released after 10 minutes
   - User can rebook released seats

5. **Promotion Validation**
   - Valid/invalid codes
   - Expired promotions
   - Usage limit enforcement

6. **Price Calculations**
   - Various seat types and modifiers
   - Edge cases (negative prices, overflow)

7. **Refund Processing**
   - Full refund
   - Seat availability after refund
   - Gateway refund confirmation

---

## Support & Contact

For API questions or issues:
- **Documentation**: This repository
- **API Base URL**: `http://localhost:8080/api` (development)
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

---

## Version History

### v1.0.0 (Current)
- Complete booking flow
- Multi-currency payment support (PayPal, Momo)
- Dynamic pricing system
- Promotion codes
- Membership tiers
- Atomic checkout
- Refund processing

---

## Additional Resources

- [Swagger/OpenAPI Documentation](http://localhost:8080/swagger-ui.html)
- [Postman Collection](#) (if available)
- [Frontend Integration Examples](#)

---

*Last Updated: November 17, 2024*
