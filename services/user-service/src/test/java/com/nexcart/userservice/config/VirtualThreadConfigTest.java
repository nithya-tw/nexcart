package com.nexcart.userservice.config;

import org.apache.coyote.ProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VirtualThreadConfig.
 * 
 * Tests verify that Virtual Threads are properly configured for Tomcat's HTTP request handling.
 * Virtual Threads enable massive concurrency with minimal memory overhead (~1KB per thread vs ~1MB).
 */
@DisplayName("VirtualThreadConfig Tests")
class VirtualThreadConfigTest {

    private VirtualThreadConfig config;

    @BeforeEach
    void setUp() {
        config = new VirtualThreadConfig();
    }

    @Test
    @DisplayName("Should create TomcatProtocolHandlerCustomizer bean")
    void shouldCreateCustomizerBean() {
        // When
        TomcatProtocolHandlerCustomizer<?> customizer = 
            config.protocolHandlerVirtualThreadExecutorCustomizer();

        // Then
        assertThat(customizer).isNotNull();
    }

    @Test
    @DisplayName("Should set virtual thread executor on protocol handler")
    void shouldSetVirtualThreadExecutor() {
        // Given
        ProtocolHandler protocolHandler = mock(ProtocolHandler.class);
        TomcatProtocolHandlerCustomizer<ProtocolHandler> customizer = 
            (TomcatProtocolHandlerCustomizer<ProtocolHandler>) 
            config.protocolHandlerVirtualThreadExecutorCustomizer();

        // When
        customizer.customize(protocolHandler);

        // Then
        verify(protocolHandler, times(1)).setExecutor(any(Executor.class));
    }

    @Test
    @DisplayName("Should use VirtualThreadPerTaskExecutor")
    void shouldUseVirtualThreadPerTaskExecutor() throws InterruptedException {
        // Given
        TomcatProtocolHandlerCustomizer<ProtocolHandler> customizer = 
            (TomcatProtocolHandlerCustomizer<ProtocolHandler>)
            config.protocolHandlerVirtualThreadExecutorCustomizer();
        
        // Create a mock protocol handler that captures the executor
        final Executor[] capturedExecutor = new Executor[1];
        ProtocolHandler protocolHandler = mock(ProtocolHandler.class);
        doAnswer(invocation -> {
            capturedExecutor[0] = invocation.getArgument(0);
            return null;
        }).when(protocolHandler).setExecutor(any(Executor.class));

        // When
        customizer.customize(protocolHandler);

        // Then - Verify executor is not null
        assertThat(capturedExecutor[0]).isNotNull();
        
        // Verify it's a virtual thread executor by checking thread properties
        final Thread[] executedThread = new Thread[1];
        capturedExecutor[0].execute(() -> {
            executedThread[0] = Thread.currentThread();
        });
        
        // Wait for thread to execute
        Thread.sleep(100);
        
        // Virtual threads are identifiable by their class name
        assertThat(executedThread[0]).isNotNull();
        assertThat(executedThread[0].toString()).contains("VirtualThread");
    }

    @Test
    @DisplayName("Should handle multiple customizations without errors")
    void shouldHandleMultipleCustomizations() {
        // Given
        TomcatProtocolHandlerCustomizer<ProtocolHandler> customizer = 
            (TomcatProtocolHandlerCustomizer<ProtocolHandler>)
            config.protocolHandlerVirtualThreadExecutorCustomizer();
        ProtocolHandler handler1 = mock(ProtocolHandler.class);
        ProtocolHandler handler2 = mock(ProtocolHandler.class);

        // When - customize multiple handlers
        customizer.customize(handler1);
        customizer.customize(handler2);

        // Then - both should be configured
        verify(handler1, times(1)).setExecutor(any(Executor.class));
        verify(handler2, times(1)).setExecutor(any(Executor.class));
    }

    @Test
    @DisplayName("Should demonstrate virtual thread characteristics")
    void shouldDemonstrateVirtualThreadCharacteristics() throws InterruptedException {
        // Given - Create virtual thread executor
        Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        // When - Execute multiple tasks
        final int[] completedTasks = {0};
        for (int i = 0; i < 1000; i++) {
            virtualExecutor.execute(() -> {
                synchronized (completedTasks) {
                    completedTasks[0]++;
                }
            });
        }
        
        // Wait for all tasks to complete
        Thread.sleep(500);
        
        // Then - All tasks should complete successfully
        // Virtual threads can handle thousands of concurrent tasks
        assertThat(completedTasks[0]).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should create configuration as Spring bean")
    void shouldBeSpringConfiguration() {
        // Then
        assertThat(VirtualThreadConfig.class.isAnnotationPresent(Configuration.class))
            .isTrue();
    }

    @Test
    @DisplayName("Should have customizer method marked as Bean")
    void shouldHaveCustomizerAsBean() throws NoSuchMethodException {
        // Given
        var method = VirtualThreadConfig.class
            .getMethod("protocolHandlerVirtualThreadExecutorCustomizer");

        // Then
        assertThat(method.isAnnotationPresent(Bean.class))
            .isTrue();
    }

    @Test
    @DisplayName("Should demonstrate memory efficiency of virtual threads")
    void shouldDemonstrateMemoryEfficiency() throws InterruptedException {
        // Given - Virtual thread executor
        Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        // When - Create many threads (would be impossible with platform threads)
        final int threadCount = 10_000;
        final int[] activeCount = {0};
        
        for (int i = 0; i < threadCount; i++) {
            virtualExecutor.execute(() -> {
                synchronized (activeCount) {
                    activeCount[0]++;
                }
                try {
                    Thread.sleep(10); // Simulate some work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Wait for threads to start
        Thread.sleep(200);
        
        // Then - Many threads should have started
        // (With platform threads, you'd hit limits around 200-500)
        assertThat(activeCount[0]).isGreaterThan(100);
    }
}
