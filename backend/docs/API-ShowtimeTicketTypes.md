# Showtime Ticket Types API

This API manages the assignment of ticket types to specific showtimes. Admins can configure which ticket types are available for each showtime.

## Base URL
```
/showtimes/{showtimeId}/ticket-types
```

---

## Endpoints

### 1. Get Assigned Ticket Types
Get all ticket types currently assigned to a showtime.

**Endpoint:** `GET /showtimes/{showtimeId}/ticket-types`

**Path Parameters:**
- `showtimeId` (UUID, required): The ID of the showtime

**Response:** `200 OK`
```json
{
  "showtimeId": "123e4567-e89b-12d3-a456-426614174000",
  "assignedTicketTypeIds": [
    "123e4567-e89b-12d3-a456-426614174001",
    "123e4567-e89b-12d3-a456-426614174002",
    "123e4567-e89b-12d3-a456-426614174003"
  ]
}
```

**Error Responses:**
- `404 Not Found`: Showtime not found

---

### 2. Assign Single Ticket Type
Assign a single ticket type to a showtime.

**Endpoint:** `POST /showtimes/{showtimeId}/ticket-types/{ticketTypeId}`

**Path Parameters:**
- `showtimeId` (UUID, required): The ID of the showtime
- `ticketTypeId` (UUID, required): The ID of the ticket type to assign

**Response:** `201 Created`

**Notes:**
- If the ticket type is already assigned, the operation is idempotent (no error)
- The ticket type must be active

**Error Responses:**
- `404 Not Found`: Showtime or ticket type not found

---

### 3. Assign Multiple Ticket Types
Assign multiple ticket types to a showtime at once.

**Endpoint:** `POST /showtimes/{showtimeId}/ticket-types`

**Path Parameters:**
- `showtimeId` (UUID, required): The ID of the showtime

**Request Body:**
```json
{
  "ticketTypeIds": [
    "123e4567-e89b-12d3-a456-426614174001",
    "123e4567-e89b-12d3-a456-426614174002",
    "123e4567-e89b-12d3-a456-426614174003"
  ]
}
```

**Validation:**
- `ticketTypeIds`: Cannot be empty

**Response:** `201 Created`

**Notes:**
- Adds to existing assignments (does not replace)
- Duplicate assignments are handled gracefully

**Error Responses:**
- `400 Bad Request`: Validation error (empty list)
- `404 Not Found`: Showtime or any ticket type not found

---

### 4. Replace All Ticket Types
Replace all ticket type assignments for a showtime.

**Endpoint:** `PUT /showtimes/{showtimeId}/ticket-types`

**Path Parameters:**
- `showtimeId` (UUID, required): The ID of the showtime

**Request Body:**
```json
{
  "ticketTypeIds": [
    "123e4567-e89b-12d3-a456-426614174001",
    "123e4567-e89b-12d3-a456-426614174002"
  ]
}
```

**Validation:**
- `ticketTypeIds`: Cannot be empty

**Response:** `200 OK`

**Notes:**
- Deactivates all existing assignments
- Creates new assignments for the provided ticket type IDs
- This is a complete replacement operation

**Error Responses:**
- `400 Bad Request`: Validation error (empty list)
- `404 Not Found`: Showtime or any ticket type not found

---

### 5. Remove Ticket Type
Remove a ticket type assignment from a showtime.

**Endpoint:** `DELETE /showtimes/{showtimeId}/ticket-types/{ticketTypeId}`

**Path Parameters:**
- `showtimeId` (UUID, required): The ID of the showtime
- `ticketTypeId` (UUID, required): The ID of the ticket type to remove

**Response:** `204 No Content`

**Notes:**
- Sets the assignment to inactive (soft delete)
- The ticket type itself is not deleted

**Error Responses:**
- `404 Not Found`: Assignment not found or showtime/ticket type not found

---

## Business Logic

### Ticket Type Assignment Flow
1. **Showtime Creation**: When a showtime is created, no ticket types are automatically assigned
2. **Admin Configuration**: Admin must explicitly assign ticket types using these endpoints
3. **Public API**: Users calling `GET /ticket-types?showtimeId={id}` will only see ticket types assigned to that showtime
4. **Fallback**: If no ticket types are assigned, all active ticket types are returned (with a warning logged)

### Soft Delete Pattern
- Removing a ticket type sets `active = false` in the `showtime_ticket_types` table
- The record is preserved for audit purposes
- Re-assigning the same ticket type may create a new record

### Validation Rules
- Both showtime and ticket type must exist
- Ticket types must be active to be assigned
- Cannot assign duplicate active assignments (idempotent)

---

## Integration with Other APIs

### Related to Ticket Types API
- `GET /ticket-types?showtimeId={id}`: Returns ticket types filtered by showtime assignments
- `GET /ticket-types`: Returns all active ticket types (no filtering)
- `GET /ticket-types/admin`: Returns all ticket types with full details

### Related to Showtimes API
- `POST /showtimes`: Creates showtime without ticket type assignments
- `DELETE /showtimes/{id}`: Deletes showtime (cascade behavior applies)

---

## Example Workflow

### Setting up a new showtime
```bash
# 1. Create showtime
POST /showtimes
{
  "movieId": "...",
  "roomId": "...",
  "startTime": "2025-11-25T19:00:00",
  "format": "2D"
}
# Response: showtimeId = "abc-123"

# 2. Assign ticket types
POST /showtimes/abc-123/ticket-types
{
  "ticketTypeIds": [
    "adult-id",
    "student-id",
    "senior-id"
  ]
}

# 3. Verify assignments
GET /showtimes/abc-123/ticket-types
# Response: { "showtimeId": "abc-123", "assignedTicketTypeIds": ["adult-id", "student-id", "senior-id"] }

# 4. Users can now get available ticket types
GET /ticket-types?showtimeId=abc-123
# Returns only adult, student, and senior ticket types with prices
```

### Updating ticket type availability
```bash
# Remove senior discount for a specific showtime
DELETE /showtimes/abc-123/ticket-types/senior-id

# Or replace all ticket types
PUT /showtimes/abc-123/ticket-types
{
  "ticketTypeIds": ["adult-id", "student-id"]
}
```
