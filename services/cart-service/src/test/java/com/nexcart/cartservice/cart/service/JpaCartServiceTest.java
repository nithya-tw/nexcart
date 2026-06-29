package com.nexcart.cartservice.cart.service;

import com.nexcart.cartservice.cart.dto.request.AddItemRequest;
import com.nexcart.cartservice.cart.dto.response.CartResponse;
import com.nexcart.cartservice.cart.entity.Cart;
import com.nexcart.cartservice.cart.entity.CartItem;
import com.nexcart.cartservice.cart.repository.CartItemRepository;
import com.nexcart.cartservice.cart.repository.CartRepository;
import com.nexcart.cartservice.cart.service.impl.JpaCartService;
import com.nexcart.cartservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 📚 CONCEPT: Cart Service Testing
 * 🎯 ANALOGY: Shopping cart is like your physical cart at a grocery store.
 *             You can add items, update quantities, remove items, or clear everything.
 * 
 * ✅ WHY: 
 *    - Tests cart lifecycle: create, add, update, remove, clear
 *    - Validates quantity management
 *    - Ensures accurate total calculations
 * 
 * 🏢 REAL-WORLD: Amazon's cart service handles millions of concurrent users
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCartService Unit Tests")
class JpaCartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private JpaCartService cartService;

    private Cart mockCart;
    private CartItem mockItem;

    @BeforeEach
    void setUp() {
        mockCart = Cart.builder()
                .id(1L)
                .userId(100L)
                .status(Cart.CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build();

        mockItem = CartItem.builder()
                .id(1L)
                .cart(mockCart)
                .productId(200L)
                .quantity(2)
                .price(BigDecimal.valueOf(99.99))
                .build();
    }

    @Test
    @DisplayName("Should create new cart when user has no active cart")
    void shouldCreateNewCart() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        CartResponse result = cartService.getOrCreateCart(100L);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(100L);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should return existing cart when user has active cart")
    void shouldReturnExistingCart() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.getOrCreateCart(100L);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(100L);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should get cart successfully")
    void shouldGetCart() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.getCart(100L);

        assertThat(result).isNotNull();
        verify(cartRepository).findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw exception when cart not found")
    void shouldThrowExceptionWhenCartNotFound() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("100");

        verify(cartRepository).findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should add new item to cart")
    void shouldAddNewItemToCart() {
        AddItemRequest request = new AddItemRequest(200L, 2, BigDecimal.valueOf(99.99));
        
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 200L))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(mockItem);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.addItem(100L, request);

        assertThat(result).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should update quantity when item already exists in cart")
    void shouldUpdateExistingItemQuantity() {
        AddItemRequest request = new AddItemRequest(200L, 3, BigDecimal.valueOf(99.99));
        mockCart.getItems().add(mockItem);
        
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 200L))
                .thenReturn(Optional.of(mockItem));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.addItem(100L, request);

        assertThat(result).isNotNull();
        assertThat(mockItem.getQuantity()).isEqualTo(5); // 2 + 3
        verify(cartItemRepository).save(mockItem);
    }

    @Test
    @DisplayName("Should update item quantity successfully")
    void shouldUpdateItemQuantity() {
        mockCart.getItems().add(mockItem);
        
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 200L))
                .thenReturn(Optional.of(mockItem));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.updateItemQuantity(100L, 200L, 5);

        assertThat(result).isNotNull();
        assertThat(mockItem.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(mockItem);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent item")
    void shouldThrowExceptionWhenUpdatingNonExistentItem() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 200L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(100L, 200L, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("200");
    }

    @Test
    @DisplayName("Should remove item from cart successfully")
    void shouldRemoveItemFromCart() {
        mockCart.getItems().add(mockItem);
        
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.removeItem(100L, 200L);

        assertThat(result).isNotNull();
        verify(cartItemRepository).deleteByCartIdAndProductId(1L, 200L);
    }

    @Test
    @DisplayName("Should throw exception when removing item from non-existent cart")
    void shouldThrowExceptionWhenRemovingFromNonExistentCart() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(100L, 200L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should clear cart successfully")
    void shouldClearCart() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));

        cartService.clearCart(100L);

        verify(cartItemRepository).deleteAllByCartId(1L);
    }

    @Test
    @DisplayName("Should calculate total amount correctly")
    void shouldCalculateTotalAmount() {
        CartItem item1 = CartItem.builder()
                .id(1L)
                .productId(200L)
                .quantity(2)
                .price(BigDecimal.valueOf(10.00))
                .build();
        
        CartItem item2 = CartItem.builder()
                .id(2L)
                .productId(201L)
                .quantity(3)
                .price(BigDecimal.valueOf(20.00))
                .build();
        
        mockCart.setItems(Arrays.asList(item1, item2));
        
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.getCart(100L);

        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(80.00)); // 20 + 60
        assertThat(result.totalItems()).isEqualTo(5); // 2 + 3
    }

    @Test
    @DisplayName("Should return zero total for empty cart")
    void shouldReturnZeroTotalForEmptyCart() {
        when(cartRepository.findByUserIdAndStatus(100L, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(mockCart));

        CartResponse result = cartService.getCart(100L);

        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalItems()).isEqualTo(0);
    }
}
