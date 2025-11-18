# Showtimes API Documentation

Base Path: `/showtimes`

## Overview
Endpoints for managing movie showtimes, including scheduling, updates, and queries.

---

## Endpoints

### 1. Add Showtime (Admin Only)
**POST** `/showtimes`

Creates a new showtime for a movie in a specific room.

#### Request Body
```json
{
  "roomId": "123e4567-e89b-12d3-a456-426614174001",
  "movieId": "123e4567-e89b-12d3-a456-426614174000",
  "format": "IMAX",
  "startTime": "2024-11-17T19:30:00"
}
```

#### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| movieId | UUID | Yes | ID of the movie |
| roomId | UUID | Yes | ID of the screening room |
| format | String | Yes | Movie format (2D, 3D, IMAX, 4DX, etc.) |
| startTime | DateTime | Yes | Showtime start (ISO 8601 format) |

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "showtimeId": "123e4567-e89b-12d3-a456-426614174002",
  "room": {
    "roomId": "123e4567-e89b-12d3-a456-426614174001",
    "cinemaId": "123e4567-e89b-12d3-a456-426614174003",
    "roomType": "IMAX",
    "roomNumber": 1
  },
  "movie": {
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
  "format": "IMAX",
  "startTime": "2024-11-17T19:30:00"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Update Showtime (Admin Only)
**PUT** `/showtimes/{showtimeId}`

Updates showtime details.

#### Path Parameters
- `showtimeId`: UUID of the showtime

#### Request Body (all fields optional)
```json
{
  "roomId": "123e4567-e89b-12d3-a456-426614174001",
  "movieId": "123e4567-e89b-12d3-a456-426614174000",
  "format": "IMAX 3D",
  "startTime": "2024-11-17T20:00:00"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: ShowtimeDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Cannot update showtime with confirmed bookings
- Should reschedule seats if room or time changes

---

### 3. Delete Showtime (Admin Only)
**DELETE** `/showtimes/{showtimeId}`

Deletes a showtime.

#### Path Parameters
- `showtimeId`: UUID of the showtime

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Cannot delete showtime with confirmed bookings
- Releases all seat locks and cancels pending bookings

---

### 4. Get Showtime by ID
**GET** `/showtimes/{showtimeId}`

Retrieves showtime details.

#### Path Parameters
- `showtimeId`: UUID of the showtime

#### Response
- **Status Code**: `200 OK`
- **Body**: ShowtimeDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 5. Get All Showtimes
**GET** `/showtimes`

Retrieves all showtimes.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Notes
- Returns all showtimes (past and future)
- For user-facing pages, use upcoming showtimes endpoints

---

### 6. Get Showtimes by Movie
**GET** `/showtimes/movie/{movieId}`

Retrieves all showtimes for a specific movie.

#### Path Parameters
- `movieId`: UUID of the movie

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Movie detail page showing all available showtimes

---

### 7. Get Upcoming Showtimes by Movie
**GET** `/showtimes/movie/{movieId}/upcoming`

Retrieves only future showtimes for a specific movie.

#### Path Parameters
- `movieId`: UUID of the movie

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeDataResponse objects (filtered for future dates)

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- User booking flow: show only bookable showtimes

---

### 8. Get Showtimes by Room
**GET** `/showtimes/room/{roomId}`

Retrieves all showtimes scheduled in a specific room.

#### Path Parameters
- `roomId`: UUID of the room

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Admin scheduling: avoid room conflicts
- Cinema management: room utilization tracking

---

### 9. Get Showtimes by Movie and Date Range
**GET** `/showtimes/movie/{movieId}/date-range`

Retrieves showtimes for a movie within a specific date range.

#### Path Parameters
- `movieId`: UUID of the movie

#### Query Parameters
- `startDate`: datetime (required, ISO 8601 format) - Start of date range
- `endDate`: datetime (required, ISO 8601 format) - End of date range

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of ShowtimeDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Example
```
GET /showtimes/movie/123e4567-e89b-12d3-a456-426614174000/date-range?startDate=2024-11-17T00:00:00&endDate=2024-11-23T23:59:59
```

#### Use Case
- Weekly schedule display
- Date picker integration

---

## Data Model

### ShowtimeDataResponse
```json
{
  "showtimeId": "uuid",
  "room": {
    "roomId": "uuid",
    "cinemaId": "uuid",
    "roomType": "string",
    "roomNumber": "integer"
  },
  "movie": {
    "movieId": "uuid",
    "title": "string",
    "genre": "string",
    "description": "string",
    "duration": "integer",
    "minimumAge": "integer",
    "director": "string",
    "actors": "string",
    "posterUrl": "string",
    "posterCloudinaryId": "string",
    "trailerUrl": "string",
    "status": "string",
    "language": "string"
  },
  "format": "string",
  "startTime": "datetime"
}
```

---

## Movie Formats

Common format values:
- `2D` - Standard 2D screening
- `3D` - 3D screening (requires 3D glasses)
- `IMAX` - IMAX large format
- `IMAX 3D` - IMAX with 3D
- `4DX` - 4D experience with motion seats
- `ScreenX` - Multi-projection immersive format
- `Dolby Atmos` - Enhanced audio experience
- `STARIUM` - Premium large screen

---

## Frontend Integration Examples

### Display Movie Showtimes
```javascript
// Movie detail page
async function loadShowtimes(movieId) {
  const response = await fetch(`/showtimes/movie/${movieId}/upcoming`);
  const showtimes = await response.json();
  
  // Group by date and cinema
  const grouped = groupShowtimesByDateAndCinema(showtimes);
  displayShowtimeGrid(grouped);
}

function groupShowtimesByDateAndCinema(showtimes) {
  return showtimes.reduce((acc, showtime) => {
    const date = showtime.startTime.split('T')[0];
    const cinemaId = showtime.room.cinemaId;
    
    if (!acc[date]) acc[date] = {};
    if (!acc[date][cinemaId]) acc[date][cinemaId] = [];
    
    acc[date][cinemaId].push(showtime);
    return acc;
  }, {});
}
```

### Date Range Picker
```javascript
// Weekly schedule
async function loadWeeklySchedule(movieId, startDate) {
  const endDate = new Date(startDate);
  endDate.setDate(endDate.getDate() + 7);
  
  const url = `/showtimes/movie/${movieId}/date-range?` +
    `startDate=${startDate.toISOString()}&` +
    `endDate=${endDate.toISOString()}`;
  
  const response = await fetch(url);
  const showtimes = await response.json();
  
  displayWeeklyCalendar(showtimes);
}
```

### Cinema Schedule (Admin)
```javascript
// Check room availability
async function checkRoomConflicts(roomId, proposedStartTime, movieDuration) {
  const response = await fetch(`/showtimes/room/${roomId}`);
  const showtimes = await response.json();
  
  const proposedEnd = new Date(proposedStartTime);
  proposedEnd.setMinutes(proposedEnd.getMinutes() + movieDuration + 30); // +30 cleanup
  
  // Check for overlaps
  const conflicts = showtimes.filter(showtime => {
    const existingStart = new Date(showtime.startTime);
    const existingEnd = new Date(existingStart);
    existingEnd.setMinutes(existingEnd.getMinutes() + showtime.movie.duration + 30);
    
    return (proposedStartTime < existingEnd && proposedEnd > existingStart);
  });
  
  return conflicts.length > 0;
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "startTime: must be in the future, format: must not be blank",
  "details": "uri=/showtimes"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Showtime not found with id: {showtimeId}",
  "details": "uri=/showtimes/{showtimeId}"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Access Denied: Admin access required",
  "details": "uri=/showtimes"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Room already booked for this time slot",
  "details": "uri=/showtimes"
}
```

---

## Important Notes

1. **Room Conflicts**: System should prevent scheduling overlapping showtimes in the same room

2. **Buffer Time**: Consider 15-30 minute buffer between showtimes for:
   - Audience exit
   - Theater cleanup
   - Next audience entry

3. **Calculation Formula**:
   ```
   Showtime End = Start Time + Movie Duration + Buffer Time
   ```

4. **Past Showtimes**: 
   - Keep for historical data and reporting
   - Filter out for user-facing booking interfaces

5. **Seat Generation**: When creating a showtime, system should automatically:
   - Create showtime_seat records for all seats in the room
   - Calculate initial prices based on seat type and price modifiers

6. **Format Pricing**: Different formats may have different base prices or modifiers

7. **DateTime Format**: 
   - Use ISO 8601 format: `2024-11-17T19:30:00`
   - Store in UTC, convert to local timezone for display

8. **Cancellation Policy**: 
   - Cannot cancel showtime with confirmed bookings
   - Refund all bookings before cancellation

9. **Update Restrictions**:
   - Cannot change room if bookings exist (different seat layout)
   - Cannot change time to past
   - Time changes should notify affected bookings

10. **Nested Objects**: Response includes full movie and room details to minimize frontend API calls
