package um.tf2025.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.context.annotation.Profile;
import um.tf2025.service.dto.auth.CatedraLoginRequest;
import um.tf2025.service.dto.auth.CatedraRegisterRequest;
import um.tf2025.service.dto.auth.CatedraTokenResponse;

@Profile("catedra")
@Component
public class AuthCatedraClient {

  private final WebClient http;
  private final String pRegister, pLogin, pRefresh;

  public AuthCatedraClient(
      @Value("${catedra.auth.base-url}") String baseUrl,
      @Value("${catedra.auth.endpoints.register}") String pRegister,
      @Value("${catedra.auth.endpoints.login}") String pLogin,
      @Value("${catedra.auth.endpoints.refresh}") String pRefresh) {

    this.http = WebClient.builder()
        .baseUrl(baseUrl)
        .build();

    this.pRegister = pRegister;
    this.pLogin = pLogin;
    this.pRefresh = pRefresh;
  }

  public CatedraTokenResponse register(CatedraRegisterRequest req) {
    return http.post().uri(pRegister)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve()
        .bodyToMono(CatedraTokenResponse.class)
        .block();
  }

  public CatedraTokenResponse login(CatedraLoginRequest req) {
    return http.post().uri(pLogin)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve()
        .bodyToMono(CatedraTokenResponse.class)
        .block();
  }

  public CatedraTokenResponse refresh(String refreshToken) {
    record RefreshReq(String refreshToken) {}
    return http.post().uri(pRefresh)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new RefreshReq(refreshToken))
        .retrieve()
        .bodyToMono(CatedraTokenResponse.class)
        .block();
  }
}
