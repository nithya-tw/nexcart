package com.nexcart.orderservice.saga;

import com.nexcart.orderservice.inventory.client.InventoryClient;
import com.nexcart.orderservice.order.entity.Order;
import com.nexcart.orderservice.order.entity.OrderItem;
import com.nexcart.orderservice.order.entity.OrderStatus;
import com.nexcart.orderservice.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSagaOrchestrator Tests")
class OrderSagaOrchestratorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentProcessor paymentProcessor;

    @InjectMocks
    private OrderSagaOrchestrator orderSagaOrchestrator;

    private Order singleItemOrder;
    private Order multiItemOrder;

    @BeforeEach
    void setUp() {
        singleItemOrder = buildOrder(1001L, List.of(buildItem(101L, "Laptop", 1, "999.99")));
        multiItemOrder = buildOrder(
                1002L,
                List.of(
                        buildItem(201L, "Keyboard", 2, "49.99"),
                        buildItem(202L, "Mouse", 1, "29.99")
                )
        );
    }

    @Test
    @DisplayName("Should confirm order when all saga steps succeed")
    void shouldConfirmOrderWhenAllSagaStepsSucceed() {
        when(inventoryClient.reserveStock(101L, 1, 1001L)).thenReturn(true);
        when(paymentProcessor.processPayment(singleItemOrder)).thenReturn(true);
        when(orderRepository.save(singleItemOrder)).thenReturn(singleItemOrder);

        orderSagaOrchestrator.executeOrderSaga(singleItemOrder);

        assertThat(singleItemOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(inventoryClient).reserveStock(101L, 1, 1001L);
        verify(paymentProcessor).processPayment(singleItemOrder);
        verify(orderRepository).save(singleItemOrder);
        verify(inventoryClient, never()).releaseStock(any(), any(), any());
    }

    @Test
    @DisplayName("Should compensate order and inventory when inventory reservation fails")
    void shouldCompensateWhenInventoryReservationFails() {
        when(inventoryClient.reserveStock(101L, 1, 1001L)).thenReturn(false);
        when(orderRepository.save(singleItemOrder)).thenReturn(singleItemOrder);

        orderSagaOrchestrator.executeOrderSaga(singleItemOrder);

        assertThat(singleItemOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentProcessor, never()).processPayment(any());
        verify(inventoryClient).releaseStock(101L, 1, 1001L);
        verify(orderRepository).save(singleItemOrder);
    }

    @Test
    @DisplayName("Should compensate inventory and order when payment fails")
    void shouldCompensateInventoryAndOrderWhenPaymentFails() {
        when(inventoryClient.reserveStock(101L, 1, 1001L)).thenReturn(true);
        when(paymentProcessor.processPayment(singleItemOrder)).thenReturn(false);
        when(orderRepository.save(singleItemOrder)).thenReturn(singleItemOrder);

        orderSagaOrchestrator.executeOrderSaga(singleItemOrder);

        assertThat(singleItemOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryClient).reserveStock(101L, 1, 1001L);
        verify(paymentProcessor).processPayment(singleItemOrder);
        verify(inventoryClient).releaseStock(101L, 1, 1001L);
        verify(orderRepository).save(singleItemOrder);
    }

    @Test
    @DisplayName("Should reserve and confirm all items in a multi-item order")
    void shouldHandleMultipleItemsSuccessfully() {
        when(inventoryClient.reserveStock(201L, 2, 1002L)).thenReturn(true);
        when(inventoryClient.reserveStock(202L, 1, 1002L)).thenReturn(true);
        when(paymentProcessor.processPayment(multiItemOrder)).thenReturn(true);
        when(orderRepository.save(multiItemOrder)).thenReturn(multiItemOrder);

        orderSagaOrchestrator.executeOrderSaga(multiItemOrder);

        assertThat(multiItemOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(inventoryClient).reserveStock(201L, 2, 1002L);
        verify(inventoryClient).reserveStock(202L, 1, 1002L);
        verify(paymentProcessor).processPayment(multiItemOrder);
        verify(orderRepository).save(multiItemOrder);
    }

    @Test
    @DisplayName("Should compensate all steps when an exception occurs")
    void shouldCompensateAllWhenExceptionOccurs() {
        when(inventoryClient.reserveStock(101L, 1, 1001L)).thenReturn(true);
        when(paymentProcessor.processPayment(singleItemOrder)).thenThrow(new RuntimeException("Payment gateway unavailable"));
        when(orderRepository.save(singleItemOrder)).thenReturn(singleItemOrder);

        orderSagaOrchestrator.executeOrderSaga(singleItemOrder);

        assertThat(singleItemOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryClient).reserveStock(101L, 1, 1001L);
        verify(paymentProcessor).processPayment(singleItemOrder);
        verify(inventoryClient, times(1)).releaseStock(101L, 1, 1001L);
        verify(orderRepository).save(singleItemOrder);
    }

    private Order buildOrder(Long orderId, List<OrderItem> items) {
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-TEST-" + orderId)
                .userId(55L)
                .shippingAddress("123 Test Street")
                .paymentMethod("CARD")
                .notes("Handle with care")
                .status(OrderStatus.PENDING)
                .build();
        items.forEach(order::addItem);
        order.calculateTotalAmount();
        return order;
    }

    private OrderItem buildItem(Long productId, String productName, Integer quantity, String unitPrice) {
        OrderItem item = OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(new BigDecimal(unitPrice))
                .build();
        item.calculateSubtotal();
        return item;
    }
}
