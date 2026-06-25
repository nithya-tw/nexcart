package com.nexcart.userservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI userServiceApi() {

        return new OpenAPI()

            .info(new Info()
                .title("NexCart User Service API")
                .description("REST APIs for managing users")
                .version("v1.0")
                .contact(new Contact()
                    .name("Nithya Mukundan")))

            .externalDocs(new ExternalDocumentation()
                .description("GitHub Repository")
                .url("https://github.com/nithya-tw/nexcart"));
    }
}
