package um.tf2025.service.auth;

import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import um.tf2025.service.dto.auth.CatedraLoginRequest;
import um.tf2025.service.dto.auth.CatedraRegisterRequest;
import um.tf2025.service.dto.auth.TokenBundle;

@Profile("catedra")
@Service
@ConditionalOnProperty(value = "catedra.auth.mock", havingValue = "true")
public class MockAuthCatedraService implements AuthCatedraService {

    private final TokenStore store;

    public MockAuthCatedraService(TokenStore store) {
        this.store = store;
    }

    @Override
    public TokenBundle register(CatedraRegisterRequest req) {
        TokenBundle tb = new TokenBundle(
            req.email(),
            "mock-token-register-" + req.email(),
            "mock-refresh-register-" + req.email(),
            Instant.now().plusSeconds(3600)
        );
        store.save(tb);
        return tb;
    }

    @Override
    public TokenBundle login(CatedraLoginRequest req) {
        TokenBundle tb = new TokenBundle(
            req.username(),
            "mock-token-login-" + req.username(),
            "mock-refresh-login-" + req.username(),
            Instant.now().plusSeconds(3600)
        );
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
        if (cur == null) return null;
        TokenBundle tb = new TokenBundle(
            username,
            "mock-token-refresh-" + username + "-" + System.currentTimeMillis(),
            cur.refreshToken(),
            Instant.now().plusSeconds(3600)
        );
        store.save(tb);
        return tb;
    }
}
