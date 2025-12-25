package org.example.tf25.proxy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class EventosKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(EventosKafkaListener.class);

    private final RestClient backendRestClient;
    private final ObjectMapper objectMapper;

    public EventosKafkaListener(@Qualifier("backendRestClient") RestClient backendRestClient,
                                ObjectMapper objectMapper) {
        this.backendRestClient = backendRestClient;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${kafka.topic.eventos}",
            groupId = "${spring.kafka.consumer.group-id:tf25-proxy}"
    )
    public void onEventoChange(String raw) {
        char firstChar = (raw != null && !raw.isBlank()) ? raw.trim().charAt(0) : '?';
        log.info("[eventos-actualizacion] raw='{}' len={} firstChar={}",
                raw, (raw != null ? raw.length() : -1), firstChar);

        Optional<Long> externalIdOpt = parseExternalId(raw);
        if (externalIdOpt.isEmpty()) {
            log.warn("Kafka: mensaje inválido, no pude extraer externalId. raw='{}'", raw);
            return;
        }
        long externalId = externalIdOpt.get();
        log.info("Kafka: recibido cambio de evento externalId={}", externalId);

        try {
            backendRestClient.post()
                    .uri("/api/eventos/sync/{externalId}", externalId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notificación enviada al backend para externalId={}", externalId);
        } catch (Exception e) {
            log.error("Error notificando al backend para externalId={}", externalId, e);
        }
    }

    private Optional<Long> parseExternalId(String raw) {
        if (raw == null) return Optional.empty();
        String t = raw.trim();
        if (t.isEmpty()) return Optional.empty();

        // Caso: "2"
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1).trim();
        }

        // Caso: 2 (o "2" ya sin comillas)
        if (t.matches("\\d+")) {
            try { return Optional.of(Long.parseLong(t)); } catch (Exception ignored) {}
        }

        // Caso: JSON {"eventoId":2} / {"externalId":"2"} / {"id": 2}
        try {
            JsonNode node = objectMapper.readTree(t);
            JsonNode idNode = node.hasNonNull("eventoId") ? node.get("eventoId")
                    : node.hasNonNull("externalId") ? node.get("externalId")
                    : node.get("id");

            if (idNode == null || idNode.isNull()) return Optional.empty();

            if (idNode.isNumber()) return Optional.of(idNode.asLong());
            if (idNode.isTextual() && idNode.asText().trim().matches("\\d+")) {
                return Optional.of(Long.parseLong(idNode.asText().trim()));
            }
        } catch (Exception ignored) {
            // no es JSON válido
        }

        return Optional.empty();
    }
}
