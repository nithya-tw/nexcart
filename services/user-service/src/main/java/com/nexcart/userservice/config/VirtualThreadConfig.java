package com.nexcart.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class VirtualThreadConfig {

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        log.info("Configuring Tomcat to use Virtual Threads for User Service");
        return (ProtocolHandler protocolHandler) -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            log.info("Virtual Threads enabled for HTTP request handling");
        };
    }
}
