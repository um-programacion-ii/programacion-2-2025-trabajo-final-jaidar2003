package um.tf2025.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import um.tf2025.service.auth.AuthService;
import um.tf2025.service.dto.auth.TokenBundle;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ActiveProfiles("catedra")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthCatedraResource.class)
class AuthCatedraResourceTest {

  @Autowired MockMvc mvc;
  @MockBean um.tf2025.service.auth.AuthCatedraService auth;
  @MockBean JwtDecoder jwtDecoder;

  @Test
  void login_ok() throws Exception {
    var tb = new TokenBundle("admin", "acc", "ref", Instant.now().plusSeconds(3600));
    when(auth.login(any())).thenReturn(tb);

    mvc.perform(post("/api/catedra/login")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.username").value("admin"))
      .andExpect(jsonPath("$.accessToken").value("acc"));
  }
}
