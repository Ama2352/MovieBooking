# Bookings API Documentation

Base Path: `/bookings`

## Overview
Endpoints for managing movie ticket bookings, including seat locking, booking confirmation, and booking history.

---

## Booking Flow
1. **Lock Seats** → `/bookings/lock` (POST)
2. **Check Availability** → `/bookings/lock/availability/{showtimeId}` (GET)
3. **Confirm Booking** → `/bookings/confirm` (POST)
4. **Initiate Payment** → See Payment API
5. **View Bookings** → `/bookings/my-bookings` (GET)

---

## Endpoints

### 1. Lock Seats
**POST** `/bookings/lock`

Locks selected seats for 10 minutes to prevent double-booking during checkout.

#### Request Body
```json
{
  "showtimeId": "uuid (required)",
  "userId": "uuid (required)",
  "seatIds": ["uuid", "uuid", ...] (required, not empty)
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "lockId": "uuid",
  "status": "string",
  "showtimeId": "uuid",
  "seats": [
    {
      "seatId": "uuid",
      "rowLabel": "string",
      "seatNumber": 1,
      "seatType": "NORMAL|VIP|COUPLE",
      "price": 100000.00
    }
  ],
  "totalPrice": 200000.00,
  "expiresAt": "2024-11-17T12:30:00",
  "remainingSeconds": 600,
  "message": "Seats locked successfully"
}
```

#### Authentication
- **Required**: No (uses userId from request body)

---

### 2. Check Seat Availability
**GET** `/bookings/lock/availability/{showtimeId}`

Returns available, locked, and booked seats for a showtime. Releases any existing locks for the user.

#### Path Parameters
- `showtimeId`: UUID of the showtime

#### Query Parameters
- `userId`: UUID of the user (required)

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "showtimeId": "uuid",
  "availableSeatIds": ["uuid", "uuid", ...],
  "lockedSeatIds": ["uuid", "uuid", ...],
  "bookedSeatIds": ["uuid", "uuid", ...],
  "message": "Seat availability retrieved"
}
```

#### Authentication
- **Required**: No (uses userId from query param)

---

### 3. Release Locked Seats
**DELETE** `/bookings/lock/release`

Manually releases locked seats before confirmation or timeout.

#### Query Parameters
- `showtimeId`: UUID (required)
- `userId`: UUID (required)

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: No

---

### 4. Handle Back Button
**POST** `/bookings/lock/back`

Releases all locked seats immediately when user navigates back. Makes seats available to everyone.

#### Query Parameters
- `showtimeId`: UUID (required)
- `userId`: UUID (required)

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: No

---

### 5. Confirm Booking
**POST** `/bookings/confirm`

Confirms the booking after seat locking, transitions seats from LOCKED to BOOKED, and applies promotion codes.

#### Request Body
```json
{
  "showtimeId": "uuid (required)",
  "userId": "uuid (required)",
  "promotionCode": "string (optional)"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "id": "uuid",
  "userId": "uuid",
  "userName": "string",
  "bookingDate": "2024-11-17T12:00:00",
  "showtimeId": "string",
  "movieTitle": "string",
  "seats": [
    {
      "rowLabel": "A",
      "seatNumber": 5,
      "seatType": "VIP",
      "price": 120000.00
    }
  ],
  "totalAmount": 240000.00,
  "promotionCode": "WINTER2024",
  "discountAmount": 24000.00,
  "finalPrice": 216000.00,
  "status": "PENDING_PAYMENT",
  "createdAt": "2024-11-17T12:00:00",
  "qrCodeUrl": null,
  "paymentMethod": null,
  "paidAt": null,
  "expiresAt": "2024-11-17T12:10:00"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Note**: Supports both registered users and guest users

---

### 6. Get User's Bookings
**GET** `/bookings/my-bookings`

Returns all bookings for the authenticated user.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of BookingResponse objects
```json
[
  {
    "id": "uuid",
    "userId": "uuid",
    "userName": "string",
    "bookingDate": "2024-11-17T12:00:00",
    "showtimeId": "string",
    "movieTitle": "string",
    "seats": [...],
    "totalAmount": 240000.00,
    "promotionCode": "WINTER2024",
    "discountAmount": 24000.00,
    "finalPrice": 216000.00,
    "status": "CONFIRMED",
    "createdAt": "2024-11-17T12:00:00",
    "qrCodeUrl": "https://cloudinary.com/...",
    "paymentMethod": "PAYPAL",
    "paidAt": "2024-11-17T12:05:00",
    "expiresAt": null
  }
]
```

#### Authentication
- **Required**: Yes (Bearer Token)

---

### 7. Get Booking Details
**GET** `/bookings/{bookingId}`

Returns details of a specific booking.

#### Path Parameters
- `bookingId`: UUID of the booking

#### Response
- **Status Code**: `200 OK`
- **Body**: BookingResponse object (same structure as above)

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: User must own the booking

---

### 8. Update QR Code
**PATCH** `/bookings/{bookingId}/qr`

Attaches a Cloudinary QR code URL to the booking after frontend generates it.

#### Path Parameters
- `bookingId`: UUID of the booking

#### Request Body
```json
{
  "qrCodeUrl": "string (required, not blank)"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: BookingResponse object with updated qrCodeUrl

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: User must own the booking

---

## Booking Status Enum

| Status | Description |
|--------|-------------|
| `PENDING_PAYMENT` | Booking confirmed, awaiting payment |
| `CONFIRMED` | Payment successful, booking active |
| `CANCELLED` | Booking cancelled by user or system |
| `EXPIRED` | Payment timeout exceeded |
| `REFUNDED` | Payment refunded, seats released |
| `USED` | Tickets scanned/used at cinema |

---

## Error Responses

### 400 Bad Request
```json
{
  "message": "Invalid request parameters",
  "details": "Seat lock expired or seats already booked"
}
```

### 404 Not Found
```json
{
  "message": "Booking not found"
}
```

### 409 Conflict
```json
{
  "message": "Seats already locked or booked by another user"
}
```

### 403 Forbidden
```json
{
  "message": "You are not authorized to access this booking"
}
```

---

## Important Notes

1. **Seat Lock Duration**: Seats are locked for 10 minutes (600 seconds). After expiration, they become available again.
2. **Promotion Codes**: Applied during booking confirmation. Must be active and valid.
3. **QR Code**: Frontend should generate QR code after successful payment and update the booking.
4. **Guest Users**: Can create bookings but must track their userId for retrieval.
5. **Back Button**: Always call `/bookings/lock/back` when user navigates away to release seats immediately.
