# Promotions API Documentation

Base Path: `/promotions`

## Overview
Endpoints for managing promotional codes and discounts. Supports percentage and fixed amount discounts with usage limits and validity periods.

---

## Endpoints

### 1. Create Promotion (Admin Only)
**POST** `/promotions`

Creates a new promotional code.

#### Request Body
```json
{
  "code": "string (required, min 3 chars, max 20 chars)",
  "name": "string (required)",
  "description": "string (optional)",
  "discountType": "string (required, values: PERCENTAGE, FIXED_AMOUNT)",
  "discountValue": 10.00 (number, required, min 0.01, max depends on type),
  "startDate": "2024-11-17T00:00:00 (datetime, required)",
  "endDate": "2024-12-31T23:59:59 (datetime, required)",
  "maxUsage": 100 (integer, min 1, optional),
  "maxUsagePerUser": 1 (integer, min 1, optional),
  "isActive": true (boolean, required)
}
```

#### Request Field Constraints
| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| code | String | Yes | 3-20 characters, uppercase recommended |
| name | String | Yes | Display name |
| description | String | No | Optional description |
| discountType | String | Yes | PERCENTAGE or FIXED_AMOUNT |
| discountValue | Number | Yes | For PERCENTAGE: 0.01-100, For FIXED_AMOUNT: positive |
| startDate | DateTime | Yes | Promotion start date |
| endDate | DateTime | Yes | Promotion end date, must be after startDate |
| maxUsage | Integer | No | Total usage limit across all users |
| maxUsagePerUser | Integer | No | Per-user usage limit |
| isActive | Boolean | Yes | Active/inactive status |

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "id": "uuid",
  "code": "WINTER2024",
  "name": "Winter Sale 2024",
  "description": "Special discount for winter season",
  "discountType": "PERCENTAGE",
  "discountValue": 15.00,
  "startDate": "2024-11-17T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "maxUsage": 1000,
  "maxUsagePerUser": 2,
  "isActive": true,
  "createdAt": "2024-11-17T10:00:00",
  "updatedAt": "2024-11-17T10:00:00"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Update Promotion (Admin Only)
**PUT** `/promotions/{promotionId}`

Updates an existing promotion.

#### Path Parameters
- `promotionId`: UUID of the promotion

#### Request Body (all fields optional)
```json
{
  "name": "string",
  "description": "string",
  "discountType": "string (values: PERCENTAGE, FIXED_AMOUNT)",
  "discountValue": 20.00 (number, min 0.01),
  "startDate": "datetime",
  "endDate": "datetime",
  "maxUsage": 500 (integer, min 1),
  "maxUsagePerUser": 1 (integer, min 1),
  "isActive": false (boolean)
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: PromotionDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Code cannot be changed after creation
- Updating limits doesn't affect already-used promotions

---

### 3. Deactivate Promotion (Admin Only)
**PATCH** `/promotions/{promotionId}/deactivate`

Marks a promotion as inactive.

#### Path Parameters
- `promotionId`: UUID of the promotion

#### Response
- **Status Code**: `204 NO CONTENT`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Use Case
- Quick disable without deleting historical data

---

### 4. Delete Promotion (Admin Only)
**DELETE** `/promotions/{promotionId}`

Permanently deletes a promotion.

#### Path Parameters
- `promotionId`: UUID of the promotion

#### Response
- **Status Code**: `204 NO CONTENT`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Cannot delete promotions with active bookings
- Consider deactivating instead of deleting

---

### 5. Get Promotion by ID
**GET** `/promotions/{promotionId}`

Retrieves promotion details by ID.

#### Path Parameters
- `promotionId`: UUID of the promotion

#### Response
- **Status Code**: `200 OK`
- **Body**: PromotionDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 6. Get Promotion by Code
**GET** `/promotions/code/{code}`

Retrieves promotion details by code.

#### Path Parameters
- `code`: Promotion code (e.g., "WINTER2024")

#### Response
- **Status Code**: `200 OK`
- **Body**: PromotionDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Validate promotion code during checkout
- Display discount preview to user

---

### 7. Get All Promotions
**GET** `/promotions`

Retrieves all promotions or filters by status.

#### Query Parameters (optional)
- `filter`: string - Filter type
  - `active`: Only active promotions
  - `valid`: Only valid promotions (active + within date range)

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PromotionDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Examples
- All promotions: `GET /promotions`
- Active only: `GET /promotions?filter=active`
- Valid only: `GET /promotions?filter=valid`

---

### 8. Get Active Promotions
**GET** `/promotions/active`

Retrieves all active promotions.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PromotionDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

---

### 9. Get Valid Promotions
**GET** `/promotions/valid`

Retrieves promotions that are:
- Active (`isActive = true`)
- Within valid date range (current date between startDate and endDate)
- Not exceeded usage limits

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of PromotionDataResponse objects

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Display available promotions to users
- Promotion selection dropdown

---

## Discount Type Enum

| Type | Description | Example |
|------|-------------|---------|
| `PERCENTAGE` | Percentage discount (0.01-100%) | 15% off |
| `FIXED_AMOUNT` | Fixed amount discount in VND | 50,000 VND off |

---

## Validation Rules

### Active Promotion
- `isActive = true`

### Valid Promotion
- Active status
- Current date >= startDate
- Current date <= endDate
- Total usage < maxUsage (if set)

### Per-User Validation
- User's usage count < maxUsagePerUser (if set)

---

## Data Model

### PromotionDataResponse
```json
{
  "id": "uuid",
  "code": "string",
  "name": "string",
  "description": "string",
  "discountType": "PERCENTAGE|FIXED_AMOUNT",
  "discountValue": "number",
  "startDate": "datetime",
  "endDate": "datetime",
  "maxUsage": "integer",
  "maxUsagePerUser": "integer",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

## Frontend Integration Examples

### Apply Promotion Code
```javascript
// Checkout page: apply promotion
async function applyPromotion(code, bookingAmount) {
  try {
    // Fetch promotion details
    const response = await fetch(`/promotions/code/${code}`);
    if (!response.ok) {
      throw new Error('Invalid promotion code');
    }
    
    const promotion = await response.json();
    
    // Validate promotion
    if (!promotion.isActive) {
      throw new Error('Promotion is not active');
    }
    
    const now = new Date();
    const start = new Date(promotion.startDate);
    const end = new Date(promotion.endDate);
    
    if (now < start || now > end) {
      throw new Error('Promotion is not valid for this date');
    }
    
    // Calculate discount
    let discount = 0;
    if (promotion.discountType === 'PERCENTAGE') {
      discount = bookingAmount * (promotion.discountValue / 100);
    } else {
      discount = promotion.discountValue;
    }
    
    // Display discount preview
    displayDiscountPreview(promotion, discount, bookingAmount - discount);
    
    return {
      code: promotion.code,
      discount: discount,
      finalAmount: bookingAmount - discount
    };
    
  } catch (error) {
    displayError(error.message);
    return null;
  }
}
```

### Display Available Promotions
```javascript
// Show valid promotions on checkout page
async function loadAvailablePromotions() {
  const response = await fetch('/promotions/valid');
  const promotions = await response.json();
  
  const container = document.getElementById('promotions-list');
  container.innerHTML = '';
  
  promotions.forEach(promo => {
    const card = createPromotionCard(promo);
    container.appendChild(card);
  });
}

function createPromotionCard(promo) {
  const card = document.createElement('div');
  card.className = 'promotion-card';
  
  const discountText = promo.discountType === 'PERCENTAGE'
    ? `${promo.discountValue}% OFF`
    : `${formatCurrency(promo.discountValue)} OFF`;
  
  card.innerHTML = `
    <div class="promo-code">${promo.code}</div>
    <div class="promo-name">${promo.name}</div>
    <div class="promo-discount">${discountText}</div>
    <div class="promo-valid">Valid until ${formatDate(promo.endDate)}</div>
    <button onclick="applyCode('${promo.code}')">Apply</button>
  `;
  
  return card;
}
```

### Admin: Promotion Management
```javascript
// Create new promotion
async function createPromotion(formData) {
  const response = await fetch('/promotions', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer <token>',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      code: formData.code.toUpperCase(),
      name: formData.name,
      description: formData.description,
      discountType: formData.type,
      discountValue: parseFloat(formData.value),
      startDate: formData.startDate,
      endDate: formData.endDate,
      maxUsage: parseInt(formData.maxUsage) || null,
      maxUsagePerUser: parseInt(formData.maxUsagePerUser) || 1,
      isActive: true
    })
  });
  
  if (!response.ok) {
    throw new Error('Failed to create promotion');
  }
  
  const promotion = await response.json();
  console.log('Promotion created:', promotion);
  return promotion;
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "message": "Invalid promotion data",
  "errors": [
    "discountValue must be between 0.01 and 100 for percentage discounts",
    "endDate must be after startDate"
  ]
}
```

### 404 Not Found
```json
{
  "message": "Promotion not found with code: INVALID2024"
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
  "message": "Promotion code already exists: WINTER2024"
}
```

---

## Important Notes

1. **Code Format**: 
   - Use uppercase for consistency (e.g., "WINTER2024")
   - Avoid special characters (stick to letters and numbers)
   - Keep short but memorable

2. **Discount Value Constraints**:
   - **PERCENTAGE**: 0.01 to 100 (represents 0.01% to 100%)
   - **FIXED_AMOUNT**: Any positive value in VND

3. **Usage Tracking**:
   - System tracks total usage across all users
   - System tracks per-user usage
   - Both limits are enforced during booking

4. **Date Validation**:
   - startDate must be before endDate
   - Promotions can be created for future dates
   - Past promotions remain in system for historical data

5. **Active vs Valid**:
   - **Active**: Manually enabled/disabled by admin
   - **Valid**: Active + within date range + under usage limits
   - Users can only apply valid promotions

6. **Stacking**: 
   - Current system: One promotion per booking
   - Cannot combine multiple promotions

7. **Minimum Amount**: 
   - Consider adding minimum booking amount requirement
   - Prevent abuse on small bookings

8. **Maximum Discount**:
   - For PERCENTAGE: Consider capping maximum discount amount
   - Example: 50% off, max 200,000 VND

9. **Deactivation vs Deletion**:
   - **Deactivate**: Keeps data, prevents new usage
   - **Delete**: Permanent removal (avoid if already used)

10. **Analytics**:
    - Track promotion usage for marketing analysis
    - Monitor conversion rates per promotion
    - Calculate ROI on promotional campaigns
