package com.nexcart.orderservice;

import com.nexcart.orderservice.inventory.client.InventoryClient;
import com.nexcart.orderservice.order.repository.OrderItemRepository;
import com.nexcart.orderservice.order.repository.OrderRepository;
import com.nexcart.orderservice.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.task.scheduling.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private OrderItemRepository orderItemRepository;

    @MockBean
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private RestClient restClient;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void contextLoads() {
    }
}
