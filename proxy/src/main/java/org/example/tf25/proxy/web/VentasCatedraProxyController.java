package org.example.tf25.proxy.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/**
 * Pasarela para los endpoints de VENTAS de la cátedra (consigna 2025).
 *
 * - POST /api/catedra/realizar-venta           -> POST  {BASE}/api/endpoints/v1/realizar-venta
 * - GET  /api/catedra/ventas                   -> GET   {BASE}/api/endpoints/v1/listar-ventas
 * - GET  /api/catedra/ventas/{id}              -> GET   {BASE}/api/endpoints/v1/listar-venta/{id}
 *
 * Política de errores "amable":
 * - En listados, ante error remoto devolvemos 200 con array vacío.
 * - En detalle, ante error remoto devolvemos 404.
 * - En crear (realizar-venta), ante error remoto devolvemos 200 con un objeto que contenga
 *   al menos { "resultado": false, "descripcion": "Error ..." } si es posible; si no, 502 genérico.
 */
@RestController
@RequestMapping("/api/catedra")
public class VentasCatedraProxyController {

    private static final Logger log = LoggerFactory.getLogger(VentasCatedraProxyController.class);

    private final RestClient catedraRestClient;

    public VentasCatedraProxyController(@Qualifier("catedraRestClient") RestClient catedraRestClient) {
        this.catedraRestClient = catedraRestClient;
    }

    @PostMapping("/realizar-venta")
    public ResponseEntity<?> realizarVenta(@RequestBody JsonNode ventaRequest) {
        log.info("Proxy: realizando venta en cátedra...");
        try {
            JsonNode response = catedraRestClient.post()
                    .uri("/api/endpoints/v1/realizar-venta")
                    .body(ventaRequest)
                    .retrieve()
                    .body(JsonNode.class);
            return ResponseEntity.ok(response);
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            log.warn("Proxy: error HTTP {} en realizar-venta: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            // Intentar retornar un objeto compatible marcando fracaso
            try {
                com.fasterxml.jackson.databind.node.ObjectNode err = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                err.put("resultado", false);
                err.put("descripcion", "Error HTTP " + ex.getRawStatusCode() + " en cátedra");
                return ResponseEntity.ok(err);
            } catch (Exception ignore) {
                return ResponseEntity.status(502).build();
            }
        } catch (org.springframework.web.client.RestClientException ex) {
            log.warn("Proxy: error de comunicación con cátedra en realizar-venta", ex);
            try {
                com.fasterxml.jackson.databind.node.ObjectNode err = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                err.put("resultado", false);
                err.put("descripcion", "Error de comunicación con cátedra");
                return ResponseEntity.ok(err);
            } catch (Exception ignore) {
                return ResponseEntity.status(502).build();
            }
        }
    }

    @GetMapping("/ventas")
    public ResponseEntity<?> listarVentas() {
        log.info("Proxy: listando ventas en cátedra...");
        try {
            JsonNode response = catedraRestClient.get()
                    .uri("/api/endpoints/v1/listar-ventas")
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return ResponseEntity.ok(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
            }
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.warn("Proxy: error consultando listado de ventas en cátedra: {}", ex.toString());
            // No 500: devolver array vacío
            return ResponseEntity.ok(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
        }
    }

    @GetMapping("/ventas/{id}")
    public ResponseEntity<?> obtenerVenta(@PathVariable("id") String ventaId) {
        log.info("Proxy: obteniendo venta {} en cátedra...", ventaId);
        try {
            JsonNode response = catedraRestClient.get()
                    .uri("/api/endpoints/v1/listar-venta/{id}", ventaId)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.isNull()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.warn("Proxy: error consultando venta {} en cátedra: {}", ventaId, ex.toString());
            return ResponseEntity.notFound().build();
        }
    }
}
