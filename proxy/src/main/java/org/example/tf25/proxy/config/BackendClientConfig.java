package org.example.tf25.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BackendClientConfig {

    @Bean
    public RestClient backendRestClient(
            RestClient.Builder builder,
            @Value("${tf25.backend.base-url:http://localhost:8080}") String backendBaseUrl
    ) {
        return builder.baseUrl(backendBaseUrl).build();
    }
}
