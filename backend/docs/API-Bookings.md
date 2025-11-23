# Bookings API Documentation

Base Path: `/bookings`

## Overview
Endpoints for managing movie ticket bookings with **session-based seat locking** supporting both authenticated users and guests.

**Authentication:**
- **Authenticated Users**: Include JWT token in `Authorization: Bearer <token>` header
- **Guest Users**: Include `X-Session-Id: <uuid>` header (generate UUID on frontend)

**Session Management:**
- Each session (user or guest) can have ONE active lock per showtime
- Locks auto-release after 10 minutes
- Frontend must manage session IDs for guests (store in localStorage/sessionStorage)

**Ticket Type Integration:**
- Each seat MUST have a ticket type selected during lock phase
- Use `GET /ticket-types?showtimeId={id}` to get available ticket types with prices
- See [API-TicketTypes.md](API-TicketTypes.md) for details

---

## Booking Flow
1. **Generate Session ID** (guests only) - `crypto.randomUUID()` on frontend
2. **Get Ticket Types** - `GET /ticket-types?showtimeId={id}`
3. **Lock Seats** - `POST /seat-locks` (with X-Session-Id header)
4. **Check Availability** - `GET /seat-locks/availability/{showtimeId}`
5. **Confirm Booking** - `POST /bookings/confirm` (creates guest User account if needed)
6. **Initiate Payment** - See Payment API or use `/checkout` for atomic flow
7. **View Bookings** - `GET /bookings/my-bookings`

---

## Endpoints

### 1. Lock Seats
**POST** `/seat-locks`

Locks selected seats for 10 minutes using session-based locking.

#### Headers
- `Authorization: Bearer <token>` (authenticated users)
- `X-Session-Id: <uuid>` (guest users - must be valid UUID format)

#### Request Body
```json
{
  "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
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

**Session Context**: lockOwnerId extracted from JWT (authenticated) or X-Session-Id header (guest).

**Note**: Each seat must have a ticket type selected. Ticket types must be active for the showtime.

#### Validation Rules
- Maximum seats per booking: 10 (configurable)
- Each seat must have a ticket type assigned
- Ticket types must be active for the showtime
- Seats must be AVAILABLE (not LOCKED or BOOKED)
- **Session can only have ONE active lock per showtime** (multi-tab protection)
- If session has locks for different showtime, they are auto-released

#### Error Responses
- `400 Bad Request`: 
  - Exceeds maximum seats per booking
  - Missing ticket type for one or more seats
  - Ticket type not available for this showtime
  - Invalid or missing X-Session-Id header (guests)
- `401 Unauthorized`: Missing or invalid JWT/session identifier
- `404 Not Found`: Showtime, seat, or ticket type not found
- `409 Conflict`: 
  - Seats already locked/booked by another session
  - Session already has active lock for this showtime (multi-tab attempt)
- `423 Locked`: Unable to acquire Redis lock (concurrent access)

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
      "ticketTypeLabel": "NGÆ¯á»œI Lá»N",
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
- `price`: Final seat price with ticket type applied (base + modifiers + ticket type)
- `ticketTypeId` / `ticketTypeLabel`: Selected ticket type for each seat
- `totalPrice`: Sum of all seat prices (with ticket types)
- `lockKey`: Internal Redis lock identifier

#### Authentication
- **Required**: Yes
  - Authenticated: JWT token in `Authorization` header
  - Guest: UUID in `X-Session-Id` header

---

### 2. Check Seat Availability
**GET** `/seat-locks/availability/{showtimeId}`

Returns available, locked, and booked seats. **READ-ONLY** - does not release locks.

#### Headers (Optional)
- `Authorization: Bearer <token>` (to see your own locks)
- `X-Session-Id: <uuid>` (to see your own locks)

#### Path Parameters
- `showtimeId`: UUID of the showtime

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
  "yourActiveLocks": [
    "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
  ],
  "message": "Seat availability retrieved"
}
```

**Note**: `yourActiveLocks` only included if session context provided in headers.

#### Authentication
- **Required**: No (public endpoint)
- Optional session headers to see your own locks

---

### 3. Release Locked Seats
**DELETE** `/seat-locks/showtime/{showtimeId}`

Manually releases locked seats before confirmation or timeout.

#### Headers
- `Authorization: Bearer <token>` (authenticated users)
- `X-Session-Id: <uuid>` (guest users)

#### Path Parametersameters
- `showtimeId`: UUID of the showtime

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty
- **Idempotent**: Returns 200 even if no locks found

#### Authentication
- **Required**: Yes (JWT or X-Session-Id)

---


### 5. Confirm Booking
**POST** `/bookings/confirm`

Confirms booking after seat locking. For guests, creates User account with role=GUEST.

#### Headers
- `Authorization: Bearer <token>` (authenticated users)
- `X-Session-Id: <uuid>` (guest users)

#### Request Body
```json
{
  "lockId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "promotionCode": "WINTER2024",
  "guestInfo": {
    "email": "guest@example.com",
    "fullName": "John Doe",
    "phoneNumber": "+84901234567"
  },
  "snackCombos": [
    { "snackId": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b", "quantity": 2 },
    { "snackId": "f2a3b4c5-d6e7-8f9a-0b1c-2d3e4f5a6b7c", "quantity": 1 }
  ]
}
```

#### Request Fields
- `lockId`: UUID of the seat lock (required)
- `promotionCode`: Optional promotion code (registered users only)
- `guestInfo`: Required for guests, omit for authenticated users
  - `email`: Valid email (must be unique)
  - `fullName`: Guest's full name
  - `phoneNumber`: Contact number
- `snackCombos`: Optional array of snack selections

**Important**: 
- Guest users cannot use promotion codes (must register for account)
- Lock must be active and belong to requesting session
- For guests, creates User record with role=GUEST

#### Process Flow
1. Validates seat lock belongs to session and hasn't expired
2. For guests: Creates User account with guestInfo
3. Creates Booking with PENDING_PAYMENT status
4. Transitions seats from LOCKED â†’ BOOKED
5. Applies promotion code discount (if applicable)
6. Returns booking details for payment

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
      "ticketTypeLabel": "NGÆ¯á»œI Lá»N",
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
- **Required**: Yes (Bearer Token or X-Session-Id)
- **Supports**: Both registered users and guest users (creates account for guests)

### 423 Locked
`json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Unable to acquire lock: concurrent access detected",
  "details": "uri=/seat-locks"
}
`

**Cause:** Redis distributed lock acquisition failed. Client should retry.