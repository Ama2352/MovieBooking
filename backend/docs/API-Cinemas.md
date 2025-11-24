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
  "name": "CGV Vincom Center",
  "address": "72 Le Thanh Ton, District 1, HCMC",
  "hotline": "1900 6017"
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "name": "CGV Vincom Center",
  "address": "72 Le Thanh Ton, District 1, HCMC",
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
  "name": "CGV Vincom Center Updated",
  "address": "72 Le Thanh Ton, District 1, HCMC",
  "hotline": "1900 6017"
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
    "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
    "name": "CGV Vincom Center",
    "address": "72 Le Thanh Ton, District 1, HCMC",
    "hotline": "1900 6017"
  },
  {
    "cinemaId": "7b2e9a1c-4567-89ab-cdef-123456789012",
    "name": "Lotte Cinema Diamond Plaza",
    "address": "34 Le Duan, District 1, HCMC",
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
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "roomType": "IMAX",
  "roomNumber": 1
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "roomId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "roomType": "IMAX",
  "roomNumber": 1
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
  "roomType": "VIP",
  "roomNumber": 2
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
  "roomId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "roomType": "IMAX",
  "roomNumber": 1
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
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "name": "Popcorn Combo",
  "description": "Large popcorn + 2 drinks",
  "price": 120000.00,
  "type": "COMBO",
  "imageUrl": "https://cdn.example.com/popcorn-combo.jpg",
  "imageCloudinaryId": "snacks/popcorn_combo_abc123"
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "snackId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "name": "Popcorn Combo",
  "description": "Large popcorn + 2 drinks",
  "price": 120000.00,
  "type": "COMBO",
  "imageUrl": "https://cdn.example.com/popcorn-combo.jpg",
  "imageCloudinaryId": "snacks/popcorn_combo_abc123"
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
  "name": "Mega Popcorn Combo",
  "description": "Extra large popcorn + 3 drinks",
  "price": 150000.00,
  "type": "COMBO",
  "imageUrl": "https://cdn.example.com/mega-popcorn-combo.jpg",
  "imageCloudinaryId": "snacks/mega_popcorn_combo_def456"
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
  "snackId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "name": "Popcorn Combo",
  "description": "Large popcorn + 2 drinks",
  "price": 120000.00,
  "type": "COMBO",
  "imageUrl": "https://cdn.example.com/popcorn-combo.jpg",
  "imageCloudinaryId": "snacks/popcorn_combo_abc123"
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
    "snackId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
    "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
    "name": "Popcorn Combo",
    "description": "Large popcorn + 2 drinks",
    "price": 120000.00,
    "type": "COMBO",
    "imageUrl": "https://cdn.example.com/popcorn-combo.jpg",
    "imageCloudinaryId": "snacks/popcorn_combo_abc123"
  },
  {
    "snackId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
    "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
    "name": "Nachos",
    "description": "Crispy nachos with cheese",
    "price": 60000.00,
    "type": "SNACK",
    "imageUrl": "https://cdn.example.com/nachos.jpg",
    "imageCloudinaryId": "snacks/nachos_xyz789"
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

---

### 16. Get Movies by Cinema and Status
**GET** `/cinemas/{cinemaId}/movies`

Retrieves all movies showing at a specific cinema filtered by status (SHOWING or UPCOMING).

#### Path Parameters
- `cinemaId`: UUID of the cinema

#### Query Parameters
- `status`: MovieStatus (required) - Filter by movie status (SHOWING or UPCOMING)

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of MovieDataResponse objects
```json
[
  {
    "movieId": "123e4567-e89b-12d3-a456-426614174000",
    "title": "Oppenheimer",
    "genre": "Biography, Drama, History",
    "description": "The story of J. Robert Oppenheimer and his role in developing the atomic bomb",
    "duration": 180,
    "minimumAge": 13,
    "director": "Christopher Nolan",
    "actors": "Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.",
    "posterUrl": "https://cdn.example.com/posters/oppenheimer.jpg",
    "posterCloudinaryId": "movies/oppenheimer_poster",
    "trailerUrl": "https://youtube.com/watch?v=xyz123",
    "status": "SHOWING",
    "language": "English"
  },
  {
    "movieId": "456e7890-e12b-34d5-a678-901234567890",
    "title": "The Dark Knight",
    "genre": "Action, Crime, Drama",
    "description": "Batman faces the Joker in Gotham City",
    "duration": 152,
    "minimumAge": 13,
    "director": "Christopher Nolan",
    "actors": "Christian Bale, Heath Ledger, Aaron Eckhart",
    "posterUrl": "https://cdn.example.com/posters/dark-knight.jpg",
    "posterCloudinaryId": "movies/dark_knight_poster",
    "trailerUrl": "https://youtube.com/watch?v=abc789",
    "status": "SHOWING",
    "language": "English"
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

#### Examples
- Currently showing: `GET /cinemas/{cinemaId}/movies?status=SHOWING`
- Coming soon: `GET /cinemas/{cinemaId}/movies?status=UPCOMING`

#### Notes
- Returns distinct movies that have showtimes at the specified cinema
- Movies are filtered by their current status
- An empty array is returned if no movies match the criteria

---

## Data Models

### CinemaDataResponse
```json
{
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "name": "CGV Vincom Center",
  "address": "72 Le Thanh Ton, District 1, HCMC",
  "hotline": "1900 6017"
}
```

### RoomDataResponse
```json
{
  "roomId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "roomType": "IMAX",
  "roomNumber": 1
}
```

### SnackDataResponse
```json
{
  "snackId": "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f",
  "cinemaId": "3e4a8c9f-1234-5678-90ab-cdef12345678",
  "name": "Popcorn Combo",
  "description": "Large popcorn + 2 drinks",
  "price": 120000.00,
  "type": "COMBO",
  "imageUrl": "https://cdn.example.com/popcorn-combo.jpg",
  "imageCloudinaryId": "snacks/popcorn_combo_abc123"
}
```

### MovieDataResponse
```json
{
  "movieId": "123e4567-e89b-12d3-a456-426614174000",
  "title": "Oppenheimer",
  "genre": "Biography, Drama, History",
  "description": "The story of J. Robert Oppenheimer and his role in developing the atomic bomb",
  "duration": 180,
  "minimumAge": 13,
  "director": "Christopher Nolan",
  "actors": "Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.",
  "posterUrl": "https://cdn.example.com/posters/oppenheimer.jpg",
  "posterCloudinaryId": "movies/oppenheimer_poster",
  "trailerUrl": "https://youtube.com/watch?v=xyz123",
  "status": "SHOWING",
  "language": "English"
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
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "price: must be positive",
  "details": "uri=/cinemas/snacks"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Cinema not found with id: {id}",
  "details": "uri=/cinemas/{id}"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Access Denied: Admin access required",
  "details": "uri=/cinemas"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Cannot delete cinema with existing rooms or showtimes",
  "details": "uri=/cinemas/{id}"
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
  option.value = cinema.cinemaId;
  option.textContent = `${cinema.name} - ${cinema.address}`;
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

### Display Movies at Cinema
```javascript
// Get currently showing movies at a specific cinema
async function loadCinemaMovies(cinemaId) {
  const response = await fetch(`/cinemas/${cinemaId}/movies?status=SHOWING`);
  const movies = await response.json();
  
  // Display movies
  movies.forEach(movie => {
    const card = createMovieCard(movie);
    movieGrid.appendChild(card);
  });
}

// Get upcoming movies at a specific cinema
async function loadUpcomingCinemaMovies(cinemaId) {
  const response = await fetch(`/cinemas/${cinemaId}/movies?status=UPCOMING`);
  const movies = await response.json();
  displayUpcomingMovies(movies);
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
