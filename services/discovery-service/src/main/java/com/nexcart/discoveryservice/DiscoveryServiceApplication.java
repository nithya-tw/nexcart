package com.nexcart.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Service
 * 
 * Acts as service registry where all microservices register themselves.
 * Other services can discover and communicate with each other dynamically.
 * 
 * Why Eureka?
 * - Dynamic service discovery (no hardcoded URLs)
 * - Load balancing support
 * - Health checking
 * - Self-preservation mode for network partitions
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
