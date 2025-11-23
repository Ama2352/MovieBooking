# Checkout API Documentation

Base Path: `/checkout`

## Overview
Atomic checkout endpoint that combines booking confirmation and payment initiation in a single transaction. If either step fails, the entire operation is rolled back.

---

## Checkout Flow Advantage

### Traditional Two-Step Flow
1. Confirm booking → `/bookings/confirm`
2. Initiate payment → `/payments/order`
**Risk**: If payment initiation fails, booking exists in PENDING_PAYMENT state

### Atomic Checkout Flow ✅
1. Checkout → `/checkout` (confirms booking + initiates payment atomically)
**Benefit**: If payment initiation fails, booking is never created (transaction rollback)

---

## Endpoint

### Confirm Booking and Initiate Payment (Atomic)
**POST** `/checkout`

Atomically validates seat locks, creates a pending booking, and initiates payment. If payment initiation fails, the entire transaction is rolled back.

#### Request Body
```json
{
  "lockId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "userId": "7b2e9a1c-4567-89ab-cdef-123456789012",
  "promotionCode": "WINTER2024",
  "paymentMethod": "PAYPAL",
  "snackCombos": [
    { "snackId": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b", "quantity": 2 },
    { "snackId": "f2a3b4c5-d6e7-8f9a-0b1c-2d3e4f5a6b7c", "quantity": 1 }
  ]
}
```

**Note:**
- `snackCombos` is an array of objects, each with `snackId` and `quantity` fields.


#### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `lockId` | UUID | Yes | ID of the seat lock |
| `userId` | UUID | Yes | ID of the user (guest or registered) |
| `promotionCode` | String | No | Promotion code for discount. **Note**: Guest users cannot use promotions - only registered users |
| `paymentMethod` | String | Yes | Payment method: PAYPAL or MOMO |
| `snackCombos` | Array | No | List of snack combo selections. Each item: `{ "snackId": UUID, "quantity": Integer }` |

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "bookingId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  "paymentId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
  "paymentMethod": "PAYPAL",
  "redirectUrl": "https://www.paypal.com/checkoutnow?token=8CB56781FV123456K",
  "message": "Booking confirmed and payment initiated"
}
```

#### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| `bookingId` | UUID | ID of the created booking |
| `paymentId` | UUID | ID of the created payment |
| `paymentMethod` | String | Payment method used (PAYPAL or MOMO) |
| `redirectUrl` | String | URL to redirect user for payment completion |
| `message` | String | Success message |

#### Authentication
- **Required**: Yes (Bearer Token)
- **Supports**: Both registered users and guest users

---

## Process Flow

### 1. Validation Phase
- Verifies user has locked seats for the showtime
- Validates seat locks haven't expired
- Confirms all locked seats are still valid
- Validates promotion code (if provided)

### 2. Booking Creation Phase
- Creates booking with status `PENDING_PAYMENT`
- Calculates total amount with any discounts
- Transitions seats from `LOCKED` to `BOOKED`
- Associates promotion code with booking

### 3. Payment Initiation Phase
- Creates payment record with status `PENDING`
- Initiates payment with selected gateway (PayPal or Momo)
- Converts currency if needed (VND → USD for PayPal)
- Stores exchange rate and gateway amounts

### 4. Rollback on Failure
- If payment initiation fails, entire transaction is rolled back
- Booking is not created
- Seats remain in `LOCKED` state
- User can retry or seats will auto-release after timeout

---

## Example Usage

### Frontend Integration
```javascript
// After user selects seats and confirms details
async function checkout(lockId, userId, promotionCode, paymentMethod) {
  try {
    const response = await fetch('/checkout', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer <token>',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        lockId: lockId,
        userId: userId,
        promotionCode: promotionCode, // optional
        paymentMethod: paymentMethod // 'PAYPAL' or 'MOMO'
      })
    });

    if (!response.ok) {
      throw new Error('Checkout failed');
    }

    const data = await response.json();
    
    // Store booking ID for later reference
    localStorage.setItem('pendingBookingId', data.bookingId);
    
    // Redirect to payment gateway
    window.location.href = data.redirectUrl;
    
  } catch (error) {
    console.error('Checkout error:', error);
    // Show error message to user
    // Seats remain locked, user can retry
  }
}
```

### After Payment Return
```javascript
// User returns from payment gateway
// See Payment API documentation for capture flow
const urlParams = new URLSearchParams(window.location.search);
const transactionId = urlParams.get('token') || urlParams.get('orderId');
const paymentMethod = urlParams.get('token') ? 'PAYPAL' : 'MOMO';

// Confirm payment
const response = await fetch('/payments/order/capture', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    transactionId: transactionId,
    paymentMethod: paymentMethod
  })
});

const result = await response.json();
// Handle success or failure
```

---

## Complete Checkout Flow Diagram

```
User Actions                  API Calls                     Backend Processing
-----------                  ---------                     ------------------
1. Select seats     ──────>  POST /bookings/lock    ──────> Lock seats (10 min)
                                                             
2. Enter details    ──────>  POST /checkout         ──────> BEGIN TRANSACTION
   - Promo code                                             ├─ Validate locks
   - Payment method                                         ├─ Create booking
                                                            ├─ Transition seats
                                                            ├─ Apply promo
                                                            ├─ Create payment
                                                            └─ Initiate gateway
                                                            
                            Response: redirectUrl    <────── COMMIT or ROLLBACK
                                                             
3. Redirected       ──────>  Gateway Website        ──────> User pays
                                                             
4. Return to site   ──────>  POST /payments/         ──────> Capture payment
                             order/capture                   Update statuses
                                                             
5. View ticket      ──────>  GET /bookings/          ──────> Return booking
                             {bookingId}                     with QR code
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Seat locks expired or invalid",
  "details": "uri=/checkout"
}
```

**Common Causes:**
- Seat locks expired (older than 10 minutes)
- No seats locked for this showtime
- Invalid promotion code
- **Guest users attempting to use promotion codes** (guests cannot apply promotions)
- Invalid payment method
- Invalid snack IDs

### 409 Conflict
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "One or more seats are no longer available",
  "details": "uri=/checkout"
}
```

**Cause:**
- Race condition: seats locked by user but booked by someone else (rare)

### 410 Gone
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Seat lock has expired",
  "details": "uri=/checkout"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Failed to initiate payment with PayPal/Momo",
  "details": "uri=/checkout"
}
```

**Cause:**
- Payment gateway communication failure
- Transaction is rolled back automatically
- Booking is not created
- Seats remain locked for retry

---

## Comparison: Checkout vs Traditional Flow

### Traditional Flow (Two-Step)
**Pros:**
- More granular control
- Can create booking without immediate payment

**Cons:**
- Risk of orphaned bookings in PENDING_PAYMENT state
- Requires manual cleanup of failed payment initiations
- More API calls

### Atomic Checkout (One-Step) ✅
**Pros:**
- Atomic transaction guarantees consistency
- No orphaned bookings
- Single API call
- Automatic rollback on failure

**Cons:**
- Less flexible for special payment flows
- All-or-nothing (can't retry payment without re-booking)

**Recommended**: Use atomic checkout for standard user flow.

---

## Important Notes

1. **Transaction Atomicity**: If payment initiation fails, booking is NOT created and transaction is rolled back.

2. **Seat Lock Requirement**: User must lock seats before checkout. Locks are validated during checkout.

3. **Lock Expiration**: If locks expire during checkout, request will fail with 400 error.

4. **Promotion Codes**: Applied during checkout. Invalid codes will fail the entire checkout.

5. **Currency Conversion**: For PayPal, VND amounts are automatically converted to USD using cached exchange rates.

6. **Payment Timeout**: After checkout, user has 15 minutes to complete payment on gateway.

7. **Retry Logic**: If checkout fails due to gateway error, user can retry. Seats remain locked until timeout.

8. **Guest Users**: 
   - Fully supported for basic booking flow
   - Use guest userId from guest registration
   - **Cannot use promotion codes** - promotions are only available to registered users
   - **Do not receive membership tier discounts** - only registered users with assigned tiers
   - Can purchase tickets and snacks at regular prices

9. **Concurrent Users**: Seat locking prevents double-booking even with concurrent checkout attempts.

10. **Status Transitions**:
    - Seats: LOCKED → BOOKED (on checkout success)
    - Booking: Created with status PENDING_PAYMENT
    - Payment: Created with status PENDING
    - After payment capture: Booking → CONFIRMED, Payment → COMPLETED
