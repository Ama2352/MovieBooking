# Guide: How to Add Price Modifiers via Backend API

This guide explains how the frontend team can add new price modifiers to the backend system using the available API endpoints. Price modifiers allow you to adjust ticket prices based on seat, showtime, format, room, or other conditions.

---

## 1. What is a Price Modifier?
A price modifier is a rule that changes the base ticket price for a seat. It can be a percentage or a fixed amount, and is applied based on conditions such as:
- Day type (WEEKEND, WEEKDAY)
- Time range (MORNING, AFTERNOON, EVENING, NIGHT)
- Format (2D, 3D, IMAX, 4DX)
- Room type (STANDARD, VIP, IMAX, STARIUM)
- Seat type (NORMAL, VIP, COUPLE)

---

## 2. API Endpoint to Add a Price Modifier

**Endpoint:**
```
POST /price-modifiers
```

**Authentication:**
- Requires admin privileges (Bearer token)

---

## 3. Request Body Format
Send a JSON object matching the `AddPriceModifierRequest` DTO:

```json
{
  "name": "VIP Room Premium",
  "conditionType": "ROOM_TYPE",
  "conditionValue": "VIP",
  "modifierType": "FIXED_AMOUNT", // or "PERCENTAGE"
  "modifierValue": 30000,
  "isActive": true
}
```

**Field Descriptions:**
- `name`: Human-readable name for the modifier (e.g., "Weekend Surcharge")
- `conditionType`: The type of condition (see below for allowed values)
- `conditionValue`: The value for the condition (e.g., "VIP", "WEEKEND", "3D")
- `modifierType`: Either `PERCENTAGE` or `FIXED_AMOUNT`
- `modifierValue`: The value to apply (e.g., 20 for 20%, 10000 for 10,000 VND)
- `isActive`: Whether this modifier is currently active

---

## 4. Allowed Condition Types & Values

You can get the allowed condition types and example values via:
```
GET /price-modifiers/condition-types
```

**Example Response:**
```json
[
  {
    "type": "DAY_TYPE",
    "description": "Apply modifier based on day of week",
    "examples": ["WEEKEND", "WEEKDAY"]
  },
  {
    "type": "TIME_RANGE",
    "description": "Apply modifier based on showtime start hour",
    "examples": ["MORNING", "AFTERNOON", "EVENING", "NIGHT"]
  },
  {
    "type": "FORMAT",
    "description": "Apply modifier based on movie format",
    "examples": ["2D", "3D", "IMAX", "4DX"]
  },
  {
    "type": "ROOM_TYPE",
    "description": "Apply modifier based on room type",
    "examples": ["STANDARD", "VIP", "IMAX", "STARIUM"]
  },
  {
    "type": "SEAT_TYPE",
    "description": "Apply modifier based on seat type",
    "examples": ["NORMAL", "VIP", "COUPLE"]
  }
]
```

---

## 5. Example: Add a Weekend Surcharge Modifier

```json
{
  "name": "Weekend Surcharge",
  "conditionType": "DAY_TYPE",
  "conditionValue": "WEEKEND",
  "modifierType": "PERCENTAGE",
  "modifierValue": 20,
  "isActive": true
}
```

---

## 6. How to Test
- Use Postman or your frontend code to send a POST request to `/price-modifiers` with the above JSON.
- Ensure you include a valid admin bearer token in the Authorization header.
- On success, you will receive the created modifier object in the response.

---

## 7. Additional Operations
- **Update Modifier:** `PUT /price-modifiers/{id}`
- **Delete Modifier:** `DELETE /price-modifiers/{id}`
- **Get All Modifiers:** `GET /price-modifiers`
- **Get Active Modifiers:** `GET /price-modifiers/active`
- **Get Modifiers by Condition:** `GET /price-modifiers/by-condition?conditionType=ROOM_TYPE`

---

## Important Note: Price Base Selection
- **At any time, only one price base is used for price calculation.**
- The price base is the default starting value for all ticket prices in the system.
- The backend will automatically select the latest created and active price base as the default for all price calculations.
- If you add or update a price base, ensure it is marked as active to be used by the system.

---

## 8. Notes
- Modifiers are applied on top of the base price, in the order defined by backend logic.
- You can combine multiple modifiers for a single seat (e.g., VIP seat in VIP room on weekend).
- Only active modifiers are used in price calculation.

---

## 9. Reference: Sample Initialization
See `PricingDataInitializer.java` for examples of how modifiers are created in code. The same logic applies to API requests.

---

**If you have questions about allowed values or want to preview the effect, use the `/price-modifiers/condition-types` and `/price-modifiers` GET endpoints.**
