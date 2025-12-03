package org.example.tf25.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CatedraClientConfig {

    @Bean
    public RestClient catedraRestClient(
            RestClient.Builder builder,
            @Value("${tf25.catedra.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
