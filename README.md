# Movie Booking System

A comprehensive cinema ticket booking platform built with Spring Boot, PostgreSQL, Redis, and React. Features real-time seat locking, payment processing with MoMo integration, membership tiers, dynamic pricing, and performance monitoring with Prometheus and Grafana.

## Tech Stack

**Backend:** Spring Boot 3.5.6, Java 21, PostgreSQL 17, Redis 7  
**Frontend:** React, Vite, TailwindCSS  
**Infrastructure:** Docker, Terraform, Azure  
**Monitoring:** Prometheus, Grafana, k6 Load Testing  
**Testing:** JUnit, Testcontainers

## Key Features

- **Seat Management:** Real-time seat locking with Redis, concurrent booking prevention
- **Payment Integration:** MoMo & PayPal payment gateways
- **Dynamic Pricing:** Configurable price modifiers (time-based, seat type, special events)
- **Membership System:** Tiered loyalty program with exclusive benefits
- **Promotions:** Flexible discount codes and campaigns
- **QR Tickets:** QR code generation for seamless ticket validation
- **Admin Dashboard:** Complete cinema, movie, and showtime management
- **Load Testing:** k6 scenarios for performance validation

## Quick Start with Docker Compose

### Prerequisites

- Docker & Docker Compose
- Create `backend/.env` file with required variables (see below)

### Environment Setup

Create `backend/.env`:

```env
# Database
POSTGRES_DB=moviebooking
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
DATA_SOURCE_URL=jdbc:postgresql://db:5432/moviebooking

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT
JWT_SECRET=your-secret-key-min-256-bits
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000

# MoMo Payment
MOMO_PARTNER_CODE=your_partner_code
MOMO_ACCESS_KEY=your_access_key
MOMO_SECRET_KEY=your_secret_key
MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api/create
MOMO_REDIRECT_URL=http://localhost:5173/payment/callback
MOMO_IPN_URL=http://localhost:8080/api/payments/momo/ipn

# PayPal Payment
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret
PAYPAL_MODE=sandbox
PAYPAL_RETURN_URL=http://localhost:5173/payment/callback
PAYPAL_CANCEL_URL=http://localhost:5173/payment/cancelled

# Postgres Exporter
DATA_SOURCE_NAME=postgresql://postgres:your_password@db:5432/moviebooking?sslmode=disable

# Cloudinary (optional)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

### Run the Application

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f moviebooking

# Stop services
docker-compose down

# Clean up volumes
docker-compose down -v
```

### Access Services

- **Backend API:** http://localhost:8080/api
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **Health Check:** http://localhost:8080/health
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000
- **PostgreSQL:** localhost:5433
- **Redis:** localhost:6379

### Run Load Tests

```bash
# Execute k6 scenarios
docker-compose --profile k6 run k6 run /scripts/scenarios/01-seat-lock-concurrency.js

# Available scenarios:
# - 01-seat-lock-concurrency.js - Concurrent seat booking stress test
# - 02-browsing-load.js - User browsing patterns
# - 03-booking-workflow.js - Complete booking flow
# - 04-spike-test.js - Traffic spike simulation
# - 05-soak-test.js - Long-running endurance test
```

## Development

### Build Backend Locally

```bash
cd backend

# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Run locally (requires external PostgreSQL and Redis)
./mvnw spring-boot:run
```

### Run Frontend

```bash
cd frontend/movie-booking-frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

### Database Initialization

The application auto-creates tables and seeds sample data on first run when `backend/.env` contains development settings.

## API Documentation

Comprehensive API docs available at `/backend/docs/`:

- [Authentication](backend/docs/API-Authentication.md) - Register, login, JWT tokens
- [Bookings](backend/docs/API-Bookings.md) - Seat locking, booking lifecycle
- [Checkout](backend/docs/API-Checkout.md) - Atomic booking + payment
- [Movies](backend/docs/API-Movies.md) - Browse, search, filter movies
- [Showtimes](backend/docs/API-Showtimes.md) - Showtime scheduling
- [Payments](backend/docs/API-Payments.md) - MoMo integration, refunds

## Monitoring

Prometheus metrics include:
- Application health and performance
- JVM metrics (heap, threads, GC)
- Database connection pool stats
- Custom business metrics (bookings, payments)
- System metrics via node-exporter

## Project Structure

```
├── backend/              # Spring Boot REST API
│   ├── src/main/java/    # Application source
│   ├── src/test/java/    # JUnit & integration tests
│   └── docs/             # API documentation
├── infrastructure/       # Terraform IaC
├── k6-tests/            # Load testing scenarios
└── docker-compose.yml   # Service orchestration
```

## License

SE113 Project - University Assignment
