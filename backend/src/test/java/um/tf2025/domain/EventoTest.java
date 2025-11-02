package um.tf2025.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static um.tf2025.domain.EventoTestSamples.*;

import org.junit.jupiter.api.Test;
import um.tf2025.web.rest.TestUtil;

class EventoTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Evento.class);
        Evento evento1 = getEventoSample1();
        Evento evento2 = new Evento();
        assertThat(evento1).isNotEqualTo(evento2);

        evento2.setId(evento1.getId());
        assertThat(evento1).isEqualTo(evento2);

        evento2 = getEventoSample2();
        assertThat(evento1).isNotEqualTo(evento2);
    }
}
