package org.example.tf25.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
// No propagamos Authorization al Proxy; el Proxy usa su propio token hacia la cátedra

@Configuration
public class RestClientConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestClientConfig.class);

    @Bean
    public SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(30_000);
        return factory;
    }

    @Bean
    public RestClient restClient(
            RestClient.Builder builder,
            SimpleClientHttpRequestFactory factory,
            @Value("${tf25.proxy.base-url:http://localhost:8081}") String proxyBaseUrl
    ) {
        return builder
                .baseUrl(proxyBaseUrl)
                .requestFactory(factory)
                .build();
    }
}
