package com.nexcart.orderservice.order.entity;

import com.nexcart.orderservice.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "failed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedEvent extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String eventId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private String topic;
    
    @Column(nullable = false)
    private Integer partition;
    
    @Column(nullable = false)
    private Long offset;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(nullable = false)
    private Integer retryCount = 0;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FailureStatus status = FailureStatus.FAILED;
    
    public enum FailureStatus {
        FAILED,
        RETRYING,
        RESOLVED,
        PERMANENTLY_FAILED
    }
}
