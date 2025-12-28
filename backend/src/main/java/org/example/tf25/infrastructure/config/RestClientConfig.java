package org.example.tf25.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class RestClientConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestClientConfig.class);

    @Bean
    public SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
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
                .requestInterceptor((request, body, execution) -> {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest incomingRequest = attributes.getRequest();
                        String authHeader = incomingRequest.getHeader("Authorization");
                        if (authHeader != null && !authHeader.isBlank()) {
                            log.info("Backend: propagando token hacia el Proxy para {} {}", request.getMethod(), request.getURI());
                            request.getHeaders().set("Authorization", authHeader);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
