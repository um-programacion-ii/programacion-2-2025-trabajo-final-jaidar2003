package um.tf2025.service.cache;

import java.util.Optional;

public interface SimpleCache {
    boolean put(String key, String value);
    Optional<String> get(String key);
    String backendName();
}
