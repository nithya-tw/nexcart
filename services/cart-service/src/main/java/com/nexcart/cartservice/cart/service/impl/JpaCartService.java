package com.nexcart.cartservice.cart.service.impl;

import com.nexcart.cartservice.cart.dto.request.AddItemRequest;
import com.nexcart.cartservice.cart.dto.response.CartItemResponse;
import com.nexcart.cartservice.cart.dto.response.CartResponse;
import com.nexcart.cartservice.cart.entity.Cart;
import com.nexcart.cartservice.cart.entity.CartItem;
import com.nexcart.cartservice.cart.repository.CartItemRepository;
import com.nexcart.cartservice.cart.repository.CartRepository;
import com.nexcart.cartservice.cart.service.CartService;
import com.nexcart.cartservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaCartService implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public CartResponse getOrCreateCart(Long userId) {
        log.info("Getting or creating cart for user: {}", userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .status(Cart.CartStatus.ACTIVE)
                            .build();
                    return cartRepository.save(newCart);
                });
        return mapToResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        log.info("Getting cart for user: {}", userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddItemRequest request) {
        log.info("Adding item to cart for user {}: product {} x{}", userId, request.productId(), request.quantity());

        Cart cart = cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .status(Cart.CartStatus.ACTIVE)
                            .build();
                    return cartRepository.save(newCart);
                });

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId())
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            cartItemRepository.save(existingItem);
            log.info("Updated existing item quantity to: {}", existingItem.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.productId())
                    .quantity(request.quantity())
                    .price(request.price())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
            log.info("Added new item to cart");
        }

        cartRepository.flush();
        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, Integer quantity) {
        log.info("Updating item quantity for user {}: product {} to {}", userId, productId, quantity);

        Cart cart = cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart: " + productId));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        log.info("Removing item from cart for user {}: product {}", userId, productId);

        Cart cart = cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);

        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user: {}", userId);

        Cart cart = cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        cartItemRepository.deleteAllByCartId(cart.getId());
        log.info("Cart cleared for user: {}", userId);
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus().name())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
