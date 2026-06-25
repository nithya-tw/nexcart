# NexCart Architecture

## Services

* User Service
* Product Service
* Inventory Service
* Order Service

Each service has:

* Its own database
* Its own domain and business logic
* REST APIs
* Independent deployment

## Communication

### Current

* REST APIs

### Planned

* Apache Kafka (event-driven communication)
* Redis (caching)
* Service Discovery
* API Gateway
