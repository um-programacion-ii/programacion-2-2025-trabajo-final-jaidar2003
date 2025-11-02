package um.tf2025.web.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import um.tf2025.service.ProxyService;

import java.util.Map;

@RestController
@RequestMapping("/api/proxy")
public class ProxyResource {

    private final ProxyService proxyService;

    public ProxyResource(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping("/kafka")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> publish(@RequestBody Map<String, String> body) {
        String key = body.getOrDefault("key", "default");
        String value = body.getOrDefault("value", "");
        return ResponseEntity.ok(proxyService.publishKafka(key, value));
    }

    @PostMapping("/cache")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> cacheSet(@RequestBody Map<String, String> body) {
        String key = body.getOrDefault("key", "k");
        String value = body.getOrDefault("value", "");
        return ResponseEntity.ok(proxyService.putCache(key, value));
    }

    @GetMapping("/cache/{key}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> cacheGet(@PathVariable String key) {
        return ResponseEntity.ok(proxyService.getCache(key));
    }
}
