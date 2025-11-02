package um.tf2025.service.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
@Primary
@ConditionalOnProperty(name = "proxy.redis.enabled", havingValue = "false", matchIfMissing = true)
public class LocalCacheService implements SimpleCache {

    private final Cache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(5_000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();

    @Override
    public boolean put(String key, String value) {
        cache.put(key, value);
        return true;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    @Override
    public String backendName() {
        return "local";
    }
}
