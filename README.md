# NexCart

> A cloud-native e-commerce platform built with microservices architecture, demonstrating event-driven patterns, saga orchestration, and modern Java practices.

## Overview

NexCart is a production-inspired microservices application featuring:
- **8 Microservices** (5 core + 3 infrastructure)
- **Event-Driven Architecture** with Kafka
- **Saga Pattern** for distributed transactions
- **Redis Caching** for performance
- **Complete Observability** (Prometheus, Grafana, Loki)
- **84.3% Average Test Coverage** with JaCoCo (364 comprehensive tests)
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

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CLIENT / FRONTEND                           │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
                    ┌────────────────────────────────┐
                    │      API GATEWAY (8765)        │
                    │  - JWT Authentication          │
                    │  - Rate Limiting (100/min)     │
                    │  - Circuit Breaker             │
                    │  - Dynamic Routing             │
                    └────────────────────────────────┘
                                     │
                   ┌─────────────────┼─────────────────┐
                   │                 │                 │
        ┌──────────▼──────────┐     │     ┌──────────▼──────────┐
        │   AUTH SERVICE      │     │     │  DISCOVERY SERVICE  │
        │      (8086)         │◄────┘────►│      (8761)         │
        │  - JWT Generation   │           │  - Eureka Server    │
        │  - Token Validation │           │  - Service Registry │
        └─────────────────────┘           └─────────────────────┘
                                                     │
                   ┌─────────────────────────────────┼─────────────────┐
                   │                                 │                 │
        ┌──────────▼──────────┐         ┌───────────▼────────┐       │
        │   USER SERVICE      │         │  PRODUCT SERVICE    │       │
        │      (8080)         │         │      (8081)         │       │
        │  - User Management  │         │  - Product Catalog  │       │
        │  - CRUD Operations  │         │  - Search/Filter    │       │
        └──────────┬──────────┘         │  - Pagination       │       │
                   │                    └──────────┬──────────┘       │
                   │                               │                  │
                   │                    ┌──────────▼──────────┐       │
                   │                    │    REDIS CACHE      │       │
                   │                    │  - Product Catalog  │       │
                   │                    │  - 1-hour TTL       │       │
                   │                    └─────────────────────┘       │
                   │                                                  │
        ┌──────────▼──────────┐         ┌──────────────────┐         │
        │   CART SERVICE      │         │ INVENTORY SERVICE│◄────────┘
        │      (8082)         │         │      (8084)      │
        │  - Shopping Cart    │         │  - Stock Mgmt    │
        │  - Cart Operations  │         │  - Reservations  │
        └──────────┬──────────┘         └────────┬─────────┘
                   │                              │
                   │         ┌────────────────────┼────────────────┐
                   │         │                    │                │
                   │    ┌────▼──────┐      ┌──────▼──────┐        │
                   └───►│   ORDER   │◄────►│    KAFKA    │◄───────┘
                        │  SERVICE  │      │   BROKER    │
                        │  (8083)   │      │             │
                        │  - Orders │      │ Topics:     │
                        │  - Saga   │      │ • order-events        │
                        │  Pattern  │      │ • inventory-events    │
                        │  - DLQ    │      │ • order-events.DLT    │
                        └─────┬─────┘      │ • inventory-events.DLT│
                              │            └─────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────┐
              │  OBSERVABILITY STACK      │
              ├───────────────────────────┤
              │  • Prometheus (Metrics)   │
              │  • Grafana (Dashboards)   │
              │  • Loki (Logs)            │
              │  • Distributed Tracing    │
              └───────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                         DATA LAYER (PostgreSQL)                          │
├─────────────────────────────────────────────────────────────────────────┤
│  userdb  │  productdb  │  cartdb  │  inventorydb  │  orderdb  │  authdb │
└─────────────────────────────────────────────────────────────────────────┘

Legend:
  ─────►  Synchronous HTTP/REST calls
  ◄────►  Service Discovery registration
  ═════►  Asynchronous Kafka event streaming
```

### Key Patterns

- **Database-per-Service** - Each service has its own PostgreSQL database  
- **Event-Driven** - Kafka for async communication between services  
- **Saga Pattern** - Orchestration-based distributed transactions with compensation  
- **Service Discovery** - Eureka for dynamic service registration  
- **API Gateway** - Centralized routing, JWT auth, rate limiting (100 req/min)
- **Circuit Breaker** - Resilience4j for fault tolerance  
- **Caching** - Redis for high-performance product queries  
- **Observability** - Prometheus, Grafana, Loki for metrics, dashboards, and logs
- **High Test Coverage** - 84.3% average with 364 tests (345 unit + 19 integration)

---

## Testing & Quality

### Test Coverage (JaCoCo)

**Overall Average Test Coverage: 84.3%** (81.3% Line | 87.3% Branch)

| Service | Line Coverage | Branch Coverage |
|---------|---------------|-----------------|
| Product Service | 91% | 80% |
| Cart Service | 85% | 100% |
| Inventory Service | 84% | 90% |
| User Service | 82% | 100% |
| Order Service | 74% | 62% |
| Auth Service | 72% | 92% |

**Total:** 364 tests (345 unit + 19 integration)

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
  - Product Service: 14 integration tests covering search, filtering, pagination, CRUD
  - Tests verified with real databases

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

## Technology Stack

**Services & Ports**
- User (8080), Product (8081), Cart (8082), Order (8083), Inventory (8084), Auth (8086)
- Discovery/Eureka (8761), API Gateway (8765)

**Backend**
- Java 21 (Virtual Threads, Records, Pattern Matching)
- Spring Boot 3.5
- Spring Cloud (Gateway, Eureka, OpenFeign)
- Maven multi-module

**Data & Messaging**
- PostgreSQL 15 (6 databases with Flyway migrations)
- Apache Kafka 3.6 (event streaming, 3 brokers, DLQ support)
- Redis 7.2 (caching with 1hr TTL, LRU eviction)

**Resilience & Security**
- Resilience4j (circuit breakers, rate limiting)
- Spring Security + JWT authentication

**Observability**
- Prometheus + Grafana (metrics & dashboards)
- Loki + Promtail (centralized logging)
- OpenTelemetry + Micrometer Tracing (Order & Inventory services)

**Testing & DevOps**
- JUnit 5 + Mockito + Testcontainers (364 tests, 84.3% coverage)
- Docker Compose + Kubernetes manifests
- GitHub Actions CI/CD

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

**Demo Users (local development only):**
- `admin` / `admin` (ROLE_ADMIN, ROLE_USER)
- `user` / `user` (ROLE_USER)

> ⚠️ **Security Note:** Change default credentials in production environments

* **API Gateway**: http://localhost:8765/api/**
* **Auth Service**: http://localhost:8086/api/v1/auth/login

**Observability:**
* **Prometheus**: http://localhost:9090
* **Grafana**: http://localhost:3000 (admin/admin)
* **Loki**: http://localhost:3100 (query via Grafana)

**Swagger UI** (each service): http://localhost:808X/swagger-ui/index.html

**Infrastructure:**
* **PostgreSQL**: localhost:5432 (6 databases - credentials in docker-compose.yml)
* **Kafka**: localhost:9092 (3 brokers)
* **Redis**: localhost:6379

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

Key configuration (set in `docker-compose.yml` or `application.yml`):

```yaml
# Database (credentials in docker-compose.yml)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/userdb
SPRING_DATASOURCE_USERNAME=nexcart

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# JWT (use strong secret in production)
JWT_SECRET=your-256-bit-secret-key-here
JWT_EXPIRATION=3600000
```

---

## License

This project is for educational and demonstration purposes.
