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

    // Importante: usar el nombre de campo exacto "id_token" que espera el cliente mobile
    public static final class LoginResponse {
        @JsonProperty("id_token")
        public String idToken;
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
            return restClient.post()
                    .uri(authUrl)
                    .body(req)
                    .retrieve()
                    .toEntity(Object.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al conectar con el servicio de autenticación");
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
