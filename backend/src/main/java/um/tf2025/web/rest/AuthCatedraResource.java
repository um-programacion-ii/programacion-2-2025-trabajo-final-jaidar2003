package um.tf2025.web.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import um.tf2025.service.auth.AuthService;
import um.tf2025.service.dto.auth.CatedraLoginRequest;
import um.tf2025.service.dto.auth.CatedraRegisterRequest;
import um.tf2025.service.dto.auth.TokenBundle;

@Profile("catedra")
@RestController
@RequestMapping("/api/catedra")
public class AuthCatedraResource {

  private final um.tf2025.service.auth.AuthCatedraService auth;
  public AuthCatedraResource(um.tf2025.service.auth.AuthCatedraService auth) { this.auth = auth; }

  @PostMapping("/register")
  public ResponseEntity<TokenBundle> register(@RequestBody CatedraRegisterRequest req) {
    return ResponseEntity.ok(auth.register(req));
  }

  @PostMapping("/login")
  public ResponseEntity<TokenBundle> login(@RequestBody CatedraLoginRequest req) {
    return ResponseEntity.ok(auth.login(req));
  }

  @PostMapping("/refresh/{username}")
  public ResponseEntity<TokenBundle> refresh(@PathVariable String username) {
    var tb = auth.refresh(username);
    return tb == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(tb);
  }

  @GetMapping("/token/{username}")
  public ResponseEntity<TokenBundle> get(@PathVariable String username) {
    var tb = auth.get(username);
    return tb == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(tb);
  }
}
