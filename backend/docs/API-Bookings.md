# Bookings API Documentation

Base Path: `/bookings`

## Overview
Endpoints for managing movie ticket bookings, including seat locking, booking confirmation, and booking history.

**Ticket Type Integration:**
- **Mandatory**: Each seat MUST have a ticket type selected during the lock phase
- Use `GET /ticket-types?showtimeId={id}` to get available ticket types with calculated prices
- Ticket types (adult, student, senior, etc.) affect the final seat price
- See [API-TicketTypes.md](API-TicketTypes.md) for detailed ticket type documentation

---

## Booking Flow
1. **Get Ticket Types** → `/ticket-types?showtimeId={id}` (GET) - See available ticket types with prices
2. **Lock Seats** → `/bookings/lock` (POST) - Lock seats with selected ticket types
3. **Check Availability** → `/bookings/lock/availability/{showtimeId}` (GET) - View seat status
4. **Confirm Booking** → `/bookings/confirm` (POST) - Finalize booking
5. **Initiate Payment** → See Payment API
6. **View Bookings** → `/bookings/my-bookings` (GET) - View booking history

---

## Endpoints

### 1. Lock Seats
**POST** `/bookings/lock`

Locks selected seats for 10 minutes to prevent double-booking during checkout.

#### Request Body
```json
{
  "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "userId": "7b2e9a1c-4567-89ab-cdef-123456789012",
  "seats": [
    {
      "showtimeSeatId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
      "ticketTypeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"
    },
    {
      "showtimeSeatId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "ticketTypeId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"
    }
  ]
}
```

**Note**: Each seat must have a ticket type selected (e.g., adult, student, senior). Use `GET /ticket-types?showtimeId={id}` to get available ticket types with calculated prices for the showtime.

#### Validation Rules
- Maximum seats per booking: 10 (configurable)
- Each seat must have a ticket type assigned
- **Ticket types must be active for the showtime** (validated via `showtime_ticket_types` table)
- Seats must be AVAILABLE (not LOCKED or BOOKED)
- User can only have one active lock per showtime at a time

#### Error Responses
- `400 Bad Request`: 
  - Exceeds maximum seats per booking
  - Missing ticket type for one or more seats
  - **Ticket type not available for this showtime**
- `404 Not Found`: Showtime, user, seat, or ticket type not found
- `409 Conflict`: Seats already locked or booked by another user
- `423 Locked`: Unable to acquire lock (concurrent booking attempt)

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "lockId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "lockKey": "LOCK_7b2e9a1c_3e4a8c9f",
  "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "lockedSeats": [
    {
      "seatId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
      "rowLabel": "A",
      "seatNumber": 5,
      "seatType": "NORMAL",
      "ticketTypeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "ticketTypeLabel": "NGƯỜI LỚN",
      "price": 100000.00
    },
    {
      "seatId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "rowLabel": "A",
      "seatNumber": 6,
      "seatType": "VIP",
      "ticketTypeId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
      "ticketTypeLabel": "HSSV/U22-GV",
      "price": 96000.00
    }
  ],
  "totalPrice": 196000.00,
  "expiresAt": "2024-11-17T12:30:00",
  "remainingSeconds": 600,
  "message": "Seats locked successfully"
}
```

**Response Fields:**
- `price`: Final seat price **with ticket type applied** (base + seat/showtime modifiers + ticket type modifier)
- `ticketTypeId` / `ticketTypeLabel`: The selected ticket type for each seat
- `totalPrice`: Sum of all seat prices (with ticket types)
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
  "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "availableSeats": [
    "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
    "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d"
  ],
  "lockedSeats": [
    "2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e"
  ],
  "bookedSeats": [
    "3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f",
    "4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a"
  ],
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
  "lockId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "userId": "7b2e9a1c-4567-89ab-cdef-123456789012",
  "promotionCode": "WINTER2024",
  "snackCombos": [
    { "snackId": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b", "quantity": 2 },
    { "snackId": "f2a3b4c5-d6e7-8f9a-0b1c-2d3e4f5a6b7c", "quantity": 1 }
  ]
}
```

**Note:**
- `snackCombos` is now an array of objects, each with `snackId` and `quantity` fields.
- This replaces the previous map structure for snack combos.

#### Request Fields
- `lockId`: UUID of the seat lock (required)
- `userId`: UUID of the user (required)
- `promotionCode`: Optional promotion code for discount
  - **Note**: Guest users cannot use promotion codes. Only registered users can apply promotions.
- `snackCombos`: Optional array of snack combo selections  
  - Each item: `{ "snackId": UUID, "quantity": Integer }`

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "bookingId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "movieTitle": "Inception",
  "showtimeStartTime": "2024-11-17T19:30:00",
  "cinemaName": "CGV Vincom Center",
  "roomName": "IMAX 1",
  "seats": [
    {
      "rowLabel": "A",
      "seatNumber": 5,
      "seatType": "VIP",
      "ticketTypeLabel": "NGƯỜI LỚN",
      "price": 120000.00
    },
    {
      "rowLabel": "A",
      "seatNumber": 6,
      "seatType": "VIP",
      "ticketTypeLabel": "HSSV/U22-GV",
      "price": 96000.00
    }
  ],
  "totalPrice": 216000.00,
  "discountReason": "Promotion WINTER2024(-10%)",
  "discountValue": 24000.00,
  "finalPrice": 216000.00,
  "status": "PENDING_PAYMENT",
  "bookedAt": "2024-11-17T19:15:00",
  "qrCode": null,
  "qrPayload": null,
  "paymentExpiresAt": "2024-11-17T19:30:00"
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
    "bookingId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
    "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
    "movieTitle": "Inception",
    "showtimeStartTime": "2025-11-18T19:30:00",
    "cinemaName": "CGV Vincom Center",
    "roomName": "IMAX 1",
    "seats": [
      {
        "rowLabel": "A",
        "seatNumber": 5,
        "seatType": "VIP",
        "price": 100000.00
      },
      {
        "rowLabel": "A",
        "seatNumber": 6,
        "seatType": "NORMAL",
        "price": 80000.00
      }
    ],
    "totalPrice": 180000.00,
    "discountReason": "Promotion SUMMER2025(-10%)",
    "discountValue": 18000.00,
    "finalPrice": 162000.00,
    "status": "CONFIRMED",
    "bookedAt": "2025-11-18T19:15:00",
    "qrCode": "https://res.cloudinary.com/demo/image/upload/qr_code_123.png",
    "qrPayload": "BOOKING_5e6f7a8b_SHOWTIME_3e4a8c9f",
    "paymentExpiresAt": null
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
  "qrCodeUrl": "https://res.cloudinary.com/demo/image/upload/qr_code_123.png"
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
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Seat lock expired or seats already booked",
  "details": "uri=/bookings/confirm"
}
```

**Common Causes:**
- Seat locks expired
- Invalid promotion code
- **Guest users attempting to use promotion codes** (guests must register to use promotions)
- Invalid snack IDs

### 404 Not Found
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Booking not found with id: {bookingId}",
  "details": "uri=/bookings/{bookingId}"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Seats already locked or booked by another user",
  "details": "uri=/bookings/lock"
}
```

### 410 Gone
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Seat lock has expired",
  "details": "uri=/bookings/confirm"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Access Denied: You are not authorized to access this booking",
  "details": "uri=/bookings/{bookingId}"
}
```

---

## Important Notes

1. **Seat Lock Duration**: Seats are locked for 10 minutes (600 seconds). After expiration, they become available again.
2. **Promotion Codes**: Applied during booking confirmation. Must be active and valid.
3. **QR Code**: Frontend should generate QR code after successful payment and update the booking.
4. **Guest Users**: 
   - Can create bookings but must track their userId for retrieval
   - **Cannot use promotion codes** - must register for an account to apply promotions
   - **Do not receive membership tier discounts** - only registered users with assigned tiers get discounts
   - Can still purchase snacks and tickets at regular prices
5. **Back Button**: Always call `/bookings/lock/back` when user navigates away to release seats immediately.
