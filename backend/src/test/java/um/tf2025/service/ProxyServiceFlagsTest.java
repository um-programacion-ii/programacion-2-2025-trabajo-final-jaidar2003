package um.tf2025.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "proxy.kafka.enabled=false",
    "proxy.redis.enabled=false",
    "spring.liquibase.enabled=false"
})
class ProxyServiceFlagsTest {

    @Autowired
    private ProxyService proxyService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    void publishKafka_when_disabled_returns_reason_and_never_uses_template() {
        Map<String, Object> res = proxyService.publishKafka("k1", "v1");
        assertThat(res.get("sent")).isEqualTo(false);
        assertThat(res.get("reason")).isIn("kafka-disabled", "kafka-disabled-dev");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void cache_when_redis_disabled_uses_local_backend() {
        Map<String, Object> put = proxyService.putCache("x1", "hola");
        assertThat(put.get("cached")).isEqualTo(true);
        assertThat(put.get("backend")).isIn("local", "none");

        Map<String, Object> get = proxyService.getCache("x1");
        assertThat(get.get("hit")).isIn(true, Boolean.TRUE, "true");
        assertThat(get.get("value")).isEqualTo("hola");
        assertThat(get.get("backend")).isIn("local", "none");
    }
}
