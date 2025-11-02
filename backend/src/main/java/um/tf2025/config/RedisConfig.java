package um.tf2025.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "proxy.redis", name = "enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(ProxyProperties p) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(p.getRedis().getHost(), p.getRedis().getPort());
        cfg.setDatabase(p.getRedis().getDatabase());
        return new LettuceConnectionFactory(cfg);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}
