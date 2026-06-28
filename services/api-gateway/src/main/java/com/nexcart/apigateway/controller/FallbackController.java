package com.nexcart.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return buildFallbackResponse("User Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        return buildFallbackResponse("Product Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/carts")
    public ResponseEntity<Map<String, Object>> cartServiceFallback() {
        return buildFallbackResponse("Cart Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        return buildFallbackResponse("Order Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryServiceFallback() {
        return buildFallbackResponse("Inventory Service is temporarily unavailable. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
