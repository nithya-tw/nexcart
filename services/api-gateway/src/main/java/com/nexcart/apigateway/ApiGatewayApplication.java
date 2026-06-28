package com.nexcart.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Service
 * 
 * Single entry point for all microservices.
 * Features:
 * - Request routing based on path patterns
 * - Service discovery integration with Eureka
 * - Load balancing across service instances
 * - Circuit breaker for fault tolerance
 * - Centralized CORS configuration
 * - Request/Response logging
 * 
 * Why Gateway Pattern?
 * - Decouples clients from individual services
 * - Single place for cross-cutting concerns (auth, logging)
 * - Backend service URLs can change without affecting clients
 * - Simplifies client code (one endpoint to remember)
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
