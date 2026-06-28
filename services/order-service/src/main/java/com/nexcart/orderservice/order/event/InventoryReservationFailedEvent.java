package com.nexcart.orderservice.order.event;

import com.nexcart.orderservice.common.event.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class InventoryReservationFailedEvent extends DomainEvent {
    
    private Long orderId;
    private String reason;
    
    public InventoryReservationFailedEvent(Long orderId, String reason) {
        super("InventoryReservationFailed");
        this.orderId = orderId;
        this.reason = reason;
    }
}
