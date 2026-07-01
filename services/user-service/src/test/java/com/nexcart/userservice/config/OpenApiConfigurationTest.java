package com.nexcart.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigurationTest {
    private final OpenApiConfiguration configuration = new OpenApiConfiguration();

    @Test
    void shouldCreateOpenApiBean() {

        OpenAPI api = configuration.userServiceApi();

        assertThat(api).isNotNull();

        assertThat(api.getInfo().getTitle())
            .isEqualTo("NexCart User Service API");

        assertThat(api.getInfo().getDescription())
            .isEqualTo("REST APIs for managing users");

        assertThat(api.getInfo().getVersion())
            .isEqualTo("v1.0");

        assertThat(api.getInfo().getContact().getName())
            .isEqualTo("Nithya Mukundan");

        assertThat(api.getExternalDocs().getDescription())
            .isEqualTo("GitHub Repository");

        assertThat(api.getExternalDocs().getUrl())
            .isEqualTo("https://github.com/nithya-tw/nexcart");
    }
}
