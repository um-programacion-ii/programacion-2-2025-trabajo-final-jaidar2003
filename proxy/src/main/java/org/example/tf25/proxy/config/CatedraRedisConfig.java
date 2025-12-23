package org.example.tf25.proxy.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(CatedraRedisProperties.class)
public class CatedraRedisConfig {

    @Bean(name = "catedraRedisConnectionFactory")
    public LettuceConnectionFactory catedraRedisConnectionFactory(CatedraRedisProperties props) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(props.host(), props.port());
        return new LettuceConnectionFactory(cfg);
    }

    @Bean(name = "catedraRedisTemplate")
    public StringRedisTemplate catedraRedisTemplate(
            @Qualifier("catedraRedisConnectionFactory") RedisConnectionFactory connectionFactory
    ) {
        return new StringRedisTemplate(connectionFactory);
    }
}
