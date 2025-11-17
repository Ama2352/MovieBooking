# Membership Tiers API Documentation

Base Path: `/membership-tiers`

## Overview
Endpoints for managing membership tiers and loyalty programs. Each tier offers different discount benefits for registered users.

---

## Endpoints

### 1. Create Membership Tier (Admin Only)
**POST** `/membership-tiers`

Creates a new membership tier.

#### Request Body
```json
{
  "name": "string (required)",
  "pointsRequired": 1000 (integer, required, min 0),
  "discountType": "string (required, values: PERCENTAGE, FIXED_AMOUNT)",
  "discountValue": 10.00 (number, required, min 0.01),
  "description": "string (optional)",
  "isActive": true (boolean, required)
}
```

#### Request Fields
| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| name | String | Yes | Tier name (e.g., "Bronze", "Silver", "Gold") |
| pointsRequired | Integer | Yes | Minimum points to achieve this tier (≥0) |
| discountType | String | Yes | PERCENTAGE or FIXED_AMOUNT |
| discountValue | Number | Yes | Discount amount (positive) |
| description | String | No | Optional tier description/benefits |
| isActive | Boolean | Yes | Active status |

#### Response
- **Status Code**: `201 CREATED`
- **Body**:
```json
{
  "id": "uuid",
  "name": "Gold",
  "pointsRequired": 5000,
  "discountType": "PERCENTAGE",
  "discountValue": 15.00,
  "description": "Premium tier with 15% discount on all bookings",
  "isActive": true,
  "createdAt": "2024-11-17T10:00:00",
  "updatedAt": "2024-11-17T10:00:00"
}
```

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 2. Update Membership Tier (Admin Only)
**PUT** `/membership-tiers/{tierId}`

Updates an existing membership tier.

#### Path Parameters
- `tierId`: UUID of the membership tier

#### Request Body (all fields optional)
```json
{
  "name": "string",
  "pointsRequired": 1500 (integer, min 0),
  "discountType": "string (values: PERCENTAGE, FIXED_AMOUNT)",
  "discountValue": 12.00 (number, min 0.01),
  "description": "string",
  "isActive": false (boolean)
}
```

#### Response
- **Status Code**: `200 OK`
- **Body**: MembershipTierDataResponse object

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

---

### 3. Deactivate Membership Tier (Admin Only)
**PATCH** `/membership-tiers/{tierId}/deactivate`

Marks a membership tier as inactive.

#### Path Parameters
- `tierId`: UUID of the membership tier

#### Response
- **Status Code**: `204 NO CONTENT`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Use Case
- Temporarily disable a tier without deleting
- Preserve historical data for existing members

---

### 4. Delete Membership Tier (Admin Only)
**DELETE** `/membership-tiers/{tierId}`

Permanently deletes a membership tier.

#### Path Parameters
- `tierId`: UUID of the membership tier

#### Response
- **Status Code**: `204 NO CONTENT`
- **Body**: Empty

#### Authentication
- **Required**: Yes (Bearer Token)
- **Authorization**: Admin only (`ROLE_ADMIN`)

#### Notes
- Cannot delete tier with active members
- Consider deactivating instead of deleting

---

### 5. Get Membership Tier by ID
**GET** `/membership-tiers/{tierId}`

Retrieves membership tier details by ID.

#### Path Parameters
- `tierId`: UUID of the membership tier

#### Response
- **Status Code**: `200 OK`
- **Body**: MembershipTierDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 6. Get Membership Tier by Name
**GET** `/membership-tiers/name/{name}`

Retrieves membership tier details by name.

#### Path Parameters
- `name`: Tier name (e.g., "Gold")

#### Response
- **Status Code**: `200 OK`
- **Body**: MembershipTierDataResponse object

#### Authentication
- **Required**: No (Public endpoint)

---

### 7. Get All Membership Tiers
**GET** `/membership-tiers`

Retrieves all membership tiers.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of MembershipTierDataResponse objects
```json
[
  {
    "id": "uuid",
    "name": "Bronze",
    "pointsRequired": 0,
    "discountType": "PERCENTAGE",
    "discountValue": 5.00,
    "description": "Starter tier with 5% discount",
    "isActive": true,
    "createdAt": "2024-11-17T10:00:00",
    "updatedAt": "2024-11-17T10:00:00"
  },
  {
    "id": "uuid",
    "name": "Silver",
    "pointsRequired": 1000,
    "discountType": "PERCENTAGE",
    "discountValue": 10.00,
    "description": "Mid-tier with 10% discount",
    "isActive": true,
    "createdAt": "2024-11-17T10:00:00",
    "updatedAt": "2024-11-17T10:00:00"
  },
  {
    "id": "uuid",
    "name": "Gold",
    "pointsRequired": 5000,
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "description": "Premium tier with 15% discount",
    "isActive": true,
    "createdAt": "2024-11-17T10:00:00",
    "updatedAt": "2024-11-17T10:00:00"
  },
  {
    "id": "uuid",
    "name": "Platinum",
    "pointsRequired": 10000,
    "discountType": "PERCENTAGE",
    "discountValue": 20.00,
    "description": "Elite tier with 20% discount",
    "isActive": true,
    "createdAt": "2024-11-17T10:00:00",
    "updatedAt": "2024-11-17T10:00:00"
  }
]
```

#### Authentication
- **Required**: No (Public endpoint)

---

### 8. Get Active Membership Tiers
**GET** `/membership-tiers/active`

Retrieves only active membership tiers.

#### Response
- **Status Code**: `200 OK`
- **Body**: Array of MembershipTierDataResponse objects (filtered for isActive=true)

#### Authentication
- **Required**: No (Public endpoint)

#### Use Case
- Display available tiers to users
- Tier comparison page

---

## Data Model

### MembershipTierDataResponse
```json
{
  "id": "uuid",
  "name": "string",
  "pointsRequired": "integer",
  "discountType": "PERCENTAGE|FIXED_AMOUNT",
  "discountValue": "number",
  "description": "string",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

## Discount Type Enum

| Type | Description | Example |
|------|-------------|---------|
| `PERCENTAGE` | Percentage discount (0.01-100%) | 15% off all bookings |
| `FIXED_AMOUNT` | Fixed amount discount in VND | 50,000 VND off per booking |

---

## Common Tier Structures

### Standard 4-Tier System
1. **Bronze** (0 points): 5% discount
2. **Silver** (1,000 points): 10% discount
3. **Gold** (5,000 points): 15% discount
4. **Platinum** (10,000 points): 20% discount

### Luxury 5-Tier System
1. **Member** (0 points): 3% discount
2. **Bronze** (500 points): 5% discount
3. **Silver** (2,000 points): 10% discount
4. **Gold** (5,000 points): 15% discount
5. **Diamond** (15,000 points): 25% discount

---

## Points System Integration

### Earning Points
Points are typically earned based on:
- **Booking Amount**: 1 point per 1,000 VND spent
- **Frequency**: Bonus points for multiple bookings
- **Special Events**: Double points promotions
- **Referrals**: Bonus points for referring friends

### Tier Progression
- User automatically promoted when points threshold reached
- Tier benefits apply immediately
- Users cannot drop tiers (one-way progression)

### Example Calculation
```
Booking Amount: 200,000 VND
Points Earned: 200 points (200,000 / 1,000)

Current Points: 800 → New Points: 1,000
Tier: Bronze (0-999) → Silver (1,000+)
```

---

## Frontend Integration Examples

### Display Membership Tiers
```javascript
// Show tier comparison page
async function loadMembershipTiers() {
  const response = await fetch('/membership-tiers/active');
  const tiers = await response.json();
  
  // Sort by points required
  tiers.sort((a, b) => a.pointsRequired - b.pointsRequired);
  
  const container = document.getElementById('tiers-grid');
  container.innerHTML = '';
  
  tiers.forEach(tier => {
    const card = createTierCard(tier);
    container.appendChild(card);
  });
}

function createTierCard(tier) {
  const card = document.createElement('div');
  card.className = 'tier-card';
  
  const discountText = tier.discountType === 'PERCENTAGE'
    ? `${tier.discountValue}% OFF`
    : `${formatCurrency(tier.discountValue)} OFF`;
  
  card.innerHTML = `
    <div class="tier-badge ${tier.name.toLowerCase()}">
      ${tier.name}
    </div>
    <div class="tier-discount">${discountText}</div>
    <div class="tier-points">
      ${tier.pointsRequired === 0 ? 'Starting Tier' : `${tier.pointsRequired}+ points`}
    </div>
    <div class="tier-description">${tier.description || ''}</div>
  `;
  
  return card;
}
```

### Show User's Current Tier
```javascript
// Display user's tier and progress
function displayUserTier(userPoints, tiers) {
  // Find current tier
  const sortedTiers = tiers.sort((a, b) => b.pointsRequired - a.pointsRequired);
  const currentTier = sortedTiers.find(t => userPoints >= t.pointsRequired);
  
  // Find next tier
  const nextTier = tiers.find(t => t.pointsRequired > userPoints);
  
  // Calculate progress
  let progress = 100;
  let pointsNeeded = 0;
  
  if (nextTier) {
    const currentThreshold = currentTier?.pointsRequired || 0;
    const nextThreshold = nextTier.pointsRequired;
    const range = nextThreshold - currentThreshold;
    const earned = userPoints - currentThreshold;
    progress = (earned / range) * 100;
    pointsNeeded = nextThreshold - userPoints;
  }
  
  return {
    current: currentTier,
    next: nextTier,
    progress: progress,
    pointsToNext: pointsNeeded
  };
}

// Usage
const userTierInfo = displayUserTier(userPoints, tiers);
console.log(`Current Tier: ${userTierInfo.current.name}`);
console.log(`Progress to ${userTierInfo.next?.name}: ${userTierInfo.progress.toFixed(1)}%`);
console.log(`Points needed: ${userTierInfo.pointsToNext}`);
```

### Apply Tier Discount at Checkout
```javascript
// Calculate tier discount
function applyTierDiscount(bookingAmount, userTier) {
  if (!userTier) return bookingAmount;
  
  let discount = 0;
  if (userTier.discountType === 'PERCENTAGE') {
    discount = bookingAmount * (userTier.discountValue / 100);
  } else {
    discount = userTier.discountValue;
  }
  
  return {
    originalAmount: bookingAmount,
    discount: discount,
    finalAmount: bookingAmount - discount,
    tierName: userTier.name
  };
}

// Usage at checkout
const tierDiscount = applyTierDiscount(200000, currentUserTier);
console.log(`Original: ${formatCurrency(tierDiscount.originalAmount)}`);
console.log(`${tierDiscount.tierName} Discount: -${formatCurrency(tierDiscount.discount)}`);
console.log(`Final: ${formatCurrency(tierDiscount.finalAmount)}`);
```

### Admin: Create Tier System
```javascript
// Create standard 4-tier system
async function setupStandardTiers() {
  const tiers = [
    {
      name: 'Bronze',
      pointsRequired: 0,
      discountType: 'PERCENTAGE',
      discountValue: 5.00,
      description: 'Starter tier for all new members',
      isActive: true
    },
    {
      name: 'Silver',
      pointsRequired: 1000,
      discountType: 'PERCENTAGE',
      discountValue: 10.00,
      description: 'Enjoy 10% off on all bookings',
      isActive: true
    },
    {
      name: 'Gold',
      pointsRequired: 5000,
      discountType: 'PERCENTAGE',
      discountValue: 15.00,
      description: 'Premium benefits with 15% discount',
      isActive: true
    },
    {
      name: 'Platinum',
      pointsRequired: 10000,
      discountType: 'PERCENTAGE',
      discountValue: 20.00,
      description: 'Elite status with maximum benefits',
      isActive: true
    }
  ];
  
  for (const tier of tiers) {
    await fetch('/membership-tiers', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer <token>',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(tier)
    });
  }
  
  console.log('Tier system created successfully');
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "message": "Invalid tier data",
  "errors": [
    "pointsRequired must be non-negative",
    "discountValue must be positive"
  ]
}
```

### 404 Not Found
```json
{
  "message": "Membership tier not found with name: Diamond"
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
  "message": "Membership tier with name 'Gold' already exists"
}
```

---

## Important Notes

1. **Entry Tier**: Always create a tier with `pointsRequired: 0` as the default starting tier

2. **Tier Naming**: Use consistent naming conventions (Bronze, Silver, Gold, Platinum) or (Member, VIP, Premium, Elite)

3. **Points Calculation**: Standardize point earning rules across the platform

4. **Tier Progression**: Users advance to higher tiers automatically, never demote

5. **Discount Stacking**: Decide if tier discounts can stack with promotion codes
   - **Recommended**: Allow both, apply tier discount first, then promotion

6. **Guest Users**: Guest users don't have tiers, only registered users

7. **Tier Benefits**: Consider additional benefits beyond discounts:
   - Priority booking
   - Exclusive screenings
   - Birthday rewards
   - Free upgrades

8. **Points Expiration**: Consider if points expire (e.g., annual reset)

9. **Tier Maintenance**: 
   - Consider if users must maintain activity to keep tier
   - Or once achieved, tier is permanent

10. **Display Order**: Always sort tiers by `pointsRequired` for consistent user experience

11. **Discount Limits**: 
    - For PERCENTAGE: Cap at reasonable level (typically 5-25%)
    - For FIXED_AMOUNT: Ensure it doesn't exceed typical booking amounts

12. **Historical Data**: Keep inactive tiers for users who achieved them previously

13. **Tier Announcements**: Notify users when they achieve a new tier

14. **Analytics**: Track tier distribution and discount usage for business insights

15. **Testing**: Test discount calculations with various booking amounts and tier combinations
