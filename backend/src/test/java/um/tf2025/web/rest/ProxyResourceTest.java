package um.tf2025.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import um.tf2025.service.ProxyService;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@WebMvcTest(controllers = ProxyResource.class)
class ProxyResourceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @MockBean
    private ProxyService proxyService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void kafka_disabled_returns_sent_false() throws Exception {
        when(proxyService.publishKafka(any(), any()))
            .thenReturn(Map.of("sent", false, "reason", "kafka-disabled"));

        mvc.perform(post("/api/proxy/kafka")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("key","k1","value","hola"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sent").value(false))
            .andExpect(jsonPath("$.reason").value("kafka-disabled"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void cache_put_then_ok_response() throws Exception {
        when(proxyService.putCache(any(), any()))
            .thenReturn(Map.of("cached", true, "backend", "local"));

        mvc.perform(post("/api/proxy/cache")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("key","x1","value","hola"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cached").value(true))
            .andExpect(jsonPath("$.backend").value("local"));
    }
}
