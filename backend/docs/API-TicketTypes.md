# Ticket Types API Documentation

Base Path: `/ticket-types`

## Overview
Ticket types represent different pricing categories for movie tickets (e.g., Adult, Student, Senior, Child). Each ticket type has a modifier that adjusts the base seat price.

**Important**: Ticket type pricing is applied **after** base price and seat/showtime modifiers, but **before** membership discounts and promotions.

---

## Pricing Flow with Ticket Types

```
1. Base Price (from PriceBase)
   ↓
2. Apply Seat/Showtime Modifiers (VIP seat, weekend, 3D, etc.)
   = Seat Base Price
   ↓
3. Apply Ticket Type Modifier (Adult, Student, Senior, etc.)
   = Final Seat Price (shown in lock response)
   ↓
4. Apply Membership Discount (at booking confirmation)
   ↓
5. Apply Promotion Code (at booking confirmation)
   = Final Booking Price
```

---

## Endpoints

### 1. Get Active Ticket Types
**GET** `/ticket-types`

Returns all active ticket types. If `showtimeId` is provided, returns calculated prices for that specific showtime.

#### Query Parameters
- `showtimeId` (optional): UUID - Calculate prices for specific showtime
- `userId` (optional): UUID - For future personalization

#### Response (without showtimeId)
- **Status Code**: `200 OK`
- **Body**:
```json
[
  {
    "ticketTypeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "code": "adult",
    "label": "NGƯỜI LỚN",
    "price": null
  },
  {
    "ticketTypeId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
    "code": "student",
    "label": "HSSV/U22-GV",
    "price": null
  }
]
```

#### Response (with showtimeId)
- **Status Code**: `200 OK`
- **Body**:
```json
[
  {
    "ticketTypeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "code": "adult",
    "label": "NGƯỜI LỚN",
    "price": 100000.00
  },
  {
    "ticketTypeId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
    "code": "student",
    "label": "HSSV/U22-GV",
    "price": 80000.00
  },
  {
    "ticketTypeId": "c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f",
    "code": "senior",
    "label": "NGƯỜI CAO TUỔI",
    "price": 75000.00
  }
]
```

**Response Fields:**
- `ticketTypeId`: UUID of the ticket type
- `code`: Unique identifier code (e.g., "adult", "student", "senior")
- `label`: Display name for the ticket type
- `price`: Calculated price for the specific showtime (null if showtimeId not provided)

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Display ticket type options during seat selection
- Show price for each ticket type for the selected showtime

---

### 2. Get All Ticket Types (Admin)
**GET** `/ticket-types/admin`

Returns all ticket types including inactive ones (for admin management).

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
[
  {
    "ticketTypeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "code": "adult",
    "label": "NGƯỜI LỚN",
    "modifierType": "PERCENTAGE",
    "modifierValue": 0,
    "price": null,
    "active": true,
    "sortOrder": 0
  },
  {
    "ticketTypeId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
    "code": "student",
    "label": "HSSV/U22-GV",
    "modifierType": "PERCENTAGE",
    "modifierValue": -20,
    "price": null,
    "active": true,
    "sortOrder": 1
  }
]
```

**Response Fields:**
- `ticketTypeId`: UUID of the ticket type
- `code`: Unique identifier code
- `label`: Display name
- `modifierType`: Either `PERCENTAGE` or `FIXED_AMOUNT`
- `modifierValue`: Modifier value (percentage or fixed amount)
- `price`: Always null for admin view (calculated per showtime)
- `active`: Whether this ticket type is currently active
- `sortOrder`: Display order (lower numbers shown first)

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 3. Create Ticket Type (Admin)
**POST** `/ticket-types`

Creates a new ticket type.

#### Request Body
```json
{
  "code": "child",
  "label": "TRẺ EM",
  "modifierType": "PERCENTAGE",
  "modifierValue": -30,
  "active": true,
  "sortOrder": 3
}
```

**Field Descriptions:**
- `code`: Unique identifier (lowercase, underscores only, e.g., "adult", "student")
- `label`: Display name (e.g., "NGƯỜI LỚN", "HSSV/U22-GV")
- `modifierType`: Either `PERCENTAGE` or `FIXED_AMOUNT`
- `modifierValue`: 
  - For PERCENTAGE: value in percent (e.g., -20 = 20% discount, 0 = no change, 10 = 10% surcharge)
  - For FIXED_AMOUNT: amount to add/subtract (e.g., -15000 = 15,000 VND discount, 10000 = 10,000 VND surcharge)
- `active`: Whether this ticket type is available for selection
- `sortOrder`: Display order (lower = shown first)

#### Response
- **Status Code**: `201 CREATED`
- **Body**: TicketTypeResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 4. Update Ticket Type (Admin)
**PUT** `/ticket-types/{id}`

Updates an existing ticket type.

#### Path Parameters
- `id`: UUID of the ticket type

#### Request Body (all fields optional)
```json
{
  "label": "TRẺ EM (DƯỚI 13 TUỔI)",
  "modifierType": "PERCENTAGE",
  "modifierValue": -25,
  "active": true,
  "sortOrder": 3
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: TicketTypeResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 5. Delete Ticket Type (Admin)
**DELETE** `/ticket-types/{id}`

Deletes a ticket type. Soft deletes (sets active=false) if used in bookings, hard deletes otherwise.

#### Path Parameters
- `id`: UUID of the ticket type

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

## Ticket Type Codes (Common Examples)

| Code | Label | Description |
|------|-------|-------------|
| `adult` | NGƯỜI LỚN | Standard adult ticket (typically 0% modifier) |
| `student` | HSSV/U22-GV | Student/Teacher discount (typically -20% to -15%) |
| `senior` | NGƯỜI CAO TUỔI | Senior citizen discount (typically -25% to -20%) |
| `child` | TRẺ EM | Child discount (typically -30% to -25%) |
| `member` | HỘI VIÊN | Membership tier pricing (varies) |
| `double` | GHẾ ĐÔI | Couple seat (maps to COUPLE seat type, typically +40-70%) |

---

## Modifier Types

### PERCENTAGE
Applies a percentage change to the seat base price.

**Formula:** `finalPrice = baseSeatPrice × (1 + modifierValue / 100)`

**Examples:**
- `modifierValue: 0` → No change (100%)
- `modifierValue: -20` → 20% discount (80%)
- `modifierValue: 10` → 10% surcharge (110%)

**Use Case:**
- Standard ticket types (adult, student, senior, child)
- Most common for cinema pricing

---

### FIXED_AMOUNT
Adds or subtracts a fixed amount from the seat base price.

**Formula:** `finalPrice = baseSeatPrice + modifierValue`

**Examples:**
- `modifierValue: 0` → No change
- `modifierValue: -15000` → 15,000 VND discount
- `modifierValue: 10000` → 10,000 VND surcharge

**Use Case:**
- Special promotions or flat discounts
- Less common in cinema pricing

---

## Integration Examples

### Display Ticket Type Selection
```javascript
// When user selects a seat, show ticket type options
async function showTicketTypeOptions(showtimeId, seatId) {
  const response = await fetch(`/ticket-types?showtimeId=${showtimeId}`);
  const ticketTypes = await response.json();
  
  // Display ticket type picker
  ticketTypes.forEach(type => {
    const option = document.createElement('button');
    option.textContent = `${type.label} - ${formatPrice(type.price)}`;
    option.dataset.ticketTypeId = type.id;
    option.dataset.price = type.price;
    option.onclick = () => selectTicketType(seatId, type.id, type.price);
    
    container.appendChild(option);
  });
}
```

### Lock Seats with Ticket Types
```javascript
// When user confirms seat selection, include ticket types in lock request
async function lockSeatsWithTicketTypes(showtimeId, userId, selections) {
  // selections = [{ showtimeSeatId: "...", ticketTypeId: "..." }, ...]
  
  const response = await fetch('/bookings/lock', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      showtimeId: showtimeId,
      userId: userId,
      seats: selections
    })
  });
  
  const lockResponse = await response.json();
  
  // Lock response includes final price per seat with ticket type applied
  console.log('Total price:', lockResponse.totalPrice);
  lockResponse.lockedSeats.forEach(seat => {
    console.log(`Seat ${seat.rowLabel}${seat.seatNumber}: ${seat.price}`);
  });
}
```

### Admin: Create Student Discount
```javascript
async function createStudentTicketType() {
  await fetch('/ticket-types', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer <admin-token>',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      code: 'student',
      label: 'HSSV/U22-GV',
      modifierType: 'PERCENTAGE',
      modifierValue: -20, // 20% discount
      active: true,
      sortOrder: 1
    })
  });
}
```

---

## Pricing Calculation Example

**Scenario:** User selects a VIP seat for a 3D evening showtime on Saturday with a student ticket type

```
Step 1: Base Price
45,000 VND

Step 2: Apply Seat/Showtime Modifiers
+ VIP Seat: +20,000 VND (FIXED_AMOUNT)
+ 3D Format: +25,000 VND (FIXED_AMOUNT)
+ Evening Time: +10,000 VND (FIXED_AMOUNT)
+ Weekend: ×1.2 (PERCENTAGE, +20%)
= (45,000 + 20,000 + 25,000 + 10,000) × 1.2
= 100,000 × 1.2 = 120,000 VND (Seat Base Price)

Step 3: Apply Ticket Type Modifier (Student)
120,000 × (1 - 0.20) = 120,000 × 0.8 = 96,000 VND

Step 4: Apply Membership Discount (if any)
Silver Member -5%: 96,000 × 0.95 = 91,200 VND

Step 5: Apply Promotion (if any)
Code "WINTER2024" -10%: 91,200 × 0.9 = 82,080 VND

Final Price: 82,080 VND
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "code: must match pattern ^[a-z_]+$, modifierValue: must not be null",
  "details": "uri=/ticket-types"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "TicketType not found with id: {id}",
  "details": "uri=/ticket-types/{id}"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Access Denied: Admin access required",
  "details": "uri=/ticket-types"
}
```

---

## Important Notes

1. **Ticket Type Selection is Mandatory**: Users must select a ticket type for each seat during lock phase.

2. **Price Display**: Always call `GET /ticket-types?showtimeId={id}` to show accurate prices for the specific showtime.

3. **Seat Type Mapping**: The "double" ticket type automatically maps to COUPLE seat type for pricing.

4. **Modifier Application Order**:
   - First: Seat/showtime modifiers (VIP, 3D, weekend, etc.)
   - Second: Ticket type modifier
   - Third: Membership discount (at booking)
   - Fourth: Promotion code (at booking)

5. **Historical Accuracy**: Ticket type and price are stored in `BookingSeat` for each booking, preserving historical data even if ticket type prices change later.

6. **Soft Delete**: Deleting a ticket type that's used in bookings will set `active=false` instead of removing it from the database.

7. **Sort Order**: Ticket types are displayed in ascending `sortOrder`. Typically: adult (0), student (1), senior (2), child (3).

8. **Code Uniqueness**: Ticket type codes must be unique and follow pattern `^[a-z_]+$` (lowercase letters and underscores only).

9. **Immutable Modifiers**: Once created, `modifierType` and `modifierValue` cannot be changed directly. Create a new ticket type if pricing logic changes.

10. **Performance**: Cache ticket types on frontend, refresh when user navigates to seat selection page.
