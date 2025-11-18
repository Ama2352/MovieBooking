# Movies API Documentation

Base Path: `/movies`

## Overview
Endpoints for managing movies, including CRUD operations, search, and filtering by status, genre, and title.

---

## Endpoints

### 1. Add Movie (Admin Only)
**POST** `/movies`

Creates a new movie in the system.

#### Request Body
```json
{
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

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
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

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Update Movie (Admin Only)
**PUT** `/movies/{movieId}`

Updates an existing movie's details.

#### Path Parameters
- `movieId`: UUID of the movie

#### Request Body
```json
{
  "title": "Oppenheimer (Director's Cut)",
  "genre": "Biography, Drama, History",
  "description": "Extended version with additional scenes",
  "duration": 195,
  "minimumAge": 13,
  "director": "Christopher Nolan",
  "actors": "Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.",
  "posterUrl": "https://cdn.example.com/posters/oppenheimer_dc.jpg",
  "posterCloudinaryId": "movies/oppenheimer_dc_poster",
  "trailerUrl": "https://youtube.com/watch?v=abc456",
  "status": "SHOWING",
  "language": "English"
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: MovieDataResponse object (same structure as create)

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 3. Delete Movie (Admin Only)
**DELETE** `/movies/{movieId}`

Deletes a movie from the system.

#### Path Parameters
- `movieId`: UUID of the movie

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Soft delete: movie may be marked inactive rather than removed from database
- Cannot delete movies with active showtimes or bookings

---

### 4. Get Movie by ID
**GET** `/movies/{movieId}`

Retrieves details of a specific movie.

#### Path Parameters
- `movieId`: UUID of the movie

#### Response
- **Status Code**: `200 OK`
- **Body**: MovieDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 5. Get All Movies / Search Movies
**GET** `/movies`

Retrieves all movies or searches movies with filters.

#### Query Parameters (all optional)
- `title`: string - Search by movie title (partial match)
- `genre`: string - Filter by genre (partial match)
- `status`: string - Filter by status (SHOWING, UPCOMING)

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
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

#### Examples
- Get all movies: `GET /movies`
- Search by title: `GET /movies?title=oppenheimer`
- Filter by genre: `GET /movies?genre=action`
- Filter by status: `GET /movies?status=SHOWING`
- Combined: `GET /movies?status=SHOWING&genre=action`

---

### 6. Search Movies by Title
**GET** `/movies/search/title`

Searches movies by title (partial match, case-insensitive).

#### Query Parameters
- `title`: string (required) - Movie title to search

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of MovieDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Example
- `GET /movies/search/title?title=spider`
- Returns: Spider-Man, Spider-Man 2, etc.

---

### 7. Filter Movies by Status
**GET** `/movies/filter/status`

Filters movies by status.

#### Query Parameters
- `status`: string (required) - Movie status (SHOWING, UPCOMING)

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of MovieDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Examples
- Currently showing: `GET /movies/filter/status?status=SHOWING`
- Coming soon: `GET /movies/filter/status?status=UPCOMING`

---

### 8. Filter Movies by Genre
**GET** `/movies/filter/genre`

Filters movies by genre (partial match).

#### Query Parameters
- `genre`: string (required) - Genre to filter

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of MovieDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Examples
- Action movies: `GET /movies/filter/genre?genre=action`
- Horror movies: `GET /movies/filter/genre?genre=horror`

---

## Movie Status Enum

| Status | Description |
|--------|-------------|
| `SHOWING` | Currently showing in theaters |
| `UPCOMING` | Coming soon, not yet released |

---

## Field Constraints

### Movie Fields
| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| title | String | Yes | Not blank |
| genre | String | Yes | Not blank |
| description | String | Yes | Not blank |
| duration | Integer | Yes | Min 1 (minutes) |
| minimumAge | Integer | Yes | Min 0 |
| director | String | Yes | Not blank |
| actors | String | Yes | Not blank |
| posterUrl | String | No | - |
| posterCloudinaryId | String | No | - |
| trailerUrl | String | Yes | Not blank |
| status | String | Yes | SHOWING or UPCOMING |
| language | String | Yes | Not blank |

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "duration: must be positive, trailerUrl: must be a valid URL",
  "details": "uri=/movies"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Movie not found with id: {movieId}",
  "details": "uri=/movies/{movieId}"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Access Denied: Admin access required",
  "details": "uri=/movies"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Cannot delete movie with active showtimes",
  "details": "uri=/movies/{movieId}"
}
```

---

## Frontend Integration Tips

### Display Movie Grid
```javascript
// Get all showing movies
const response = await fetch('/movies?status=SHOWING');
const movies = await response.json();

// Display in grid
movies.forEach(movie => {
  const card = createMovieCard(movie);
  grid.appendChild(card);
});
```

### Search Functionality
```javascript
// Search as user types
const searchInput = document.getElementById('search');
searchInput.addEventListener('input', async (e) => {
  const query = e.target.value;
  if (query.length >= 3) {
    const response = await fetch(`/movies?title=${encodeURIComponent(query)}`);
    const results = await response.json();
    displayResults(results);
  }
});
```

### Filter by Genre
```javascript
// Genre filter dropdown
const genreSelect = document.getElementById('genre');
genreSelect.addEventListener('change', async (e) => {
  const genre = e.target.value;
  const url = genre ? `/movies?genre=${genre}` : '/movies';
  const response = await fetch(url);
  const movies = await response.json();
  displayMovies(movies);
});
```

---

## Important Notes

1. **Multiple Genres**: The `genre` field can contain comma-separated genres (e.g., "Action, Adventure, Sci-Fi")

2. **Search is Case-Insensitive**: All search operations are case-insensitive and support partial matching

3. **Poster URLs**: Should be absolute URLs (e.g., Cloudinary, S3, or other CDN)

4. **Trailer URLs**: Must be valid URLs (validated on creation/update)

5. **Duration Format**: Duration is in minutes (e.g., 120 = 2 hours)

6. **Cast Format**: Can be comma-separated list of actor names

7. **Status Management**: 
   - Admin should change status from UPCOMING to SHOWING on release date
   - Consider automated job to handle status changes

8. **Deletion Restrictions**: Cannot delete movies with:
   - Active showtimes in the future
   - Confirmed bookings
   - Historical data may be preserved for reporting
