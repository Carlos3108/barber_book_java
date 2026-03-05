package org.azdev.barber_book.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Barber Book API")
                        .version("1.0.0")
                        .description("API de agendamento para barbearia")
                        .contact(new Contact()
                                .name("AzDev")
                                .url("https://barber_book.com")));
    }
}
