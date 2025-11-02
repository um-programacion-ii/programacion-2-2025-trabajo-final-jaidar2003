package um.tf2025.service;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import um.tf2025.config.ProxyProperties;
import um.tf2025.service.cache.SimpleCache;

@Service
public class ProxyService {

    private final ProxyProperties props;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private SimpleCache localCacheService;

    public ProxyService(ProxyProperties props) {
        this.props = props;
    }

    // New naming as per tests
    public Map<String, Object> publishKafka(String key, String value) {
        if (!props.getKafka().isEnabled() || kafkaTemplate == null) {
            return Map.of("sent", false, "reason", "kafka-disabled");
        }
        kafkaTemplate.send(props.getKafka().getTopic(), key, value);
        return Map.of("sent", true);
    }

    public Map<String, Object> putCache(String key, String value) {
        if (!props.getRedis().isEnabled() || redisTemplate == null) {
            if (localCacheService != null) {
                localCacheService.put(key, value);
                return Map.of("cached", true, "backend", localCacheService.backendName());
            }
            // Fallback safety net if no bean present
            return Map.of("cached", false, "backend", "none");
        }
        redisTemplate.opsForValue().set(key, value);
        return Map.of("cached", true, "backend", "redis");
    }

    public Map<String, Object> getCache(String key) {
        if (!props.getRedis().isEnabled() || redisTemplate == null) {
            if (localCacheService != null) {
                Optional<String> v = localCacheService.get(key);
                return Map.of("hit", v.isPresent(), "value", v.orElse(null), "backend", localCacheService.backendName());
            }
            return Map.of("hit", false, "value", null, "backend", "none");
        }
        String v = redisTemplate.opsForValue().get(key);
        return Map.of("hit", v != null, "value", v, "backend", "redis");
    }

    // Backward-compatible methods (delegating)
    public Map<String, Object> sendToKafka(String key, String value) { return publishKafka(key, value); }
    public Map<String, Object> cachePut(String key, String value) { return putCache(key, value); }
    public Map<String, Object> cacheGet(String key) { return getCache(key); }
}
