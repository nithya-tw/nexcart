# NexCart

> A cloud-native e-commerce platform built with microservices architecture, demonstrating event-driven patterns, saga orchestration, and modern Java practices.

## Overview

NexCart is a production-inspired microservices application featuring:
- **8 Microservices** (5 core + 3 infrastructure)
- **Event-Driven Architecture** with Kafka
- **Saga Pattern** for distributed transactions
- **Redis Caching** for performance
- **Complete Observability** (Prometheus, Grafana, Loki)
- **74% Average Test Coverage** with JaCoCo (253 tests)
- **Kubernetes Ready** with complete manifests
- **CI/CD Pipeline** with GitHub Actions

---

## Architecture

### Microservices (8 Services)

**Core Business Services:**
- **User Service** (8080) - User management
- **Product Service** (8081) - Product catalog with Redis caching
- **Cart Service** (8082) - Shopping cart operations
- **Inventory Service** (8084) - Stock management with event-driven reservations
- **Order Service** (8083) - Order processing with saga orchestration

**Infrastructure Services:**
- **Discovery Service** (8761) - Eureka service registry
- **API Gateway** (8765) - Routing, JWT auth, rate limiting
- **Auth Service** (8086) - JWT token generation

### Key Patterns

- **Database-per-Service** - Each service has its own PostgreSQL database  
- **Event-Driven** - Kafka for async communication between services  
- **Saga Pattern** - Orchestration-based distributed transactions with compensation  
- **Service Discovery** - Eureka for dynamic service registration  
- **API Gateway** - Centralized routing and cross-cutting concerns  
- **Circuit Breaker** - Resilience4j for fault tolerance  
- **Caching** - Redis for high-performance product queries  
- **Rate Limiting** - 100 requests per 60 seconds per user/IP  

### Features

- Complete CRUD operations for users, products, carts, orders
- Product search with brand/category filters and pagination
- Stock reservation with automatic release on order failure
- Idempotent event consumers with deduplication
- JWT authentication with role-based access control
- Distributed tracing across all services
- Centralized logging with Grafana Loki
- Real-time metrics with Prometheus + Grafana
- Health checks and graceful shutdown
- Virtual Threads for high-concurrency operations
- Integration tests with Testcontainers (real databases)

---

## Testing & Quality

### Test Coverage (JaCoCo)

| Service | Line Coverage | Branch Coverage | Tests (Unit + Integration) |
|---------|---------------|-----------------|----------------------------|
| User Service | 81% | 100% | 54 unit tests |
| Product Service | 85% | 88% | 45 unit + 13 integration |
| Cart Service | 85% | 100% | 21 unit tests |
| Inventory Service | 60% | 70% | 58 unit tests |
| Auth Service | 72% | 92% | 27 unit tests |
| Order Service | 61% | 47% | 34 unit tests |

**Total:** 253 tests (239 unit + 14 integration)

**Check Coverage:**
```bash
# Individual service
mvn test jacoco:report -pl services/user-service
open services/user-service/target/site/jacoco/index.html

# Aggregate report (all services)
mvn test && cd jacoco-aggregate && mvn jacoco:report-aggregate
open jacoco-aggregate/target/site/jacoco-aggregate/index.html
```

### Test Types

- **Unit Tests** - Service layer, mappers, entities (JUnit 5 + Mockito)
- **Controller Tests** - REST API endpoints (@WebMvcTest)
- **Exception Handler Tests** - Error response validation
- **Entity Tests** - Business logic in domain models
- **Integration Tests** - End-to-end flows with Testcontainers (PostgreSQL + Redis)
  - Product Service: 13 integration tests covering search, filtering, CRUD
  - Tests verified with real databases (see `INTEGRATION_TESTS_VERIFIED.md`)

### Running Tests

```bash
# Run all tests (unit + integration)
# Requires Docker for integration tests
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_RYUK_DISABLED=true
mvn clean test

# Run unit tests only (no Docker required)
mvn test -Dtest='!ProductServiceIntegrationTest'

# Run integration tests only (requires Docker)
mvn test -Dtest=ProductServiceIntegrationTest -pl services/product-service
```

---

## Service Details

### Microservices

| Service | Port | Database | Key Features |
|---------|------|----------|--------------|
| User Service | 8080 | PostgreSQL | User CRUD, duplicate email detection |
| Product Service | 8081 | PostgreSQL | Catalog + search, **Redis caching**, slug generation |
| Cart Service | 8082 | PostgreSQL | Cart operations, price calculations |
| Inventory Service | 8084 | PostgreSQL | Stock tracking, **Kafka event consumer** |
| Order Service | 8083 | PostgreSQL | **Saga orchestration**, circuit breaker |
| Discovery (Eureka) | 8761 | - | Service registry |
| API Gateway | 8765 | - | Routing, JWT auth, rate limiting |
| Auth Service | 8086 | - | JWT token generation |

### Infrastructure

| Component | Port | Purpose |
|-----------|------|---------|
| PostgreSQL | 5432 | Primary database (5 DBs) |
| Kafka | 9092 | Event streaming (3 brokers) |
| Redis | 6379 | Product caching |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Dashboards (metrics + logs) |
| Loki | 3100 | Log aggregation |

---

## Technology Stack

**Backend**
- Java 21 (Records, Pattern Matching, Virtual Threads)
- Spring Boot 3.5
- Spring Cloud (Gateway, Eureka, OpenFeign)
- Maven multi-module

**Data & Messaging**
- PostgreSQL 15 (5 databases with Flyway migrations)
- Apache Kafka 3.6 (event streaming, 3 brokers)
- Redis 7.2 (caching with 1hr TTL, LRU eviction)

**Resilience & Security**
- Resilience4j (circuit breakers, rate limiting, retries)
- Spring Security + JWT authentication
- Role-based access control (ADMIN, USER)

**Observability**
- OpenTelemetry (distributed tracing)
- Prometheus (metrics collection)
- Grafana (dashboards)
- Loki + Promtail (centralized logging)
- Spring Boot Actuator (health checks)

**Testing**
- JUnit 5 + Mockito (unit tests)
- Testcontainers (integration tests with real databases)
- JaCoCo (code coverage 74% average)
- 253 total tests (239 unit + 14 integration)

**DevOps**
- Docker + Docker Compose
- Kubernetes (complete YAML manifests)
- GitHub Actions (CI/CD)
- Flyway database migrations

---

## Quick Start

### Prerequisites
- Java 21
- Docker Desktop
- Maven (included via wrapper)

### Run with Docker Compose

```bash
# Clone the repository
git clone https://github.com/nithya-tw/nexcart.git
cd nexcart

# Start all services (8 microservices + infrastructure)
docker-compose up -d

# View logs
docker-compose logs -f

# Check health
curl http://localhost:8761  # Eureka Dashboard
curl http://localhost:8080/actuator/health  # User Service
curl http://localhost:3000  # Grafana (admin/admin)

# Stop all services
docker-compose down
```

### Build and Run Locally

```bash
# Build all services
./mvnw clean install

# Run a specific service
cd services/user-service
./mvnw spring-boot:run

# Run tests with coverage
./mvnw test jacoco:report
open target/site/jacoco/index.html
```

---

## Project Structure

```text
nexcart/
├── services/                      # Microservices
│   ├── user-service/             # User management
│   ├── product-service/          # Product catalog + Redis
│   ├── cart-service/             # Shopping cart
│   ├── inventory-service/        # Stock + Kafka consumer
│   ├── order-service/            # Saga orchestration
│   ├── discovery-service/        # Eureka server
│   ├── api-gateway/              # Spring Cloud Gateway
│   └── auth-service/             # JWT authentication
├── jacoco-aggregate/              # Aggregate coverage reports
├── docker/                        # Dockerfiles
├── k8s/                           # Kubernetes manifests
└── docker-compose.yml             # Local orchestration
```

Each service follows feature-based packaging:
```
service-name/
└── src/main/java/com/nexcart/servicename/
    ├── config/                    # Configuration
    ├── common/                    # Shared utilities
    ├── exception/                 # Exception handling
    └── feature/                   # Domain feature
        ├── controller/           # REST endpoints
        ├── dto/                  # Request/Response DTOs
        ├── entity/               # JPA entities
        ├── mapper/               # Entity ↔ DTO
        ├── repository/           # Data access
        └── service/              # Business logic
```

---

## API Endpoints

All requests through API Gateway: `http://localhost:8765`

### Authentication
```bash
# Login (get JWT token)
POST /api/auth/login
{
  "username": "admin",
  "password": "admin"
}

# Returns: { "token": "eyJhbGc...", "type": "Bearer" }
```

### Users (Protected - requires JWT)
```bash
# Get all users
GET /api/users

# Create user
POST /api/users
{
  "username": "john.doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Products (Public)
```bash
# Get all products
GET /api/products?page=0&size=10

# Search products
GET /api/products/search?query=laptop&brand=Dell

# Get by ID
GET /api/products/1

# Create product (requires JWT)
POST /api/products
{
  "name": "Dell XPS 13",
  "description": "Laptop",
  "price": 1299.99,
  "sku": "DELL-XPS-13",
  "brand": "Dell",
  "category": "Laptops"
}
```

### Cart (Protected)
```bash
# Add to cart
POST /api/carts/1/items
{
  "productId": 1,
  "quantity": 2
}

# Get cart
GET /api/carts/1
```

### Orders (Protected)
```bash
# Create order (triggers saga)
POST /api/orders
{
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

**Demo Users:**
- `admin` / `admin` (ROLE_ADMIN, ROLE_USER)
- `user` / `user` (ROLE_USER)
* **API Gateway**: http://localhost:8765/api/**
* **Auth Service**: http://localhost:8086/api/v1/auth/login

**Observability:**
* **Prometheus**: http://localhost:9090
* **Grafana**: http://localhost:3000 (admin/admin)
* **Loki**: http://localhost:3100 (query via Grafana)

**Swagger UI** (each service): http://localhost:808X/swagger-ui/index.html

**Infrastructure:**
* **PostgreSQL**: localhost:5432 (user: postgres, password: postgres, 5 databases)
* **Kafka**: localhost:9092 (3 brokers)
* **Redis**: localhost:6379

---

## Architecture

### Microservices (5 Services)

```
┌──────────────────────────────────────────────────────────┐
│                    Client / API Calls                    │
└────────────────────────┬─────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ Product │    │  Cart   │    │  Order  │
    │ Service │    │ Service │    │ Service │
    └────┬────┘    └────┬────┘    └────┬────┘
         │              │              │
         │              │              ├──→ Kafka (OrderPlaced)
         │              │              │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────────┐
    │   DB    │    │   DB    │    │  Inventory  │
    │ (PgSQL) │    │ (PgSQL) │    │   Service   │
    └─────────┘    └─────────┘    └────┬────────┘
                                        │
                                   ┌────▼────┐
                                   │   DB    │
                                   │ (PgSQL) │
                                   └─────────┘
```

### Communication Patterns

| Type | Use Case | Example |
|------|----------|---------|
| **REST (Sync)** | Real-time queries | Order Service → Inventory Service (check stock) |
| **Kafka (Async)** | Event-driven | Order Service → Inventory Service (reserve stock) |
| **Circuit Breaker** | Resilience | Order → Inventory call fails → Fallback |

---

## Testing

```bash
# Run all tests (requires Docker for integration tests)
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_RYUK_DISABLED=true
./mvnw clean test

# Run unit tests only (no Docker required)
./mvnw test -Dtest='!ProductServiceIntegrationTest'

# Run tests with coverage
./mvnw clean test jacoco:report

# View coverage report
open services/product-service/target/site/jacoco/index.html

# Run integration tests for Product Service (requires Docker)
./mvnw -pl services/product-service test -Dtest=ProductServiceIntegrationTest
```

**Coverage Achieved:** 74% average across all services  
**Total Tests:** 253 tests (239 unit + 14 integration)

See `INTEGRATION_TESTS_VERIFIED.md` for integration test verification report.

---


## Kubernetes Deployment

Complete Kubernetes manifests available in `k8s/` directory:

```bash
# Deploy to Kubernetes
kubectl apply -f k8s/

# Check deployments
kubectl get all -n nexcart

# Access services
kubectl port-forward svc/api-gateway 8765:8765 -n nexcart
```

---

## Development

### Running Individual Services

```bash
# Build
./mvnw clean install

# Run specific service
cd services/user-service
./mvnw spring-boot:run
```

### Database Migrations

Flyway migrations run automatically on startup. Manual migration:

```bash
./mvnw flyway:migrate -pl services/user-service
```

### Environment Variables

Key configuration (set in `docker-compose.yml` or application.yml):

```yaml
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/userdb
SPRING_DATASOURCE_USERNAME=nexcart
SPRING_DATASOURCE_PASSWORD=nexcart

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# JWT
JWT_SECRET=your-256-bit-secret-key-here
JWT_EXPIRATION=3600000
```

---

## Performance Features

**Caching Strategy:**
- Product catalog cached in Redis (1-hour TTL)
- Automatic cache eviction on product updates
- LRU eviction policy for memory management

**Rate Limiting:**
- 100 requests per 60 seconds per user/IP
- Prevents API abuse
- Returns HTTP 429 when exceeded

**Circuit Breaker:**
- Protects Order → Inventory communication
- Automatic fallback responses
- Configurable thresholds and timeouts

---

## License

This project is built for educational purposes as part of a bootcamp assignment.

---

## Acknowledgments

Built with:
- Spring Boot & Spring Cloud ecosystem
- Apache Kafka for event streaming
- PostgreSQL for data persistence
- Redis for caching
- Prometheus & Grafana for observability
- Docker & Kubernetes for containerization
- Testcontainers for integration testing
