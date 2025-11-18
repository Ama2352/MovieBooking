# Payments API Documentation

Base Path: `/payments`

## Overview
Payment processing endpoints supporting multiple payment gateways (PayPal, Momo) with multi-currency support and refund capabilities.

---

## Payment Flow

### Standard Flow (Two-Step: Confirm Booking → Initiate Payment)
1. Lock seats → `/bookings/lock`
2. Confirm booking → `/bookings/confirm`
3. Initiate payment → `/payments/order`
4. User completes payment on gateway
5. Confirm payment → `/payments/order/capture`

### Atomic Flow (One-Step: Checkout)
1. Lock seats → `/bookings/lock`
2. Checkout (confirm + initiate) → `/checkout` (See Checkout API)
3. User completes payment on gateway
4. Confirm payment → `/payments/order/capture`

---

## Endpoints

### 1. Initiate Payment
**POST** `/payments/order`

Creates a payment order and returns the payment gateway URL for user redirect.

#### Request Body
```json
{
  "bookingId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  "paymentMethod": "PAYPAL",
  "amount": 216000.00
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "paymentId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
  "orderId": "8CB56781FV123456K",
  "txnRef": "8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c",
  "paymentUrl": "https://www.paypal.com/checkoutnow?token=8CB56781FV123456K"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)

#### Notes
- For PayPal: amount in VND is converted to USD automatically
- For Momo: amount must be in VND
- Payment gateway amount and exchange rate are stored in payment record

---

### 2. Confirm Payment
**POST** `/payments/order/capture`

Captures/verifies payment after user completes payment on gateway and returns to your site.

#### Request Body
```json
{
  "transactionId": "8CB56781FV123456K",
  "paymentMethod": "PAYPAL"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "paymentId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
  "bookingId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  "amount": 216000.00,
  "currency": "VND",
  "gatewayAmount": 8.64,
  "gatewayCurrency": "USD",
  "exchangeRate": 25000.00,
  "status": "COMPLETED",
  "method": "PAYPAL",
  "bookingStatus": "CONFIRMED",
  "qrPayload": "BOOKING_5e6f7a8b_SHOWTIME_3e4a8c9f"
}
```

#### Authentication
- **Required**: No (Public callback endpoint)

#### Notes
- Frontend should call this after gateway redirects back
- On success: booking status → CONFIRMED, seats → BOOKED
- On failure: booking status → CANCELLED, seats → AVAILABLE

---

### 3. Momo IPN Handler
**GET/POST** `/payments/momo/ipn`

Webhook endpoint for Momo to send payment notifications (Instant Payment Notification).

#### Request
- Accepts both GET and POST methods
- Contains Momo signature and payment details in query params or form data

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "status": "string (success, error, invalid_checksum, etc.)",
  "message": "string (status description)"
}
```

#### Authentication
- **Required**: No (Public webhook endpoint)

#### Notes
- Validates Momo signature for security
- Automatically updates payment and booking status
- This is called by Momo servers, not by frontend

---

### 4. Search Payments
**GET** `/payments/search`

Search and filter payments with various criteria.

#### Query Parameters (all optional)
- `bookingId`: UUID - Filter by booking
- `userId`: UUID - Filter by user
- `status`: string - Filter by payment status (PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED)
- `method`: string - Filter by payment method (PAYPAL, MOMO)
- `startDate`: datetime (ISO 8601) - Filter payments after this date
- `endDate`: datetime (ISO 8601) - Filter payments before this date

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PaymentResponse objects
```json
[
  {
    "paymentId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
    "bookingId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
    "amount": 216000.00,
    "currency": "VND",
    "gatewayAmount": 8.64,
    "gatewayCurrency": "USD",
    "exchangeRate": 25000.00,
    "status": "COMPLETED",
    "method": "PAYPAL",
    "bookingStatus": "CONFIRMED",
    "qrPayload": "BOOKING_5e6f7a8b_SHOWTIME_3e4a8c9f"
  }
]
```

#### Authentication
- **Required**: Yes (Bearer Token)

---

### 5. Refund Payment
**POST** `/payments/{paymentId}/refund`

Initiates a full refund for a completed payment and releases associated seats.

#### Path Parameters
- `paymentId`: UUID of the payment

#### Request Body
```json
{
  "reason": "Customer requested refund due to scheduling conflict"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: PaymentResponse object with updated status
```json
{
  "paymentId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
  "bookingId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  "amount": 216000.00,
  "currency": "VND",
  "gatewayAmount": 8.64,
  "gatewayCurrency": "USD",
  "exchangeRate": 25000.00,
  "method": "PAYPAL",
  "status": "REFUNDED",
  "bookingStatus": "REFUNDED",
  "qrPayload": null
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Only completed payments can be refunded
- Refund is processed through the original payment gateway
- Booking status → REFUNDED, Seats → AVAILABLE
- Full refund only (no partial refunds)

---

## Payment Status Enum

| Status | Description |
|--------|-------------|
| `PENDING` | Payment initiated, awaiting user action |
| `PROCESSING` | Payment being processed by gateway |
| `COMPLETED` | Payment successful |
| `FAILED` | Payment failed or rejected |
| `CANCELLED` | Payment cancelled by user or timeout |
| `REFUNDED` | Payment refunded to user |

---

## Payment Method Enum

| Method | Currency | Notes |
|--------|----------|-------|
| `PAYPAL` | VND → USD | Auto-converted using exchange rate API |
| `MOMO` | VND | Direct VND processing |

---

## Multi-Currency Details

### Currency Conversion (PayPal)
- **Base Currency**: VND (Vietnamese Dong)
- **Gateway Currency**: USD (US Dollar)
- **Exchange Rate**: Fetched from external API, cached for 1 hour
- **Conversion Flow**:
  1. User pays in VND
  2. System converts to USD for PayPal
  3. All amounts and rates stored in payment record

### Currency Fields
- `amount`: Original booking amount in VND
- `currency`: "VND"
- `gatewayAmount`: Converted amount in gateway currency
- `gatewayCurrency`: "USD" for PayPal, "VND" for Momo
- `exchangeRate`: Conversion rate used (e.g., 25000 VND = 1 USD)

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Booking must be in PENDING_PAYMENT status",
  "details": "uri=/payments/order"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Payment not found with id: {paymentId}",
  "details": "uri=/payments/{paymentId}"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Payment already processed",
  "details": "uri=/payments/order/capture"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Access Denied: Admin access required",
  "details": "uri=/payments/{paymentId}/refund"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Failed to communicate with PayPal/Momo",
  "details": "uri=/payments/order"
}
```

---

## Integration Guide

### Frontend Payment Flow

#### Step 1: Initiate Payment
```javascript
// After booking confirmation
const response = await fetch('/payments/order', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer <token>',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    bookingId: 'booking-uuid',
    method: 'PAYPAL', // or 'MOMO'
    amount: 216000
  })
});

const { paymentUrl } = await response.json();
// Redirect user to paymentUrl
window.location.href = paymentUrl;
```

#### Step 2: Handle Return from Gateway
```javascript
// User returns to your site after payment
// PayPal: ?token=ORDER_ID
// Momo: ?orderId=REQUEST_ID&resultCode=0

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
if (result.status === 'COMPLETED') {
  // Show success page
  // Generate QR code for booking
} else {
  // Show failure page
}
```

---

## Testing

### Test Payment Methods

#### PayPal Sandbox
- Use PayPal sandbox accounts for testing
- No real money transactions
- Full payment flow simulation

#### Momo Test
- Use Momo test credentials
- Test phone number: 0399999999
- No real money transactions

### Test Scenarios
1. **Successful Payment**: Complete payment flow
2. **Cancelled Payment**: User cancels on gateway page
3. **Expired Payment**: Don't complete payment within timeout
4. **Refund**: Admin refunds completed payment
5. **Currency Conversion**: Test VND to USD conversion for PayPal

---

## Important Notes

1. **Payment Timeout**: Pending payments expire after 15 minutes
2. **Idempotency**: Multiple capture requests with same orderId are safe (returns existing result)
3. **Webhook Security**: Momo IPN validates signature to prevent fraud
4. **Currency Caching**: Exchange rates cached for 1 hour for performance
5. **Refund Policy**: Full refunds only, no partial refunds supported
6. **Transaction IDs**: Store gateway transaction IDs for reconciliation
