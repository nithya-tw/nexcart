package com.nexcart.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for RestClient used to call other microservices.
 */
@Configuration
public class RestClientConfig {
    
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .build();
    }
}
