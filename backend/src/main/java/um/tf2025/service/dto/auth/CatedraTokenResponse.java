package um.tf2025.service.dto.auth;

public record CatedraTokenResponse(String accessToken, String refreshToken, long expiresIn) {}
