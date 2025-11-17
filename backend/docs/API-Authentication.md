# Authentication API Documentation

Base Path: `/auth`

## Overview
Authentication endpoints for user registration, login, logout, token refresh, and guest user creation.

---

## Endpoints

### 1. Register User
**POST** `/auth/register`

Creates a new user account.

#### Request Body
```json
{
  "username": "string (required)",
  "email": "string (required, valid email format)",
  "fullName": "string (required)",
  "phoneNumber": "string (required, valid format)",
  "password": "string (required, min 8 chars)"
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**: Empty

#### Authentication
- **Required**: No (Public endpoint)

---

### 2. Login
**POST** `/auth/login`

Authenticates a user and returns access/refresh tokens via HTTP-only cookies.

#### Request Body
```json
{
  "email": "string (required, not blank, valid email)",
  "password": "string (required, not blank)"
}
```

#### Response
- **Status Code**: `200 OK`
- **Headers**: 
  - `Set-Cookie: accessToken=...` (HTTP-only cookie)
  - `Set-Cookie: refreshToken=...` (HTTP-only cookie)
- **Body**: Empty

#### Authentication
- **Required**: No (Public endpoint)

---

### 3. Logout
**POST** `/auth/logout`

Logs out the current user by invalidating their refresh token.

#### Request
- Requires `refreshToken` cookie

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)

---

### 4. Logout All Sessions
**POST** `/auth/logout-all`

Logs out all sessions for a specific user.

#### Query Parameters
- `email` (required): User's email address

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)

---

### 5. Refresh Access Token
**GET** `/auth/refresh`

Generates a new access token using the refresh token.

#### Request
- Requires `refreshToken` cookie

#### Response
- **Status Code**: `200 OK`
- **Headers**: 
  - `Set-Cookie: accessToken=...` (new access token)
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)

---

### 6. Register Guest User
**POST** `/auth/guest/register`

Creates a temporary guest account for booking without full registration.

#### Request Body
```json
{
  "email": "string (required, valid email)",
  "fullName": "string (required)",
  "phoneNumber": "string (required)"
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "userId": "uuid-string"
}
```

#### Authentication
- **Required**: No (Public endpoint)

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "message": "Invalid input data",
  "errors": ["field validation error messages"]
}
```

### 401 Unauthorized
```json
{
  "message": "Invalid credentials or token expired"
}
```

### 409 Conflict
```json
{
  "message": "Email already exists"
}
```

### 500 Internal Server Error
```json
{
  "message": "Internal server error"
}
```

---

## Notes

1. **Token Management**: Access and refresh tokens are stored in HTTP-only cookies for security.
2. **Guest Users**: Guest accounts have limited functionality and can be upgraded to full accounts later.
3. **Session Management**: Users can have multiple active sessions. Use logout-all to terminate all sessions.
4. **Token Expiration**: Access tokens expire quickly (typically 15-30 minutes), while refresh tokens last longer (7-30 days).
