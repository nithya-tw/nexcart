package com.nexcart.inventoryservice.inventory.event;

import com.nexcart.inventoryservice.common.event.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderPlacedEvent extends DomainEvent {
    
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    
    public OrderPlacedEvent(Long orderId, String orderNumber, Long userId, 
                           List<OrderItemDto> items, BigDecimal totalAmount) {
        super("OrderPlaced");
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
    }
    
    @Data
    @NoArgsConstructor
    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
        private BigDecimal price;
        
        public OrderItemDto(Long productId, Integer quantity, BigDecimal price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
