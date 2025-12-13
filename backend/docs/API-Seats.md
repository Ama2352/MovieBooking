# Seats & Showtime Seats API Documentation

## Overview
Two separate but related resources:
- **Seats** (`/seats`): Physical seat templates in rooms
- **Showtime Seats** (`/showtime-seats`): Seat instances for specific showtimes with booking status

---

# Seats API

Base Path: `/seats`

## Seat Management Endpoints

### 1. Add Seat (Admin Only)
**POST** `/seats`

Creates a single seat in a room.

#### Request Body
```json
{
  "roomId": "123e4567-e89b-12d3-a456-426614174001",
  "seatNumber": 5,
  "rowLabel": "A",
  "seatType": "NORMAL"
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "seatId": "123e4567-e89b-12d3-a456-426614174004",
  "roomId": "123e4567-e89b-12d3-a456-426614174001",
  "roomNumber": "1",
  "cinemaName": "CGV Vincom Center",
  "seatNumber": 5,
  "rowLabel": "A",
  "seatType": "NORMAL"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Generate Seats Automatically (Admin Only)
**POST** `/seats/generate`

Auto-generates seats in a grid pattern for a room.

#### Request Body
```json
{
  "roomId": "123e4567-e89b-12d3-a456-426614174001",
  "rows": 10,
  "seatsPerRow": 15,
  "vipRows": ["A", "B"],
  "coupleRows": ["J"]
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "totalSeatsCreated": 150,
  "normalSeats": 105,
  "vipSeats": 30,
  "coupleSeats": 15,
  "seats": [
    {
      "seatId": "123e4567-e89b-12d3-a456-426614174004",
      "roomId": "123e4567-e89b-12d3-a456-426614174001",
      "roomNumber": "1",
      "cinemaName": "CGV Vincom Center",
      "seatNumber": 1,
      "rowLabel": "A",
      "seatType": "VIP"
    }
  ]
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Generates seats in pattern: A1-A15, B1-B15, ..., J1-J15
- Row labels: A-Z, then AA-AZ, BA-BZ, etc.
- VIP and couple rows applied as specified
- Total seats = rows × seatsPerRow

---

### 3. Get Row Labels Preview
**GET** `/seats/row-labels`

Returns row label preview for a given number of rows.

#### Query Parameters
- `rows`: integer (default: 10) - Number of rows

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "numberOfRows": 10,
  "labels": ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"]
}
```

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Frontend display for room setup
- Shows what row labels will be used before generating seats

---

### 4. Update Seat (Admin Only)
**PUT** `/seats/{seatId}`

Updates a seat's properties.

#### Path Parameters
- `seatId`: UUID of the seat

#### Request Body (all fields optional)
```json
{
  "seatNumber": 6,
  "rowLabel": "A",
  "seatType": "VIP"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: SeatDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 5. Delete Seat (Admin Only)
**DELETE** `/seats/{seatId}`

Deletes a seat from a room.

#### Path Parameters
- `seatId`: UUID of the seat

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 6. Get Seat by ID
**GET** `/seats/{seatId}`

Retrieves seat details.

#### Path Parameters
- `seatId`: UUID of the seat

#### Response
- **Status Code**: `200 OK`
- **Body**: SeatDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 7. Get All Seats
**GET** `/seats`

Retrieves all seats across all rooms.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of SeatDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

---

### 8. Get Seats by Room
**GET** `/seats/room/{roomId}`

Retrieves all seats for a specific room.

#### Path Parameters
- `roomId`: UUID of the room

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of SeatDataResponse objects
```json
[
  {
    "seatId": "123e4567-e89b-12d3-a456-426614174004",
    "roomId": "123e4567-e89b-12d3-a456-426614174001",
    "roomNumber": "1",
    "cinemaName": "CGV Vincom Center",
    "seatNumber": 1,
    "rowLabel": "A",
    "seatType": "VIP"
  },
  {
    "seatId": "123e4567-e89b-12d3-a456-426614174005",
    "roomId": "123e4567-e89b-12d3-a456-426614174001",
    "roomNumber": "1",
    "cinemaName": "CGV Vincom Center",
    "seatNumber": 2,
    "rowLabel": "A",
    "seatType": "VIP"
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

---

### 9. Get Seat Layout for Showtime
**GET** `/seats/layout`

Returns the complete seat layout for a specific showtime, including seat details and current status (AVAILABLE, LOCKED, or BOOKED).

#### Query Parameters
- `showtime_id`: UUID of the showtime (required)

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
[
  {
    "showtimeSeatId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
    "seatId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
    "rowLabel": "A",
    "seatNumber": 1,
    "seatType": "NORMAL",
    "status": "AVAILABLE",
    "price": 100000.00
  },
  {
    "showtimeSeatId": "9f0b1c2d-3e4f-5a6b-7c8d-9e0f1a2b3c4d",
    "seatId": "a1b2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "rowLabel": "A",
    "seatNumber": 2,
    "seatType": "VIP",
    "status": "LOCKED",
    "price": 120000.00
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Display seat map on seat selection page
- Show real-time seat availability with status indicators
- Alternative to `/showtime-seats/showtime/{showtimeId}` with a query parameter approach

---

# Showtime Seats API

Base Path: `/showtime-seats`

## Showtime Seat Management Endpoints

### 1. Get Showtime Seat by ID
**GET** `/showtime-seats/{id}`

Retrieves a specific showtime seat instance.

#### Path Parameters
- `id`: UUID of the showtime seat

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "showtimeSeatId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
  "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "seatId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "rowLabel": "A",
  "seatNumber": 5,
  "seatType": "VIP",
  "status": "AVAILABLE",
  "price": 120000.00,
  "priceBreakdown": "{\"basePrice\":100000,\"modifiers\":[{\"name\":\"VIP Seat\",\"type\":\"FIXED_AMOUNT\",\"value\":20000}],\"finalPrice\":120000}"
}
```

#### Authentication
- **Required**: No (Public endpoint)

---

### 2. Get Showtime Seats by Showtime
**GET** `/showtime-seats/showtime/{showtimeId}`

Retrieves all seat instances for a showtime.

#### Path Parameters
- `showtimeId`: UUID of the showtime

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeSeatDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Seat selection page: display all seats with their status and prices

---

### 3. Get Available Showtime Seats
**GET** `/showtime-seats/showtime/{showtimeId}/available`

Retrieves only available seats for a showtime.

#### Path Parameters
- `showtimeId`: UUID of the showtime

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeSeatDataResponse objects (filtered for AVAILABLE status)

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Quick check for available seats
- Filter seat picker to only show bookable seats

---

### 4. Update Showtime Seat (Admin Only)
**PUT** `/showtime-seats/{id}`

Manually updates a showtime seat's status or price.

#### Path Parameters
- `id`: UUID of the showtime seat

#### Request Body (all fields optional)
```json
{
  "status": "AVAILABLE",
  "price": 150000.00
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: ShowtimeSeatDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Used for manual seat management
- Status changes should respect booking integrity

---

### 5. Reset Showtime Seat Status (Admin Only)
**PUT** `/showtime-seats/{id}/reset`

Resets a showtime seat status to AVAILABLE.

#### Path Parameters
- `id`: UUID of the showtime seat

#### Response
- **Status Code**: `200 OK`
- **Body**: ShowtimeSeatDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Use Case
- Release stuck locks
- Fix erroneous bookings
- Emergency seat release

---

### 6. Recalculate Prices for Showtime (Admin Only)
**POST** `/showtime-seats/showtime/{showtimeId}/recalculate-prices`

Recalculates prices for all seats in a showtime based on current price modifiers.

#### Path Parameters
- `showtimeId`: UUID of the showtime

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeSeatDataResponse objects with updated prices

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Use Case
- After updating price modifiers
- After changing base prices
- Applying new pricing rules to existing showtimes

---

## Seat Type Enum

| Type | Description | Typical Price Impact |
|------|-------------|---------------------|
| `NORMAL` | Standard seat | Base price |
| `VIP` | Premium seat (more space, better position) | +20-50% |
| `COUPLE` | Double seat for two people | +30-70% |

---

## Seat Status Enum

| Status | Description | Bookable |
|--------|-------------|----------|
| `AVAILABLE` | Seat is free for booking | ✅ Yes |
| `LOCKED` | Temporarily locked during checkout | ❌ No |
| `BOOKED` | Confirmed booking | ❌ No |

---

## Data Models

### SeatDataResponse (Template)
```json
{
  "seatId": "uuid",
  "roomId": "uuid",
  "roomNumber": "string",
  "cinemaName": "string",
  "seatNumber": "integer",
  "rowLabel": "string",
  "seatType": "NORMAL|VIP|COUPLE"
}
```

### ShowtimeSeatDataResponse (Instance)
```json
{
  "showtimeSeatId": "8f9a0b1c-2d3e-4f5a-6b7c-8d9e0f1a2b3c",
  "showtimeId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "seatId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "rowLabel": "A",
  "seatNumber": 5,
  "seatType": "VIP",
  "status": "AVAILABLE",
  "price": 120000.00,
  "priceBreakdown": "{\"basePrice\":100000,\"modifiers\":[{\"name\":\"VIP Seat\",\"type\":\"FIXED_AMOUNT\",\"value\":20000}],\"finalPrice\":120000}"
}
```

### Price Breakdown JSON Structure
```json
{
  "basePrice": 100000.00,
  "modifiers": [
    {
      "name": "VIP Seat",
      "type": "FIXED_AMOUNT",
      "value": 20000.00
    },
    {
      "name": "Weekend",
      "type": "PERCENTAGE",
      "value": 1.2
    }
  ],
  "finalPrice": 144000.00
}
```

---

## Frontend Integration Examples

### Display Seat Map
```javascript
// Load seat map for showtime
async function loadSeatMap(showtimeId) {
  const response = await fetch(`/showtime-seats/showtime/${showtimeId}`);
  const seats = await response.json();
  
  // Group by row
  const seatsByRow = seats.reduce((acc, seat) => {
    if (!acc[seat.rowLabel]) acc[seat.rowLabel] = [];
    acc[seat.rowLabel].push(seat);
    return acc;
  }, {});
  
  // Sort rows and seats
  Object.keys(seatsByRow).sort().forEach(row => {
    seatsByRow[row].sort((a, b) => a.seatNumber - b.seatNumber);
  });
  
  renderSeatMap(seatsByRow);
}

function renderSeatMap(seatsByRow) {
  const container = document.getElementById('seat-map');
  
  Object.entries(seatsByRow).forEach(([rowLabel, seats]) => {
    const rowDiv = document.createElement('div');
    rowDiv.className = 'seat-row';
    
    // Row label
    const label = document.createElement('span');
    label.className = 'row-label';
    label.textContent = rowLabel;
    rowDiv.appendChild(label);
    
    // Seats
    seats.forEach(seat => {
      const seatBtn = document.createElement('button');
      seatBtn.className = `seat seat-${seat.status.toLowerCase()} seat-${seat.seatType.toLowerCase()}`;
      seatBtn.textContent = seat.seatNumber;
      seatBtn.disabled = seat.status !== 'AVAILABLE';
      seatBtn.dataset.seatId = seat.showtimeSeatId;
      seatBtn.dataset.price = seat.price;
      
      if (seat.status === 'AVAILABLE') {
        seatBtn.onclick = () => toggleSeat(seat);
      }
      
      rowDiv.appendChild(seatBtn);
    });
    
    container.appendChild(rowDiv);
  });
}
```

### Generate Room Seats (Admin)
```javascript
// Auto-generate seats for a new room
async function generateSeats(roomId, rows, seatsPerRow) {
  const response = await fetch('/seats/generate', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer <token>',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      roomId: roomId,
      rows: rows,
      seatsPerRow: seatsPerRow,
      vipRows: ['A', 'B'], // First two rows as VIP
      coupleRows: ['J'] // Last row as couple seats
    })
  });
  
  const result = await response.json();
  console.log(`Created ${result.totalSeatsCreated} seats:`, {
    normal: result.normalSeats,
    vip: result.vipSeats,
    couple: result.coupleSeats
  });
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "seatNumber: must be positive, rowLabel: must not be blank",
  "details": "uri=/seats"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Seat not found with id: {seatId}",
  "details": "uri=/seats/{seatId}"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Access Denied: Admin access required",
  "details": "uri=/seats"
}
```

---

## Important Notes

1. **Seat vs Showtime Seat**:
   - **Seat**: Physical template in room (created once)
   - **Showtime Seat**: Instance for specific showtime (created per showtime)

2. **Automatic Creation**: When a showtime is created, system automatically:
   - Duplicates all room seats as showtime seats
   - Calculates prices based on modifiers
   - Sets all statuses to AVAILABLE

3. **Seat Labels**: Automatically generated as `{rowLabel}{seatNumber}` (e.g., "A5", "B12")

4. **Row Label Generation**:
   - 1-26 rows: A-Z
   - 27-52 rows: AA-AZ
   - 53+ rows: BA-BZ, CA-CZ, etc.

5. **Couple Seats**: 
   - Typically wider seats for two people
   - Priced higher than normal seats
   - Usually in back rows for privacy

6. **VIP Seats**:
   - Better location (center, back rows)
   - More legroom
   - Premium pricing

7. **Price Calculation**: Final price = Base Price + Seat Type Modifier + Format Modifier + Time Modifier + Day Modifier

8. **Lock Duration**: Seat locks expire after 10 minutes

9. **Status Transitions**:
   - AVAILABLE → LOCKED (user selects seat)
   - LOCKED → BOOKED (payment confirmed)
   - LOCKED → AVAILABLE (lock expires or user cancels)
   - BOOKED → AVAILABLE (admin refund/cancel)

10. **Performance**: Cache seat maps on frontend, only refresh when user enters seat selection page
