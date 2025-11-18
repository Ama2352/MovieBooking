# User Management API

## Base URL
`/api/users`

All endpoints require authentication unless specified otherwise.

---

## User Endpoints

### 1. Get Current User Profile

**Endpoint:** `GET /api/users/profile`

**Description:** Retrieve the authenticated user's profile information including loyalty points and membership tier.

**Authentication:** Required (Bearer Token)

**Authorization:** USER, ADMIN

**Request Body:** None

**Success Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "john_doe",
  "phoneNumber": "0912345678",
  "avatarUrl": "https://example.com/avatar.jpg",
  "avatarCloudinaryId": "user_avatars/abc123def456",
  "loyaltyPoints": 5000,
  "membershipTier": {
    "membershipTierId": "660e8400-e29b-41d4-a716-446655440000",
    "name": "Gold",
    "minPoints": 3000,
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "description": "Gold tier members get 15% off",
    "isActive": true,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "createdAt": "2024-06-15T10:30:00",
  "updatedAt": "2025-11-17T14:20:00"
}
```

**Error Responses:**
- **401 Unauthorized:** Invalid or missing authentication token
- **404 Not Found:** User not found

---

### 2. Update User Profile

**Endpoint:** `PUT /api/users/profile`

**Description:** Update the authenticated user's profile information (username, phone number, avatar).

**Authentication:** Required (Bearer Token)

**Authorization:** USER, ADMIN

**Request Body:**
```json
{
  "username": "john_updated",
  "phoneNumber": "0987654321",
  "avatarUrl": "https://example.com/new-avatar.jpg"
}
```

**Field Validations:**
- `username` (optional): 3-50 characters
- `phoneNumber` (optional): Must match Vietnamese phone format `^(03|05|07|08|09)[0-9]{8}$`
- `avatarUrl` (optional): Valid URL string

**Note:** The request does not include `avatarCloudinaryId`. The backend stores the avatar URL, and the `avatarCloudinaryId` field in User entity is available but not currently updated through this endpoint.

**Success Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "john_updated",
  "phoneNumber": "0987654321",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "avatarCloudinaryId": "user_avatars/xyz789abc012",
  "loyaltyPoints": 5000,
  "membershipTier": {
    "membershipTierId": "660e8400-e29b-41d4-a716-446655440000",
    "name": "Gold",
    "minPoints": 3000,
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "description": "Gold tier members get 15% off",
    "isActive": true,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "createdAt": "2024-06-15T10:30:00",
  "updatedAt": "2025-11-17T15:00:00"
}
```

**Error Responses:**
- **400 Bad Request:** Invalid phone number format or validation error
- **401 Unauthorized:** Token missing or invalid
- **404 Not Found:** User not found

**Notes:**
- All fields are optional; only provided fields will be updated
- Email cannot be changed through this endpoint
- `avatarCloudinaryId` is returned in response for frontend to use with Cloudinary integration
- Frontend should upload avatar to Cloudinary and provide the URL in `avatarUrl` field

---

### 3. Update Password

**Endpoint:** `PATCH /api/users/password`

**Description:** Change the authenticated user's password.

**Authentication:** Required (Bearer Token)

**Authorization:** USER, ADMIN

**Request Body:**
```json
{
  "currentPassword": "currentPassword123",
  "newPassword": "newPassword456",
  "confirmPassword": "newPassword456"
}
```

**Field Validations:**
- `currentPassword` (required): Not blank
- `newPassword` (required): Not blank
- `confirmPassword` (required): Must match newPassword

**Success Response (200 OK):**
```json
{
  "message": "Password updated successfully"
}
```

**Error Responses:**
- **400 Bad Request:** 
  - Current password is incorrect
  - New passwords do not match
  - Validation errors (password too short, etc.)
- **401 Unauthorized:** Token missing or invalid
- **404 Not Found:** User not found

**Notes:**
- Old password must be verified before update
- Password is encrypted using BCrypt
- All active sessions remain valid after password change

---

### 4. Get User Loyalty Points

**Endpoint:** `GET /api/users/loyalty`

**Description:** Retrieve the authenticated user's current loyalty points and membership tier details.

**Authentication:** Required (Bearer Token)

**Authorization:** USER, ADMIN

**Request Body:** None

**Success Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "john_doe",
  "phoneNumber": "0912345678",
  "avatarUrl": "https://example.com/avatar.jpg",
  "avatarCloudinaryId": "user_avatars/abc123def456",
  "loyaltyPoints": 5000,
  "membershipTier": {
    "membershipTierId": "660e8400-e29b-41d4-a716-446655440000",
    "name": "Gold",
    "minPoints": 3000,
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "description": "Gold tier members get 15% off",
    "isActive": true,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "createdAt": "2024-06-15T10:30:00",
  "updatedAt": "2025-11-17T14:20:00"
}
```

**Error Responses:**
- **401 Unauthorized:** Token missing or invalid
- **404 Not Found:** User not found

**Notes:**
- Loyalty points are earned automatically when completing bookings
- 1 point = 1,000 VND spent
- Membership tier is automatically upgraded when point threshold is reached

---

## Admin Endpoints

### 5. Get User by ID (Admin)

**Endpoint:** `GET /api/users/{userId}`

**Description:** Retrieve detailed information about a specific user (Admin only).

**Authentication:** Required (Bearer Token)

**Authorization:** ADMIN only

**Path Parameters:**
- `userId` (UUID, required): The ID of the user to retrieve

**Request Body:** None

**Success Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "john_doe",
  "phoneNumber": "0912345678",
  "avatarUrl": "https://example.com/avatar.jpg",
  "avatarCloudinaryId": "user_avatars/abc123def456",
  "loyaltyPoints": 5000,
  "membershipTier": {
    "membershipTierId": "660e8400-e29b-41d4-a716-446655440000",
    "name": "Gold",
    "minPoints": 3000,
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "description": "Gold tier members get 15% off",
    "isActive": true,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "createdAt": "2024-06-15T10:30:00",
  "updatedAt": "2025-11-17T14:20:00"
}
```

**Error Responses:**
- **400 Bad Request:** Invalid UUID format
- **401 Unauthorized:** Token missing or invalid
- **403 Forbidden:** User does not have ADMIN role
- **404 Not Found:** User not found

---

### 6. Get All Users (Admin)

**Endpoint:** `GET /api/users`

**Description:** Retrieve a list of all users in the system (Admin only).

**Authentication:** Required (Bearer Token)

**Authorization:** ADMIN only

**Request Body:** None

**Success Response (200 OK):**
```json
[
  {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user1@example.com",
    "username": "john_doe",
    "phoneNumber": "0912345678",
    "avatarUrl": "https://example.com/avatar1.jpg",
    "avatarCloudinaryId": "user_avatars/abc123def456",
    "loyaltyPoints": 5000,
    "membershipTier": {
      "membershipTierId": "660e8400-e29b-41d4-a716-446655440000",
      "name": "Gold",
      "minPoints": 3000,
      "discountType": "PERCENTAGE",
      "discountValue": 15.00,
      "description": "Gold tier members get 15% off",
      "isActive": true,
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    },
    "createdAt": "2024-06-15T10:30:00",
    "updatedAt": "2025-11-17T14:20:00"
  },
  {
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "email": "user2@example.com",
    "username": "jane_smith",
    "phoneNumber": "0987654321",
    "avatarUrl": null,
    "avatarCloudinaryId": null,
    "loyaltyPoints": 1500,
    "membershipTier": {
      "membershipTierId": "770e8400-e29b-41d4-a716-446655440002",
      "name": "Silver",
      "minPoints": 1000,
      "discountType": "PERCENTAGE",
      "discountValue": 5.00,
      "description": "Silver tier members get 5% off",
      "isActive": true,
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    },
    "createdAt": "2024-08-20T09:15:00",
    "updatedAt": "2025-11-16T11:45:00"
  }
]
```

**Error Responses:**
- **401 Unauthorized:** Token missing or invalid
- **403 Forbidden:** User does not have ADMIN role

**Notes:**
- Returns all users regardless of role (USER, ADMIN, GUEST)
- Consider implementing pagination for large datasets

---

### 7. Update User Role (Admin)

**Endpoint:** `PATCH /api/users/{userId}/role`

**Description:** Update a user's role in the system (Admin only).

**Authentication:** Required (Bearer Token)

**Authorization:** ADMIN only

**Path Parameters:**
- `userId` (UUID, required): The ID of the user to update

**Request Body:**
```json
{
  "role": "ADMIN"
}
```

**Valid Roles:**
- `USER` - Regular user with booking privileges
- `ADMIN` - Administrator with full system access
- `GUEST` - Temporary user for guest checkout

**Success Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "john_doe",
  "phoneNumber": "0912345678",
  "avatarUrl": "https://example.com/avatar.jpg",
  "avatarCloudinaryId": "user_avatars/abc123def456",
  "loyaltyPoints": 5000,
  "membershipTier": {
    "membershipTierId": "660e8400-e29b-41d4-a716-446655440000",
    "name": "Gold",
    "minPoints": 3000,
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "description": "Gold tier members get 15% off",
    "isActive": true,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "createdAt": "2024-06-15T10:30:00",
  "updatedAt": "2025-11-17T15:30:00"
}
```

**Error Responses:**
- **400 Bad Request:** Invalid role value or UUID format
- **401 Unauthorized:** Token missing or invalid
- **403 Forbidden:** User does not have ADMIN role
- **404 Not Found:** User not found

**Notes:**
- Use with caution - changing roles affects system permissions
- Consider implementing additional validation (e.g., preventing removal of last admin)

---

### 8. Delete User (Admin)

**Endpoint:** `DELETE /api/users/{userId}`

**Description:** Permanently delete a user from the system (Admin only). Performs strict dependency checks before deletion.

**Authentication:** Required (Bearer Token)

**Authorization:** ADMIN only

**Path Parameters:**
- `userId` (UUID, required): The ID of the user to delete

**Request Body:** None

**Success Response (200 OK):**
```json
{
  "message": "User deleted successfully"
}
```

**Error Responses:**
- **400 Bad Request:** Invalid UUID format
- **401 Unauthorized:** Token missing or invalid
- **403 Forbidden:** User does not have ADMIN role
- **404 Not Found:** User not found
- **409 Conflict:** User has dependencies that prevent deletion
  ```json
  {
    "timestamp": "2025-11-17T10:30:00",
    "status": 409,
    "error": "Conflict",
    "message": "Cannot delete user with existing bookings. Found 5 booking(s)."
  }
  ```
  ```json
  {
    "timestamp": "2025-11-17T10:30:00",
    "status": 409,
    "error": "Conflict",
    "message": "Cannot delete user with active sessions. Found 2 refresh token(s). Please revoke all sessions first."
  }
  ```

**Dependency Checks:**
1. **Bookings:** User must not have any bookings (past or present)
2. **Refresh Tokens:** User must not have any active sessions

**Notes:**
- This action is permanent and cannot be undone
- All dependencies must be cleared before deletion
- Consider implementing soft deletion for users with historical data
- Users with bookings should be deactivated rather than deleted
- Refresh tokens should be revoked before attempting deletion

---

## Loyalty Program Details

### How It Works
- Users earn **1 loyalty point per 1,000 VND** spent on bookings
- Points are awarded when payment is confirmed
- Points are revoked if booking is refunded
- Membership tier is automatically upgraded when point thresholds are reached

### Membership Tiers
The system supports multiple membership tiers (configurable by admin):
- **BRONZE** (0-999 points): Base tier, no discount
- **SILVER** (1000-2999 points): 5% discount
- **GOLD** (3000-9999 points): 15% discount
- **PLATINUM** (10000+ points): 25% discount

### Benefits
- Automatic discounts applied during checkout
- Higher tiers receive priority customer support (future feature)
- Special promotions for high-tier members (future feature)

---

## Security Notes

1. **Authentication:** All endpoints require a valid JWT Bearer token in the Authorization header
2. **Authorization:** Admin endpoints check for ADMIN role via `@PreAuthorize("hasRole('ADMIN')")`
3. **Password Security:** Passwords are hashed using BCrypt before storage
4. **Token Security:** Refresh tokens are stored in database and can be revoked
5. **Data Validation:** All inputs are validated using Bean Validation annotations
6. **Phone Number Format:** Vietnamese phone numbers only (03/05/07/08/09 + 8 digits)

---

## Error Response Format

All error responses follow this format:
```json
{
  "timestamp": "2025-11-18T12:34:56.789+00:00",
  "message": "Detailed error message",
  "details": "uri=/api/users/profile"
}
```

Common HTTP Status Codes:
- **200 OK:** Request successful
- **400 Bad Request:** Invalid input or validation error
- **401 Unauthorized:** Missing or invalid authentication token
- **403 Forbidden:** Insufficient permissions
- **404 Not Found:** Resource not found
- **409 Conflict:** Resource conflict (e.g., dependency violation)
- **500 Internal Server Error:** Server error
