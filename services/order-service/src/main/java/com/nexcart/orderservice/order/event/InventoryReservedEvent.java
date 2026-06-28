package com.nexcart.orderservice.order.event;

import com.nexcart.orderservice.common.event.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class InventoryReservedEvent extends DomainEvent {
    
    private Long orderId;
    private Long reservationId;
    private List<ReservedItemDto> items;
    
    public InventoryReservedEvent(Long orderId, Long reservationId, List<ReservedItemDto> items) {
        super("InventoryReserved");
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.items = items;
    }
    
    @Data
    @NoArgsConstructor
    public static class ReservedItemDto {
        private Long productId;
        private Integer quantity;
        
        public ReservedItemDto(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}
