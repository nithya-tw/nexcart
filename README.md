# NexCart

> A Cloud-Native E-Commerce Platform built using Java 21, Spring Boot 3.5, and a microservices architecture.

## Overview

NexCart is a production-inspired cloud-native e-commerce platform designed to demonstrate modern backend engineering practices.

The platform is built as a collection of independently deployable microservices with clear service boundaries, event-driven communication, and logical data ownership.

The project emphasizes clean architecture, maintainability, scalability, and testability while following industry best practices.

---

## Current Status

### ✅ Completed

* Multi-module Maven project
* User Service
* CRUD REST APIs
* DTO-first architecture
* Bean Validation
* Global Exception Handling
* PostgreSQL integration
* OpenAPI / Swagger
* Controller tests
* Engineering documentation

### 🚧 Planned

* Product Service
* Inventory Service
* Order Service
* Docker Compose
* Redis
* Apache Kafka
* Service Discovery
* Config Server
* JWT Authentication
* Kubernetes Deployment

---

## Technology Stack

### Backend

* Java 21
* Spring Boot 3.5
* Maven

### Database

* PostgreSQL
* MongoDB (planned)
* Redis (planned)

### Messaging

* Apache Kafka (planned)

### Infrastructure

* Docker
* Docker Compose
* Kubernetes (planned)

### Testing

* JUnit 5
* Mockito
* Testcontainers

---

## Engineering Principles

* Clean Architecture
* Feature-based Packaging
* DTO-first API Design
* SOLID Principles
* Repository Pattern
* Constructor Injection
* Bean Validation
* Global Exception Handling
* RESTful API Design
* Database per Service
* Production-ready Engineering Practices

---

## Project Structure

```text
nexcart/
├── docs/
├── services/
│   └── user-service/
├── pom.xml
├── mvnw
└── README.md
```

---

## Running the Project

### Prerequisites

* Java 21
* Docker
* Maven Wrapper

### Start PostgreSQL

```bash
docker run \
  --name nexcart-postgres \
  -e POSTGRES_DB=nexcart \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:17
```

### Run User Service

From the project root:

```bash
./mvnw -pl services/user-service spring-boot:run
```

### Run Tests

```bash
./mvnw clean test
```

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

---
