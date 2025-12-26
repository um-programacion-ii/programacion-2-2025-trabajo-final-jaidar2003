package org.example.tf25.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final RestClient restClient;
    private final String authUrl;
    private final String registerUrl;

    public AuthController(RestClient.Builder builder,
                          @Value("${tf25.catedra.auth-url}") String authUrl,
                          @Value("${tf25.catedra.register-url}") String registerUrl) {
        this.restClient = builder.build();
        this.authUrl = authUrl;
        this.registerUrl = registerUrl;
    }

    public record LoginRequest(String username, String password, Boolean rememberMe) {}

    public record RegisterRequest(
            String username,
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
        try {
            // Llamamos a la cátedra y obtenemos la respuesta como un Map
            Map<String, Object> response = restClient.post()
                    .uri(authUrl)
                    .body(req)
                    .retrieve()
                    .body(Map.class);

            if (response != null && (response.containsKey("id_token") || response.containsKey("token"))) {
                String token = (String) response.getOrDefault("id_token", response.get("token"));
                return ResponseEntity.ok(new LoginResponse(token));
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Respuesta de autenticación inválida");
            
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace(); // Para ver el error real en los logs de docker
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al conectar con el servicio de autenticación: " + e.getMessage());
        }
    }

    @PostMapping("/agregar_usuario")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return restClient.post()
                    .uri(registerUrl)
                    .body(req)
                    .retrieve()
                    .toEntity(RegisterResponse.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al conectar con el servicio de registro");
        }
    }
}
