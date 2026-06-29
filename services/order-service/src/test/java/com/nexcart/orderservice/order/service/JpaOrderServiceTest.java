package com.nexcart.orderservice.order.service;

import com.nexcart.orderservice.exception.InvalidOrderStateException;
import com.nexcart.orderservice.exception.ResourceNotFoundException;
import com.nexcart.orderservice.order.dto.request.CreateOrderRequest;
import com.nexcart.orderservice.order.dto.request.OrderItemRequest;
import com.nexcart.orderservice.order.dto.request.UpdateOrderStatusRequest;
import com.nexcart.orderservice.order.dto.response.OrderResponse;
import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderItem;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.repository.OrderItemRepository;
import com.nexcart.orderservice.order.repository.OrderRepository;
import com.nexcart.orderservice.order.service.impl.JpaOrderService;
import com.nexcart.orderservice.saga.OrderSagaOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaOrderService Tests")
class JpaOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OutboxPublisherService outboxPublisherService;

    @Mock
    private OrderSagaOrchestrator sagaOrchestrator;

    @InjectMocks
    private JpaOrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private Order existingOrder;

    @BeforeEach
    void setUp() {
        createOrderRequest = new CreateOrderRequest(
                10L,
                "221B Baker Street",
                "CARD",
                "Leave at the front desk",
                List.of(
                        new OrderItemRequest(101L, "Keyboard", 2, new BigDecimal("49.99")),
                        new OrderItemRequest(102L, "Mouse", 1, new BigDecimal("29.50"))
                )
        );

        existingOrder = buildOrder(
                1L,
                "ORD-20240101-0001",
                10L,
                OrderStatus.PENDING,
                List.of(
                        buildItem(11L, 101L, "Keyboard", 2, "49.99"),
                        buildItem(12L, 102L, "Mouse", 1, "29.50")
                )
        );
    }

    @Test
    @DisplayName("Should create an order successfully with multiple items")
    void shouldCreateOrderSuccessfullyWithMultipleItems() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });
        doNothing().when(sagaOrchestrator).executeOrderSaga(any(Order.class));
        doNothing().when(outboxPublisherService).saveEventToOutbox(any(), eq("Order"), eq(99L));

        OrderResponse response = orderService.createOrder(createOrderRequest);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.itemCount()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("129.48");
        assertThat(response.orderNumber()).startsWith("ORD-");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getItems()).hasSize(2);
        assertThat(savedOrder.getItems()).allMatch(item -> item.getOrder() == savedOrder);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("129.48");

        verify(sagaOrchestrator).executeOrderSaga(savedOrder);
        verify(outboxPublisherService).saveEventToOutbox(any(), eq("Order"), eq(99L));
    }

    @Test
    @DisplayName("Should calculate total amount correctly when creating an order")
    void shouldCalculateTotalAmountCorrectly() {
        CreateOrderRequest request = new CreateOrderRequest(
                22L,
                "742 Evergreen Terrace",
                "UPI",
                "Fragile items",
                List.of(
                        new OrderItemRequest(201L, "Headphones", 3, new BigDecimal("19.99")),
                        new OrderItemRequest(202L, "Stand", 2, new BigDecimal("15.50"))
                )
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.totalAmount()).isEqualByComparingTo("90.97");
        assertThat(response.items())
                .extracting(item -> item.subtotal().toPlainString())
                .containsExactly("59.97", "31.00");
    }

    @Test
    @DisplayName("Should return an order by ID")
    void shouldGetOrderByIdSuccessfully() {
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(existingOrder));

        OrderResponse response = orderService.getOrderById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("129.48");
    }

    @Test
    @DisplayName("Should throw when order ID is not found")
    void shouldThrowWhenOrderIdIsNotFound() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with ID: 999");
    }

    @Test
    @DisplayName("Should return orders for a user ID")
    void shouldGetOrdersByUserId() {
        Order secondOrder = buildOrder(2L, "ORD-20240101-0002", 10L, OrderStatus.CONFIRMED, List.of());
        when(orderRepository.findByUserId(10L)).thenReturn(List.of(existingOrder, secondOrder));

        List<OrderResponse> responses = orderService.getOrdersByUserId(10L);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(OrderResponse::id).containsExactly(1L, 2L);
        assertThat(responses).allMatch(response -> response.items().isEmpty());
    }

    @Test
    @DisplayName("Should return all orders")
    void shouldGetAllOrders() {
        Order secondOrder = buildOrder(2L, "ORD-20240101-0002", 20L, OrderStatus.SHIPPED, List.of());
        when(orderRepository.findAll()).thenReturn(List.of(existingOrder, secondOrder));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(OrderResponse::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Should update order status")
    void shouldUpdateOrderStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(existingOrder);

        OrderResponse response = orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.SHIPPED));

        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Should cancel an order successfully")
    void shouldCancelOrderSuccessfully() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));

        orderService.cancelOrder(1L);

        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Should reject cancelling an order in an invalid state")
    void shouldRejectCancellingOrderInInvalidState() {
        existingOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Cannot cancel order with status: DELIVERED");

        verify(orderRepository, never()).save(any(Order.class));
    }

    private Order buildOrder(Long id, String orderNumber, Long userId, OrderStatus status, List<OrderItem> items) {
        Order order = Order.builder()
                .id(id)
                .orderNumber(orderNumber)
                .userId(userId)
                .shippingAddress("221B Baker Street")
                .paymentMethod("CARD")
                .notes("Leave at the front desk")
                .status(status)
                .build();
        order.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        order.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 12, 30));
        items.forEach(order::addItem);
        order.calculateTotalAmount();
        return order;
    }

    private OrderItem buildItem(Long id, Long productId, String productName, Integer quantity, String unitPrice) {
        OrderItem item = OrderItem.builder()
                .id(id)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(new BigDecimal(unitPrice))
                .build();
        item.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        item.calculateSubtotal();
        return item;
    }
}
