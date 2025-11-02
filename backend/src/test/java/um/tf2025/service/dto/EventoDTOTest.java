package um.tf2025.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import um.tf2025.web.rest.TestUtil;

class EventoDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EventoDTO.class);
        EventoDTO eventoDTO1 = new EventoDTO();
        eventoDTO1.setId(1L);
        EventoDTO eventoDTO2 = new EventoDTO();
        assertThat(eventoDTO1).isNotEqualTo(eventoDTO2);
        eventoDTO2.setId(eventoDTO1.getId());
        assertThat(eventoDTO1).isEqualTo(eventoDTO2);
        eventoDTO2.setId(2L);
        assertThat(eventoDTO1).isNotEqualTo(eventoDTO2);
        eventoDTO1.setId(null);
        assertThat(eventoDTO1).isNotEqualTo(eventoDTO2);
    }
}
