package org.example.tf25.proxy.service;

import org.example.tf25.proxy.dto.AsientoRemotoDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Service
public class AsientosProxyService {

    private final RestClient catedraRestClient;

    public AsientosProxyService(@Qualifier("catedraRestClient") RestClient catedraRestClient) {
        this.catedraRestClient = catedraRestClient;
    }

    public List<AsientoRemotoDto> obtenerAsientos(String externalEventoId) {
        AsientoRemotoDto[] asientos = catedraRestClient.get()
                .uri("/api/eventos/{id}/asientos", externalEventoId)
                .retrieve()
                .body(AsientoRemotoDto[].class);

        if (asientos == null) {
            return List.of();
        }
        return Arrays.asList(asientos);
    }
}
