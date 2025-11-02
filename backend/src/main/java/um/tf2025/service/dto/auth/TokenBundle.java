package um.tf2025.service.dto.auth;

import java.time.Instant;

public record TokenBundle(String username, String accessToken, String refreshToken, Instant expiresAt) {
  public boolean isExpiringSoon() { return Instant.now().isAfter(expiresAt.minusSeconds(120)); }
}
