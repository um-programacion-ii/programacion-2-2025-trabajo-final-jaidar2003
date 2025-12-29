package org.example.tf25.proxy.config;

import org.example.tf25.proxy.service.CatedraAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
// Eliminamos dependencia de Authorization entrante; el Proxy usará su propio token

@Configuration
public class CatedraClientConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatedraClientConfig.class);

    @Bean
    public RestClient catedraRestClient(
            RestClient.Builder builder,
            @Value("${tf25.catedra.base-url}") String baseUrl,
            CatedraAuthService authService,
            CatedraProperties catedraProperties
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);

        return builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    // Usar SIEMPRE el token del Proxy para hablar con cátedra (no propagar Authorization entrante)
                    // Evitar añadir Authorization al endpoint de login para no generar recursión
                    String path = request.getURI().getPath();
                    String loginPath = catedraProperties.getAuth() != null ? catedraProperties.getAuth().getLoginPath() : null;
                    boolean isLoginCall = (loginPath != null && !loginPath.isBlank()) && path != null && path.startsWith(loginPath);

                    if (!isLoginCall) {
                        try {
                            String token = authService.getBearerToken();
                            if (token != null && !token.isBlank()) {
                                request.getHeaders().setBearerAuth(token);
                            }
                        } catch (Exception ignored) {
                            // Si no hay token configurado aún, dejamos pasar; el servicio puede manejar el 401
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
