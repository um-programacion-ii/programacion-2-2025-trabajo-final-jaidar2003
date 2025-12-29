package org.example.tf25.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    private final RestClient restClient;
    private final String authUrl;
    private final String registerUrl;

    public AuthController(RestClient.Builder builder,
                          SimpleClientHttpRequestFactory factory,
                          @Value("${tf25.catedra.auth-url}") String authUrl,
                          @Value("${tf25.catedra.register-url}") String registerUrl) {
        // Usamos un builder limpio y el factory con timeouts para evitar cuelgues
        this.restClient = builder.requestFactory(factory).build();
        this.authUrl = authUrl;
        this.registerUrl = registerUrl;
    }

    public record LoginRequest(String username, String password, Boolean rememberMe) {}

    public record RegisterRequest(
            @JsonProperty("login") @JsonAlias("username") String username,
            String password,
            String firstName,
            String lastName,
            String email,
            String nombreAlumno,
            String descripcionProyecto
    ) {}

    public static final class LoginResponse {
        @JsonProperty("id_token")
        public String idToken;
        public LoginResponse() {}
        public LoginResponse(String idToken) { this.idToken = idToken; }
    }

    public record RegisterResponse(
            boolean creado,
            String resultado,
            String token
    ) {}

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest req) {
        log.info("Intento de login para usuario: {}", req.username());
        try {
            log.debug("Llamando a la cátedra para login en: {}", authUrl);
            // Llamamos a la cátedra y obtenemos la respuesta como un Map
            Map<String, Object> response = restClient.post()
                    .uri(authUrl)
                    .header("Connection", "close")
                    .body(req)
                    .retrieve()
                    .body(Map.class);

            log.debug("Respuesta recibida de la cátedra para login: {}", response);

            if (response != null && (response.containsKey("id_token") || response.containsKey("token"))) {
                String token = (String) response.getOrDefault("id_token", response.get("token"));
                log.info("Login exitoso para usuario: {}", req.username());
                return ResponseEntity.ok(new LoginResponse(token));
            }
            
            log.warn("Respuesta de autenticación inválida de la cátedra para usuario: {}", req.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Respuesta de autenticación inválida"));
            
        } catch (RestClientResponseException e) {
            log.error("Error de la cátedra al autenticar usuario {}: Status {}, Body {}", 
                    req.username(), e.getStatusCode(), e.getResponseBodyAsString());
            // Envolvemos el error en JSON para que el cliente mobile no falle al parsear
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getResponseBodyAsString()));
        }
    }

    @PostMapping("/agregar_usuario")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        log.info("Intento de registro para usuario: {}, email: {}", req.username(), req.email());
        try {
            log.debug("Llamando a la cátedra para registro en: {}", registerUrl);
            
            // Leemos como String primero para ser resilientes a errores de parsing
            ResponseEntity<String> response = restClient.post()
                    .uri(registerUrl)
                    .header("Connection", "close")
                    .body(req)
                    .retrieve()
                    .toEntity(String.class);
            
            log.info("Respuesta de registro recibida de la cátedra para usuario {}: Status {}", 
                    req.username(), response.getStatusCode());
            log.debug("Cuerpo de respuesta de registro: {}", response.getBody());
            
            boolean ok = response.getStatusCode().is2xxSuccessful();
            return ResponseEntity.status(ok ? HttpStatus.OK : response.getStatusCode()).body(new RegisterResponse(
                    ok,
                    ok ? "Usuario creado con éxito" : response.getBody(),
                    null
            ));
            
        } catch (RestClientResponseException e) {
            log.error("Error de la cátedra al registrar usuario {}: Status {}, Body {}", 
                    req.username(), e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(new RegisterResponse(
                    false,
                    "Error de la cátedra: " + e.getResponseBodyAsString(),
                    null
            ));
        } catch (Exception e) {
            log.error("Error inesperado al registrar usuario {}: {}", req.username(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new RegisterResponse(
                    false,
                    "Error interno al procesar el registro: " + e.getMessage(),
                    null
            ));
        }
    }
}
