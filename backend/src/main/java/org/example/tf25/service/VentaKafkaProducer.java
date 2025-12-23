package org.example.tf25.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import org.example.tf25.domain.Venta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class VentaKafkaProducer {

    private final KafkaTemplate<String, JsonNode> kafkaTemplate;
    private final String topic;

    public VentaKafkaProducer(KafkaTemplate<String, JsonNode> kafkaTemplate,
                              @Value("${tf25.kafka.topic.ventas:ventas-confirmadas}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<Void> enviarNotificacionVenta(Venta venta) {
        String key = String.valueOf(venta.getId());
        int eventoId;
        try {
            eventoId = Integer.parseInt(venta.getExternalEventoId());
        } catch (Exception e) {
            log.error("Venta {}: externalEventoId '{}' no es un entero válido", venta.getId(), venta.getExternalEventoId());
            return CompletableFuture.failedFuture(new IllegalArgumentException("externalEventoId inválido: " + venta.getExternalEventoId()));
        }

        JsonNode payload = JsonNodeFactory.instance.objectNode()
                .put("ventaId", venta.getId())
                .put("eventoId", eventoId)
                .set("asientos", JsonNodeFactory.instance.arrayNode().addAll(
                        venta.getAsientosIds().stream().map(JsonNodeFactory.instance::textNode).toList()
                ));

        log.info("Kafka SEND topic={} key={} payload={}", topic, key, payload);

        return kafkaTemplate.send(topic, key, payload)
                .thenAccept(result -> {
                    log.info("Kafka OK topic={} key={} partition={} offset={}",
                            topic, key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                })
                .whenComplete((ok, ex) -> {
                    if (ex != null) {
                        log.error("Kafka FAIL topic={} key={} ex={}", topic, key, ex.toString(), ex);
                    }
                })
                .thenApply(x -> (Void) null);
    }
}
