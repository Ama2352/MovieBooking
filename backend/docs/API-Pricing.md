# Pricing System API Documentation

## Overview
The pricing system consists of:
- **Base Prices** (`/price-base`): Foundation ticket prices
- **Price Modifiers** (`/price-modifiers`): Dynamic adjustments based on conditions

---

# Price Base API

Base Path: `/price-base`

## Base Price Endpoints

### 1. Add Base Price (Admin Only)
**POST** `/price-base`

Creates a new base price configuration.

#### Request Body
```json
{
  "name": "string (required)",
  "basePrice": 80000.00 (number, required, positive)
}
```

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "id": "uuid",
  "name": "Standard Base Price 2024",
  "basePrice": 80000.00,
  "isActive": true,
  "createdAt": "2024-11-17T10:00:00",
  "updatedAt": "2024-11-17T10:00:00"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Update Base Price (Admin Only)
**PUT** `/price-base/{id}`

Updates base price configuration.

#### Path Parameters
- `id`: UUID of the base price

#### Request Body
```json
{
  "name": "string (optional)",
  "isActive": true (boolean, optional)
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: PriceBaseDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- `basePrice` value cannot be modified after creation (create new one instead)
- Only one base price can be active at a time

---

### 3. Delete Base Price (Admin Only)
**DELETE** `/price-base/{id}`

Deletes a base price configuration.

#### Path Parameters
- `id`: UUID of the base price

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 4. Get Base Price by ID
**GET** `/price-base/{id}`

Retrieves a base price configuration.

#### Path Parameters
- `id`: UUID of the base price

#### Response
- **Status Code**: `200 OK`
- **Body**: PriceBaseDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 5. Get All Base Prices
**GET** `/price-base`

Retrieves all base price configurations.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PriceBaseDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

---

### 6. Get Active Base Price
**GET** `/price-base/active`

Retrieves the currently active base price.

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
{
  "id": "uuid",
  "name": "Standard Base Price 2024",
  "basePrice": 80000.00,
  "isActive": true,
  "createdAt": "2024-11-17T10:00:00",
  "updatedAt": "2024-11-17T10:00:00"
}
```

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Display current ticket prices to users
- Calculate showtime seat prices

---

# Price Modifiers API

Base Path: `/price-modifiers`

## Price Modifier Endpoints

### 1. Add Price Modifier (Admin Only)
**POST** `/price-modifiers`

Creates a new price modifier rule.

#### Request Body
```json
{
  "name": "string (required)",
  "conditionType": "string (required, values: DAY_TYPE, TIME_RANGE, FORMAT, ROOM_TYPE, SEAT_TYPE)",
  "conditionValue": "string (required)",
  "modifierType": "string (required, values: ADD, MULTIPLY)",
  "modifierValue": 20000.00 (number, required)
}
```

#### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Display name for the modifier |
| conditionType | String | Yes | When this modifier applies |
| conditionValue | String | Yes | Specific condition value |
| modifierType | String | Yes | How to apply the modifier |
| modifierValue | Number | Yes | Amount to add or multiply |

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "id": "uuid",
  "name": "VIP Seat Premium",
  "conditionType": "SEAT_TYPE",
  "conditionValue": "VIP",
  "modifierType": "ADD",
  "modifierValue": 20000.00,
  "isActive": true,
  "createdAt": "2024-11-17T10:00:00",
  "updatedAt": "2024-11-17T10:00:00"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Update Price Modifier (Admin Only)
**PUT** `/price-modifiers/{id}`

Updates a price modifier's status.

#### Path Parameters
- `id`: UUID of the price modifier

#### Request Body
```json
{
  "name": "string (optional)",
  "isActive": false (boolean, optional)
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: PriceModifierDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Condition and modifier values cannot be changed after creation
- Create new modifier if values need to change

---

### 3. Delete Price Modifier (Admin Only)
**DELETE** `/price-modifiers/{id}`

Deletes a price modifier.

#### Path Parameters
- `id`: UUID of the price modifier

#### Response
- **Status Code**: `200 OK`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 4. Get Price Modifier by ID
**GET** `/price-modifiers/{id}`

Retrieves a price modifier.

#### Path Parameters
- `id`: UUID of the price modifier

#### Response
- **Status Code**: `200 OK`
- **Body**: PriceModifierDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 5. Get All Price Modifiers
**GET** `/price-modifiers`

Retrieves all price modifiers.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PriceModifierDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

---

### 6. Get Active Price Modifiers
**GET** `/price-modifiers/active`

Retrieves only active price modifiers.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PriceModifierDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

---

### 7. Get Price Modifiers by Condition Type
**GET** `/price-modifiers/by-condition`

Retrieves modifiers filtered by condition type.

#### Query Parameters
- `conditionType`: string (required) - Condition type to filter

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PriceModifierDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Example
```
GET /price-modifiers/by-condition?conditionType=SEAT_TYPE
```

---

### 8. Get Condition Types Info
**GET** `/price-modifiers/condition-types`

Retrieves information about available condition types.

#### Response
- **Status Code**: `200 OK`
- **Body**:
```json
[
  {
    "type": "DAY_TYPE",
    "description": "Apply modifier based on day of week",
    "exampleValues": ["WEEKEND", "WEEKDAY"]
  },
  {
    "type": "TIME_RANGE",
    "description": "Apply modifier based on showtime start hour",
    "exampleValues": ["MORNING", "AFTERNOON", "EVENING", "NIGHT"]
  },
  {
    "type": "FORMAT",
    "description": "Apply modifier based on movie format",
    "exampleValues": ["2D", "3D", "IMAX", "4DX"]
  },
  {
    "type": "ROOM_TYPE",
    "description": "Apply modifier based on room type",
    "exampleValues": ["STANDARD", "VIP", "IMAX", "STARIUM"]
  },
  {
    "type": "SEAT_TYPE",
    "description": "Apply modifier based on seat type",
    "exampleValues": ["NORMAL", "VIP", "COUPLE"]
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

---

## Condition Types

### DAY_TYPE
Applies based on day of the week.

**Valid Values:**
- `WEEKEND` - Saturday, Sunday
- `WEEKDAY` - Monday to Friday

**Example:**
```json
{
  "name": "Weekend Premium",
  "conditionType": "DAY_TYPE",
  "conditionValue": "WEEKEND",
  "modifierType": "MULTIPLY",
  "modifierValue": 1.2
}
```

---

### TIME_RANGE
Applies based on showtime start hour.

**Valid Values:**
- `MORNING` - 06:00 to 11:59
- `AFTERNOON` - 12:00 to 17:59
- `EVENING` - 18:00 to 21:59
- `NIGHT` - 22:00 to 05:59

**Example:**
```json
{
  "name": "Prime Time Surcharge",
  "conditionType": "TIME_RANGE",
  "conditionValue": "EVENING",
  "modifierType": "ADD",
  "modifierValue": 10000.00
}
```

---

### FORMAT
Applies based on movie format.

**Valid Values:**
- `2D` - Standard 2D
- `3D` - 3D screening
- `IMAX` - IMAX format
- `4DX` - 4D experience
- `ScreenX` - Multi-projection
- `Dolby Atmos` - Enhanced audio

**Example:**
```json
{
  "name": "3D Glasses Fee",
  "conditionType": "FORMAT",
  "conditionValue": "3D",
  "modifierType": "ADD",
  "modifierValue": 15000.00
}
```

---

### ROOM_TYPE
Applies based on screening room type.

**Valid Values:**
- `STANDARD` - Regular room
- `VIP` - Premium room
- `IMAX` - IMAX room
- `STARIUM` - Large premium screen

**Example:**
```json
{
  "name": "IMAX Premium",
  "conditionType": "ROOM_TYPE",
  "conditionValue": "IMAX",
  "modifierType": "MULTIPLY",
  "modifierValue": 1.5
}
```

---

### SEAT_TYPE
Applies based on seat type.

**Valid Values:**
- `NORMAL` - Standard seat
- `VIP` - Premium seat
- `COUPLE` - Double seat

**Example:**
```json
{
  "name": "VIP Seat Fee",
  "conditionType": "SEAT_TYPE",
  "conditionValue": "VIP",
  "modifierType": "ADD",
  "modifierValue": 20000.00
}
```

---

## Modifier Types

### ADD
Adds a fixed amount to the base price.

**Formula:** `Final Price = Base Price + Modifier Value`

**Example:**
- Base Price: 80,000 VND
- Modifier Value: 20,000 VND
- Final Price: 100,000 VND

---

### MULTIPLY
Multiplies the current price by a factor.

**Formula:** `Final Price = Current Price × Modifier Value`

**Example:**
- Base Price: 80,000 VND
- Modifier Value: 1.2 (20% increase)
- Final Price: 96,000 VND

---

## Price Calculation Flow

### Step-by-Step Calculation
```
1. Start with Base Price: 80,000 VND

2. Apply SEAT_TYPE modifier (ADD):
   80,000 + 20,000 = 100,000 VND

3. Apply FORMAT modifier (ADD):
   100,000 + 15,000 = 115,000 VND

4. Apply TIME_RANGE modifier (ADD):
   115,000 + 10,000 = 125,000 VND

5. Apply DAY_TYPE modifier (MULTIPLY):
   125,000 × 1.2 = 150,000 VND

Final Ticket Price: 150,000 VND
```

### Calculation Order
1. All **ADD** modifiers applied first (in any order)
2. All **MULTIPLY** modifiers applied second (in sequence)

---

## Frontend Integration Examples

### Display Ticket Price with Breakdown
```javascript
// Fetch active base price and modifiers
async function calculateTicketPrice(seat, showtime) {
  // Get base price
  const basePriceResponse = await fetch('/price-base/active');
  const basePrice = await basePriceResponse.json();
  
  // Get active modifiers
  const modifiersResponse = await fetch('/price-modifiers/active');
  const allModifiers = await modifiersResponse.json();
  
  // Filter applicable modifiers
  const applicableModifiers = allModifiers.filter(mod => {
    if (mod.conditionType === 'SEAT_TYPE' && mod.conditionValue === seat.type) return true;
    if (mod.conditionType === 'FORMAT' && mod.conditionValue === showtime.format) return true;
    if (mod.conditionType === 'TIME_RANGE' && matchesTimeRange(showtime.startTime, mod.conditionValue)) return true;
    if (mod.conditionType === 'DAY_TYPE' && matchesDayType(showtime.startTime, mod.conditionValue)) return true;
    return false;
  });
  
  // Calculate price
  let currentPrice = basePrice.basePrice;
  const breakdown = {
    basePrice: currentPrice,
    modifiers: []
  };
  
  // Apply ADD modifiers
  applicableModifiers
    .filter(m => m.modifierType === 'ADD')
    .forEach(mod => {
      currentPrice += mod.modifierValue;
      breakdown.modifiers.push({
        name: mod.name,
        type: 'ADD',
        value: mod.modifierValue
      });
    });
  
  // Apply MULTIPLY modifiers
  applicableModifiers
    .filter(m => m.modifierType === 'MULTIPLY')
    .forEach(mod => {
      const addedValue = currentPrice * (mod.modifierValue - 1);
      currentPrice *= mod.modifierValue;
      breakdown.modifiers.push({
        name: mod.name,
        type: 'MULTIPLY',
        value: addedValue
      });
    });
  
  breakdown.finalPrice = currentPrice;
  return breakdown;
}
```

### Admin: Create Pricing Rules
```javascript
// Create weekend surcharge
async function createWeekendSurcharge() {
  await fetch('/price-modifiers', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer <token>',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name: 'Weekend Surcharge',
      conditionType: 'DAY_TYPE',
      conditionValue: 'WEEKEND',
      modifierType: 'MULTIPLY',
      modifierValue: 1.2 // 20% increase
    })
  });
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "message": "Invalid modifier data",
  "errors": [
    "modifierValue must be positive",
    "Invalid conditionType"
  ]
}
```

### 404 Not Found
```json
{
  "message": "Price base/modifier not found"
}
```

### 403 Forbidden
```json
{
  "message": "Admin access required"
}
```

---

## Important Notes

1. **Single Active Base Price**: Only one base price can be active at a time

2. **Multiple Modifiers**: Multiple modifiers can be active simultaneously and stack

3. **Modifier Order**: ADD modifiers applied before MULTIPLY for consistent results

4. **Immutable Values**: Base price and modifier values cannot be changed after creation

5. **Historical Data**: Keep inactive prices/modifiers for historical booking records

6. **Recalculation**: After updating modifiers, use recalculate endpoint for existing showtimes

7. **Testing**: Test price calculations thoroughly with various combinations

8. **Display**: Always show price breakdown to users for transparency

9. **Rounding**: Final prices should be rounded appropriately (nearest 1,000 VND)

10. **Performance**: Cache active modifiers on frontend, refresh periodically
