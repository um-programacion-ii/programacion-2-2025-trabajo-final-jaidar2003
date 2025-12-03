package org.example.tf25.proxy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EventosKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(EventosKafkaListener.class);

    private final RestClient backendRestClient;

    public EventosKafkaListener(@Qualifier("backendRestClient") RestClient backendRestClient) {
        this.backendRestClient = backendRestClient;
    }

    @KafkaListener(
            topics = "${kafka.topic.eventos}",
            groupId = "${spring.kafka.consumer.group-id:tf25-proxy}"
    )
    public void onEventoChange(String externalId) {
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
}
