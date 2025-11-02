package um.tf2025.service.auth;

import um.tf2025.service.dto.auth.CatedraLoginRequest;
import um.tf2025.service.dto.auth.CatedraRegisterRequest;
import um.tf2025.service.dto.auth.TokenBundle;

/**
 * Abstraction for Cátedra authentication integration.
 * Implementations can target the real upstream or provide a local mock based on property flags.
 */
public interface AuthCatedraService {
    TokenBundle register(CatedraRegisterRequest req);
    TokenBundle login(CatedraLoginRequest req);
    TokenBundle get(String username);
    TokenBundle refresh(String username);
}
