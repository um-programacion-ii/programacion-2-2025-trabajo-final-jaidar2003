package um.tf2025.service.auth;

import um.tf2025.service.dto.auth.TokenBundle;

public interface TokenStore {
  void save(TokenBundle t);
  TokenBundle get(String username);
  void clear(String username);
}
