package com.transitassist.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transitAssistOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TransitAssist API")
                .version("1.0.0")
                .description("Accessible Transit Assistance Booking - request, assign and fulfil transit assistance."));
    }
}
