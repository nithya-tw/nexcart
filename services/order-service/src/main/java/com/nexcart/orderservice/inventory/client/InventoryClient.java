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
     * Reserve inventory for an order (Saga Pattern - Step 1).
     * 
     * Saga Pattern Context:
     * - This is a compensatable transaction
     * - If subsequent steps fail, releaseStock() must be called
     * - Idempotent: Multiple calls with same orderId should not double-reserve
     * 
     * @param productId Product ID to reserve
     * @param quantity Quantity to reserve
     * @param orderId Order ID for idempotency
     * @return true if reservation successful
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventoryService")
    public boolean reserveStock(Long productId, Integer quantity, Long orderId) {
        log.info("Reserving inventory for product: {}, quantity: {}, orderId: {}", 
                productId, quantity, orderId);
        
        try {
            String url = String.format("%s/api/v1/inventory/reservations", inventoryServiceUrl);
            
            // Request body matching inventory-service DTO
            var request = new ReserveStockRequest(productId, orderId, quantity, 15);
            
            var response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(ReservationResponse.class);
            
            log.info("Stock reservation result for product {}: {}", productId, response);
            return response != null && "ACTIVE".equals(response.status());
            
        } catch (RestClientException e) {
            log.error("Failed to reserve stock for product: {}", productId, e);
            throw e;
        }
    }
    
    /**
     * Release previously reserved inventory (Saga Pattern - Compensation).
     * 
     * Compensation Logic:
     * - Called when order processing fails after inventory reservation
     * - Must be idempotent: safe to call multiple times
     * - Should not fail (best effort, with logging)
     * 
     * @param productId Product ID (for logging only)
     * @param quantity Quantity (for logging only)
     * @param orderId Order ID for tracking
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseStockFallback")
    @Retry(name = "inventoryService")
    public void releaseStock(Long productId, Integer quantity, Long orderId) {
        log.info("Releasing inventory for product: {}, quantity: {}, orderId: {}", 
                productId, quantity, orderId);
        
        try {
            String url = String.format("%s/api/v1/inventory/reservations/order/%d", 
                    inventoryServiceUrl, orderId);
            
            restClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("Stock released successfully for product: {} (orderId: {})", productId, orderId);
            
        } catch (RestClientException e) {
            // Log but don't throw - compensation should be best effort
            log.error("Failed to release stock for product: {} (orderId: {}). " +
                    "Manual intervention may be required.", productId, orderId, e);
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
    
    private boolean reserveStockFallback(Long productId, Integer quantity, Long orderId, Throwable throwable) {
        log.error("Failed to reserve stock for product: {} (orderId: {}). Circuit breaker activated. " +
                "Reason: {}", productId, orderId, throwable.getMessage());
        return false;
    }
    
    private void releaseStockFallback(Long productId, Integer quantity, Long orderId, Throwable throwable) {
        log.error("Failed to release stock for product: {} (orderId: {}). Manual intervention required. " +
                "Reason: {}", productId, orderId, throwable.getMessage());
        // In production: Send alert to ops team or add to dead letter queue
    }
    
    // Inner classes for request/response DTOs
    private record ReserveStockRequest(Long productId, Long orderId, Integer quantity, Integer ttlMinutes) {}
    private record ReservationResponse(Long id, Long productId, Long orderId, Integer quantity, String status) {}
}
