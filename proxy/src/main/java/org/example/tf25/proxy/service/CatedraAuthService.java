package org.example.tf25.proxy.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.tf25.proxy.config.CatedraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CatedraAuthService {

    private static final Logger log = LoggerFactory.getLogger(CatedraAuthService.class);

    private final CatedraProperties properties;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();

    public CatedraAuthService(CatedraProperties properties) {
        this.properties = properties;
    }

    /**
     * Devuelve un token válido desde cache o realizando login.
     */
    public String getBearerToken() {
        // 1) Si hay token estático configurado, úsalo y evitá login
        if (properties.getToken() != null && StringUtils.hasText(properties.getToken())) {
            return properties.getToken().trim();
        }
        // 1.b) Si hay archivo de token configurado y con contenido, úsalo
        String fileToken = getTokenFromFileIfConfigured();
        if (StringUtils.hasText(fileToken)) {
            return fileToken;
        }
        // 2) Caso contrario, usar token cacheado o loguear
        String token = cachedToken.get();
        if (!StringUtils.hasText(token)) {
            synchronized (this) {
                token = cachedToken.get();
                if (!StringUtils.hasText(token)) {
                    token = doLogin();
                    cachedToken.set(token);
                }
            }
        }
        return token;
    }

    /**
     * Fuerza relogin e invalida el token cacheado.
     */
    public String refreshToken() {
        // Si hay token estático, simplemente lo devolvemos
        if (properties.getToken() != null && StringUtils.hasText(properties.getToken())) {
            return properties.getToken().trim();
        }
        // Si hay archivo de token, recargar desde allí
        String fileToken = getTokenFromFileIfConfigured();
        if (StringUtils.hasText(fileToken)) {
            cachedToken.set(fileToken);
            return fileToken;
        }
        synchronized (this) {
            String token = doLogin();
            cachedToken.set(token);
            return token;
        }
    }

    private String getTokenFromFileIfConfigured() {
        try {
            String tokenFile = properties.getTokenFile();
            if (!StringUtils.hasText(tokenFile)) return null;
            Path path = Path.of(tokenFile);
            if (!Files.exists(path)) return null;
            String content = Files.readString(path);
            if (!StringUtils.hasText(content)) return null;
            // Tomar la primera línea no vacía y limpiar espacios/quotes
            String first = java.util.Arrays.stream(content.split("\r?\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .findFirst().orElse("");
            if (!StringUtils.hasText(first)) return null;
            if ((first.startsWith("\"") && first.endsWith("\"")) || (first.startsWith("'") && first.endsWith("'"))) {
                first = first.substring(1, first.length() - 1).trim();
            }
            return first;
        } catch (Exception e) {
            log.debug("Proxy: no se pudo leer token desde archivo configurado: {}", e.toString());
            return null;
        }
    }

    private String doLogin() throws RestClientException, IllegalStateException {
        if (properties.getAuth() == null || !StringUtils.hasText(properties.getAuth().getLoginPath())) {
            throw new IllegalStateException("tf25.catedra.auth.login-path no configurado");
        }
        if (!StringUtils.hasText(properties.getAuth().getUsername()) || !StringUtils.hasText(properties.getAuth().getPassword())) {
            throw new IllegalStateException("Credenciales de cátedra no configuradas (username/password)");
        }

        String baseUrl = properties.getBaseUrl();
        String loginPath = properties.getAuth().getLoginPath();

        // Log claro según sea path relativo o absoluto
        if (loginPath != null && loginPath.startsWith("http")) {
            log.info("Proxy: realizando login contra cátedra en {}", loginPath);
        } else {
            log.info("Proxy: realizando login contra cátedra en {} + {}", baseUrl, loginPath);
        }

        // RestClient local para evitar dependencia circular con el bean compartido
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        JsonNode response = restClient.post()
                // Usamos solo el path; si viene absoluto, RestClient lo respeta igualmente
                .uri(loginPath)
                .body(new org.example.tf25.proxy.dto.catedra.CatedraLoginRequest(
                        properties.getAuth().getUsername(),
                        properties.getAuth().getPassword()
                ))
                .retrieve()
                .body(JsonNode.class);

        String token = extractToken(response);
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("No se pudo obtener token JWT de la cátedra (revisar nombre de campo en la respuesta)");
        }
        log.info("Proxy: login cátedra OK, token obtenido");
        return token;
    }

    private String extractToken(JsonNode node) {
        if (node == null) return null;
        // Intentar distintos nombres comunes
        String[] fields = new String[]{"id_token", "token", "access_token", "accessToken", "jwt"};
        for (String f : fields) {
            if (node.hasNonNull(f)) {
                String v = node.get(f).asText();
                if (StringUtils.hasText(v)) return v;
            }
        }
        // Si viniera anidado (p.ej. { data: { token: "..." } })
        if (node.has("data")) {
            JsonNode data = node.get("data");
            for (String f : fields) {
                if (data.hasNonNull(f)) {
                    String v = data.get(f).asText();
                    if (StringUtils.hasText(v)) return v;
                }
            }
        }
        return null;
    }
}
