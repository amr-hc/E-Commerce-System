package com.intelligent.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OllamaConfig {

    @Bean
    public RestClient ollamaRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

}
