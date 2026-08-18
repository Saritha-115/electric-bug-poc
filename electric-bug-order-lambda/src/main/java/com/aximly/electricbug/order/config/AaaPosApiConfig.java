package com.aximly.electricbug.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(name = "externalApiFlag", havingValue = "true")
public class AaaPosApiConfig {

    @Value("${aaapos.api.base-url}")
    private String baseUrl;

    @Value("${aaapos.api.key}")
    private String apiKey;

    @Bean
    public WebClient aaaPosWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}