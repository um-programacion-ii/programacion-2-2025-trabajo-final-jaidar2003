package um.tf2025.service.auth;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import um.tf2025.service.dto.auth.CatedraLoginRequest;
import um.tf2025.service.dto.auth.CatedraRegisterRequest;
import um.tf2025.service.dto.auth.CatedraTokenResponse;
import um.tf2025.service.dto.auth.TokenBundle;

@Profile("catedra")
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(value = "catedra.auth.mock", havingValue = "false", matchIfMissing = true)
public class AuthService implements AuthCatedraService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final AuthCatedraClient client;
  private final TokenStore store;

  public AuthService(AuthCatedraClient client, TokenStore store) {
    this.client = client;
    this.store = store;
  }

  @Override
  public TokenBundle register(CatedraRegisterRequest req) {
    CatedraTokenResponse tok = client.register(req);
    TokenBundle tb = toBundle(req.email(), tok);
    store.save(tb);
    return tb;
  }

  @Override
  public TokenBundle login(CatedraLoginRequest req) {
    CatedraTokenResponse tok = client.login(req);
    TokenBundle tb = toBundle(req.username(), tok);
    store.save(tb);
    return tb;
  }

  @Override
  public TokenBundle get(String username) {
    return store.get(username);
  }

  @Override
  public TokenBundle refresh(String username) {
    TokenBundle cur = store.get(username);
    if (cur == null || cur.refreshToken() == null) return null;
    CatedraTokenResponse tok = client.refresh(cur.refreshToken());
    TokenBundle tb = new TokenBundle(username, tok.accessToken(), tok.refreshToken(),
        Instant.now().plusSeconds(tok.expiresIn()));
    store.save(tb);
    return tb;
  }

  private TokenBundle toBundle(String username, CatedraTokenResponse t) {
    return new TokenBundle(
        username,
        t.accessToken(),
        t.refreshToken(),
        Instant.now().plusSeconds(t.expiresIn()));
  }

  // Auto-refresh cada minuto si falta < 120s
  @Scheduled(fixedDelay = 60000)
  public void autoRefresh() {
    TokenBundle admin = store.get("admin");
    if (admin != null && admin.isExpiringSoon()) {
      try {
        refresh("admin");
      } catch (Exception e) {
        log.warn("Auto-refresh falló", e);
      }
    }
  }
}
