package com.nexcart.cartservice.cart.controller;

import com.nexcart.cartservice.cart.dto.request.AddItemRequest;
import com.nexcart.cartservice.cart.dto.response.CartResponse;
import com.nexcart.cartservice.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long userId) {
        log.info("REST request to get cart for user: {}", userId);
        CartResponse response = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/{userId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody AddItemRequest request) {
        log.info("REST request to add item to cart for user: {}", userId);
        CartResponse response = cartService.addItem(userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/user/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        log.info("REST request to update item quantity for user {}: product {} to {}", userId, productId, quantity);
        CartResponse response = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/user/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        log.info("REST request to remove item from cart for user {}: product {}", userId, productId);
        CartResponse response = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        log.info("REST request to clear cart for user: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
