package com.nexcart.cartservice.cart.service;

import com.nexcart.cartservice.cart.dto.request.AddItemRequest;
import com.nexcart.cartservice.cart.dto.response.CartResponse;

public interface CartService {

    CartResponse getOrCreateCart(Long userId);

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddItemRequest request);

    CartResponse updateItemQuantity(Long userId, Long productId, Integer quantity);

    CartResponse removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}
