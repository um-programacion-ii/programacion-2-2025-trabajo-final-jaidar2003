package um.tf2025.service.auth;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import um.tf2025.service.dto.auth.TokenBundle;

@Component
public class LocalTokenStore implements TokenStore {
  private final ConcurrentHashMap<String, TokenBundle> map = new ConcurrentHashMap<>();
  public void save(TokenBundle t) { map.put(t.username(), t); }
  public TokenBundle get(String username) { return map.get(username); }
  public void clear(String username) { map.remove(username); }
}
