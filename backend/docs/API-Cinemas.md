# Cinemas API Documentation

Base Path: `/cinemas`

## Overview
Endpoints for managing cinemas, rooms, and snacks. Includes CRUD operations for cinema locations, screening rooms, and concession items.

---

## Cinema Endpoints

### 1. Add Cinema (Admin Only)
**POST** `/cinemas`

Creates a new cinema location.

#### Request Body
```json
{
  "name": "string (required)",
  "location": "string (required)",
  "hotline": "string (required)"
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "id": "uuid",
  "name": "CGV Vincom Center",
  "location": "72 Le Thanh Ton, District 1, HCMC",
  "hotline": "1900 6017"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Update Cinema (Admin Only)
**PUT** `/cinemas/{cinemaId}`

Updates cinema details.

#### Path Parameters
- `cinemaId`: UUID of the cinema

#### Request Body (all fields optional)
```json
{
  "name": "string",
  "location": "string",
  "hotline": "string"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: CinemaDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 3. Delete Cinema (Admin Only)
**DELETE** `/cinemas/{cinemaId}`

Deletes a cinema.

#### Path Parameters
- `cinemaId`: UUID of the cinema

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 4. Get Cinema by ID
**GET** `/cinemas/{cinemaId}`

Retrieves cinema details.

#### Path Parameters
- `cinemaId`: UUID of the cinema

#### Response
- **Status Code**: `200 OK`
- **Body**: CinemaDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 5. Get All Cinemas
**GET** `/cinemas`

Retrieves all cinemas.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of CinemaDataResponse objects
```json
[
  {
    "id": "uuid",
    "name": "CGV Vincom Center",
    "location": "72 Le Thanh Ton, District 1, HCMC",
    "hotline": "1900 6017"
  },
  {
    "id": "uuid",
    "name": "Lotte Cinema Diamond Plaza",
    "location": "34 Le Duan, District 1, HCMC",
    "hotline": "1900 5454"
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

---

## Room Endpoints

### 6. Add Room (Admin Only)
**POST** `/cinemas/rooms`

Creates a new screening room within a cinema.

#### Request Body
```json
{
  "cinemaId": "uuid (required)",
  "name": "string (required)",
  "totalSeats": 100 (integer, required)
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "id": "uuid",
  "cinemaId": "uuid",
  "name": "Room 1",
  "totalSeats": 100
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 7. Update Room (Admin Only)
**PUT** `/cinemas/rooms/{roomId}`

Updates room details.

#### Path Parameters
- `roomId`: UUID of the room

#### Request Body (all fields optional)
```json
{
  "name": "string",
  "totalSeats": 100 (integer)
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: RoomDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 8. Delete Room (Admin Only)
**DELETE** `/cinemas/rooms/{roomId}`

Deletes a screening room.

#### Path Parameters
- `roomId`: UUID of the room

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 9. Get Room by ID
**GET** `/cinemas/rooms/{roomId}`

Retrieves room details.

#### Path Parameters
- `roomId`: UUID of the room

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "id": "uuid",
  "cinemaId": "uuid",
  "name": "Room 1 - IMAX",
  "totalSeats": 150
}
```

#### Authentication
- **Required**: No (Public endpoint)

---

### 10. Get All Rooms
**GET** `/cinemas/rooms`

Retrieves all rooms across all cinemas.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of RoomDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

---

## Snack Endpoints

### 11. Add Snack (Admin Only)
**POST** `/cinemas/snacks`

Creates a new snack/concession item.

#### Request Body
```json
{
  "cinemaId": "uuid (required)",
  "name": "string (required)",
  "description": "string (required)",
  "price": 50000.00 (number, required, positive),
  "category": "string (required)",
  "imageUrl": "string (required)",
  "availability": "string (required)"
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "id": "uuid",
  "cinemaId": "uuid",
  "name": "Popcorn Combo",
  "description": "Large popcorn + 2 drinks",
  "price": 120000.00,
  "category": "COMBO"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 12. Update Snack (Admin Only)
**PUT** `/cinemas/snacks/{snackId}`

Updates snack details.

#### Path Parameters
- `snackId`: UUID of the snack

#### Request Body (all fields optional)
```json
{
  "name": "string",
  "description": "string",
  "price": 50000.00 (number, positive),
  "category": "string",
  "imageUrl": "string",
  "availability": "string"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: SnackDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 13. Delete Snack (Admin Only)
**DELETE** `/cinemas/snacks/{snackId}`

Deletes a snack item.

#### Path Parameters
- `snackId`: UUID of the snack

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 14. Get Snack by ID
**GET** `/cinemas/snacks/{snackId}`

Retrieves snack details.

#### Path Parameters
- `snackId`: UUID of the snack

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "id": "uuid",
  "cinemaId": "uuid",
  "name": "Popcorn Combo",
  "description": "Large popcorn + 2 drinks",
  "price": 120000.00,
  "category": "COMBO"
}
```

#### Authentication
- **Required**: No (Public endpoint)

---

### 15. Get All Snacks
**GET** `/cinemas/snacks`

Retrieves all snacks across all cinemas.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of SnackDataResponse objects
```json
[
  {
    "id": "uuid",
    "cinemaId": "uuid",
    "name": "Popcorn Combo",
    "description": "Large popcorn + 2 drinks",
    "price": 120000.00,
    "category": "COMBO"
  },
  {
    "id": "uuid",
    "cinemaId": "uuid",
    "name": "Nachos",
    "description": "Crispy nachos with cheese",
    "price": 60000.00,
    "category": "SNACK"
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

---

## Data Models

### CinemaDataResponse
```json
{
  "id": "uuid",
  "name": "string",
  "location": "string",
  "hotline": "string"
}
```

### RoomDataResponse
```json
{
  "id": "uuid",
  "cinemaId": "uuid",
  "name": "string",
  "totalSeats": "integer"
}
```

### SnackDataResponse
```json
{
  "id": "uuid",
  "cinemaId": "uuid",
  "name": "string",
  "description": "string",
  "price": "number",
  "category": "string"
}
```

---

## Common Snack Categories

- `POPCORN` - Popcorn variations
- `DRINK` - Beverages (soda, water, juice)
- `COMBO` - Combo packages (popcorn + drink)
- `SNACK` - Other snacks (nachos, candy, etc.)
- `HOT_FOOD` - Hot food items (hot dogs, pizza, etc.)

---

## Error Responses

### 400 Bad Request
```json
{
  "message": "Invalid input",
  "errors": ["price must be positive"]
}
```

### 404 Not Found
```json
{
  "message": "Cinema/Room/Snack not found with id: {id}"
}
```

### 403 Forbidden
```json
{
  "message": "Admin access required"
}
```

### 409 Conflict
```json
{
  "message": "Cannot delete cinema with existing rooms or showtimes"
}
```

---

## Frontend Integration Examples

### Display Cinema Locations
```javascript
// Get all cinemas for location selector
const response = await fetch('/cinemas');
const cinemas = await response.json();

cinemas.forEach(cinema => {
  const option = document.createElement('option');
  option.value = cinema.id;
  option.textContent = `${cinema.name} - ${cinema.location}`;
  cinemaSelect.appendChild(option);
});
```

### Display Snack Menu
```javascript
// Get snacks for a specific cinema
async function loadSnacks(cinemaId) {
  const response = await fetch('/cinemas/snacks');
  const allSnacks = await response.json();
  
  // Filter by cinema
  const cinemaSnacks = allSnacks.filter(s => s.cinemaId === cinemaId);
  
  // Group by category
  const grouped = cinemaSnacks.reduce((acc, snack) => {
    if (!acc[snack.category]) acc[snack.category] = [];
    acc[snack.category].push(snack);
    return acc;
  }, {});
  
  displaySnackMenu(grouped);
}
```

---

## Important Notes

1. **Cinema Hierarchy**: Cinema → Rooms → Seats → Showtimes
   - Each cinema can have multiple rooms
   - Each room has multiple seats
   - Each room can have multiple showtimes

2. **Room Capacity**: `totalSeats` should match the actual number of seat records created for the room

3. **Snack Availability**: Track availability to show "Out of Stock" status

4. **Price Format**: All prices in VND (Vietnamese Dong), stored as decimal

5. **Deletion Restrictions**:
   - Cannot delete cinema with active rooms
   - Cannot delete room with active showtimes
   - Cannot delete snack with pending orders (if order system implemented)

6. **Location Format**: Recommended format: "Street Address, District, City"

7. **Hotline Format**: Recommended format: "1900 XXXX" or "+84 XX XXX XXXX"

8. **Room Naming**: Common conventions:
   - "Room 1", "Room 2" (standard)
   - "IMAX 1", "VIP 1" (special formats)
   - "Room A", "Room B" (letter-based)

9. **Image URLs**: Use CDN URLs for snack images (Cloudinary, S3, etc.)

10. **Category Consistency**: Use consistent category names across snacks for proper filtering
