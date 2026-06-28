package com.nexcart.orderservice.inventory.client;

import com.nexcart.orderservice.inventory.dto.InventoryAvailabilityResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for communicating with Inventory Service.
 * Includes circuit breaker and retry mechanisms for resilience.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {
    
    private final RestClient restClient;
    
    @Value("${inventory.service.url:http://localhost:8083}")
    private String inventoryServiceUrl;
    
    /**
     * Check if inventory is available for a product.
     * 
     * Circuit Breaker Pattern:
     * - Tracks failures and opens circuit after 50% failure rate
     * - Falls back to pessimistic response when circuit is open
     * 
     * Retry Pattern:
     * - Retries up to 3 times with exponential backoff
     * - Helps with transient network failures
     * 
     * @param productId Product ID to check
     * @param quantity Quantity needed
     * @return true if available, false otherwise
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "checkInventoryFallback")
    @Retry(name = "inventoryService")
    public boolean checkInventoryAvailability(Long productId, Integer quantity) {
        log.info("Checking inventory for product: {}, quantity: {}", productId, quantity);
        
        try {
            String url = String.format("%s/api/v1/inventory/product/%d", 
                    inventoryServiceUrl, productId);
            
            InventoryAvailabilityResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(InventoryAvailabilityResponse.class);
            
            if (response == null) {
                log.warn("Null response from inventory service for product: {}", productId);
                return false;
            }
            
            boolean available = response.getAvailableQuantity() != null && 
                               response.getAvailableQuantity() >= quantity;
            
            log.info("Inventory check result for product {}: available={}, stock={}", 
                    productId, available, response.getAvailableQuantity());
            
            return available;
            
        } catch (RestClientException e) {
            log.error("Failed to check inventory for product: {}", productId, e);
            throw e;
        }
    }
    
    /**
     * Fallback method when inventory service is unavailable.
     * 
     * Graceful Degradation Strategy:
     * - Returns false (pessimistic approach) to prevent overselling
     * - Logs the fallback for monitoring
     * - Alternative: Could return cached values or estimated availability
     * 
     * @param productId Product ID
     * @param quantity Quantity
     * @param throwable Original exception
     * @return false (pessimistic fallback)
     */
    private boolean checkInventoryFallback(Long productId, Integer quantity, Throwable throwable) {
        log.warn("Inventory service unavailable for product: {}. Falling back to pessimistic response. " +
                "Reason: {}", productId, throwable.getMessage());
        
        // Pessimistic approach: assume not available to prevent overselling
        // Alternative strategies:
        // 1. Return cached inventory data (if recently fetched)
        // 2. Allow order creation but mark as "pending verification"
        // 3. Use historical data to estimate availability
        
        return false;
    }
}
