package org.example.tf25.proxy.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.example.tf25.proxy.service.LockService;

/**
 * Pasarela para los endpoints de VENTAS de la cátedra (consigna 2025).
 *
 * - POST /api/endpoints/v1/realizar-venta           -> POST  {BASE}/api/endpoints/v1/realizar-venta
 * - GET  /api/endpoints/v1/listar-ventas             -> GET   {BASE}/api/endpoints/v1/listar-ventas
 * - GET  /api/endpoints/v1/listar-venta/{id}         -> GET   {BASE}/api/endpoints/v1/listar-venta/{id}
 *
 * Política de errores "amable":
 * - En listados, ante error remoto devolvemos 200 con array vacío.
 * - En detalle, ante error remoto devolvemos 404.
 * - En crear (realizar-venta), ante error remoto devolvemos 200 con un objeto que contenga
 *   al menos { "resultado": false, "descripcion": "Error ..." } si es posible; si no, 502 genérico.
 */
@RestController
@RequestMapping("/api/endpoints/v1")
public class VentasCatedraProxyController {

    private static final Logger log = LoggerFactory.getLogger(VentasCatedraProxyController.class);

    private final RestClient catedraRestClient;
    private final boolean forceSuccess;
    private final org.example.tf25.proxy.service.CatedraAuthService authService;
    private final LockService lockService;

    public VentasCatedraProxyController(@Qualifier("catedraRestClient") RestClient catedraRestClient,
                                        org.springframework.core.env.Environment env,
                                        @org.springframework.beans.factory.annotation.Value("${tf25.proxy.dev.force-success:false}") boolean forceSuccessProp,
                                        org.example.tf25.proxy.service.CatedraAuthService authService, LockService lockService) {
        this.catedraRestClient = catedraRestClient;
        boolean devProfile = java.util.Arrays.asList(env.getActiveProfiles()).contains("dev");
        this.forceSuccess = forceSuccessProp || devProfile;
        this.authService = authService;
        this.lockService = lockService;
    }

    @PostMapping("/realizar-venta")
    public ResponseEntity<?> realizarVenta(
            @RequestBody JsonNode ventaRequest,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionIdHeader
    ) {
        log.info("Proxy: realizando venta en cátedra...");

        // 1) Traducir payload backend -> cátedra
        String externalEventoId = ventaRequest.hasNonNull("externalEventoId") ? ventaRequest.get("externalEventoId").asText() : null;
        Long localVentaId = ventaRequest.hasNonNull("ventaId") ? ventaRequest.get("ventaId").asLong() : null;

        String sessionIdBody = ventaRequest.hasNonNull("sessionId") ? ventaRequest.get("sessionId").asText() : null;
        // Unificar: preferimos siempre el header X-Session-Id; si no viene, usamos el del body
        String sessionId = (sessionIdHeader != null && !sessionIdHeader.isBlank())
                ? sessionIdHeader
                : (sessionIdBody != null && !sessionIdBody.isBlank() ? sessionIdBody : null);

        // Modo dev/flag: forzar éxito sin llamar a la cátedra
        if (forceSuccess) {
            var ok = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            ok.put("resultado", true);
            ok.put("descripcion", "(dev) venta forzada exitosa");
            if (externalEventoId != null && sessionId != null) {
                lockService.releaseLocks(externalEventoId, sessionId);
            }
            return ResponseEntity.ok(ok);
        }

        String compradorEmail = ventaRequest.hasNonNull("compradorEmail") ? ventaRequest.get("compradorEmail").asText() : "";
        int eventoId;
        try {
            eventoId = Integer.parseInt(externalEventoId);
        } catch (Exception e) {
            var err = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            err.put("resultado", false);
            err.put("descripcion", "externalEventoId no numérico");
            return ResponseEntity.ok(err);
        }

        var f = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

        // 2) Parsear asientos (acepta asientosIds o asientos)
        java.util.List<com.fasterxml.jackson.databind.JsonNode> posiciones = new java.util.ArrayList<>(); java.util.List<String> rawIds = new java.util.ArrayList<>();
        JsonNode arr = null;
        if (ventaRequest.has("asientosIds") && ventaRequest.get("asientosIds").isArray()) {
            arr = ventaRequest.get("asientosIds");
        } else if (ventaRequest.has("asientos") && ventaRequest.get("asientos").isArray()) {
            arr = ventaRequest.get("asientos");
        }

        if (arr != null) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("r(\\d+)c(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
            for (JsonNode n : arr) {
                if (n == null || n.isNull()) continue;

                if (n.isTextual()) {
                    String id = n.asText(); rawIds.add(id);
                    var m = p.matcher(id == null ? "" : id.trim());
                    if (m.matches()) {
                        int fila = Integer.parseInt(m.group(1));
                        int columna = Integer.parseInt(m.group(2));
                        var pos = f.objectNode();
                        pos.put("fila", fila);
                        pos.put("columna", columna);
                        posiciones.add(pos);
                    }
                } else if (n.isObject()) {
                    if (n.has("fila") && n.has("columna")) {
                        int fila = n.get("fila").asInt();
                        int columna = n.get("columna").asInt();
                        rawIds.add("r" + fila + "c" + columna);
                        var pos = f.objectNode();
                        pos.put("fila", fila);
                        pos.put("columna", columna);
                        posiciones.add(pos);
                    } else if (n.has("asientoId") && n.get("asientoId").isTextual()) {
                        String id = n.get("asientoId").asText(); rawIds.add(id);
                        var m = p.matcher(id == null ? "" : id.trim());
                        if (m.matches()) {
                            int fila = Integer.parseInt(m.group(1));
                            int columna = Integer.parseInt(m.group(2));
                            var pos = f.objectNode();
                            pos.put("fila", fila);
                            pos.put("columna", columna);
                            posiciones.add(pos);
                        }
                    }
                }
            }
        }

        String sidLog = (sessionId == null) ? "(null)" : (sessionId.length() <= 8 ? sessionId : sessionId.substring(0, 8) + "…");
        log.info("Proxy→Catedra realizar-venta: eventoId={}, sessionId={}, asientos={}",
                eventoId, sidLog, posiciones.size());

        // Validación: no llamar a cátedra sin asientos válidos
        if (posiciones.isEmpty()) {
            var err = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            err.put("resultado", false);
            err.put("descripcion", "Se requiere al menos 1 asiento válido (formato rNcM o {fila,columna})");
            return ResponseEntity.ok(err);
        }

        // 3) Armar body upstream (alineado con el contrato de cátedra similar a implementación dino)
        var body = f.objectNode();
        body.put("eventoId", eventoId);

        // nombresOcupantes opcional desde el backend
        java.util.List<String> ocupantes = new java.util.ArrayList<>();
        if (ventaRequest.has("nombresOcupantes") && ventaRequest.get("nombresOcupantes").isArray()) {
            for (JsonNode n : ventaRequest.get("nombresOcupantes")) {
                if (n != null && n.isTextual()) ocupantes.add(n.asText());
            }
        }

        // asientos como objetos {fila,columna,persona}
        var arrAsientos = f.arrayNode();
        for (int i = 0; i < posiciones.size(); i++) {
            JsonNode pos = posiciones.get(i);
            var obj = f.objectNode();
            obj.set("fila", pos.get("fila"));
            obj.set("columna", pos.get("columna"));
            String persona = null;
            if (i < ocupantes.size()) {
                persona = ocupantes.get(i);
            } else if (compradorEmail != null && !compradorEmail.isBlank()) {
                persona = compradorEmail;
            }
            obj.put("persona", persona != null ? persona : "");
            arrAsientos.add(obj);
        }
        body.set("asientos", arrAsientos);

        // Campos adicionales usados por dino: fecha y precioVenta (si vienen del backend, respetarlos)
        if (ventaRequest.has("fecha")) {
            body.set("fecha", ventaRequest.get("fecha"));
        } else {
            body.put("fecha", java.time.OffsetDateTime.now().toString());
        }
        if (ventaRequest.has("precioVenta")) {
            body.set("precioVenta", ventaRequest.get("precioVenta"));
        }

        // incluir datos de comprador y session (por si upstream los usa)
        body.put("compradorEmail", compradorEmail == null ? "" : compradorEmail);
        if (sessionId != null && !sessionId.isBlank()) body.put("sessionId", sessionId);

        log.info("Proxy→Catedra realizar-venta body={}", body);

        // helper: construir request con header opcional
        java.util.function.Supplier<org.springframework.web.client.RestClient.RequestHeadersSpec<?>> requestSupplier = () -> {
            var req = catedraRestClient.post()
                    .uri("/api/endpoints/v1/realizar-venta")
                    .body(body);
            if (sessionId != null && !sessionId.isBlank()) {
                req = req.header("X-Session-Id", sessionId);
            }
            return req;
        };

        try {
            JsonNode response = requestSupplier.get()
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("resultado") && response.get("resultado").asBoolean()) {
                log.info("Proxy: venta exitosa en cátedra; liberando locks locales para session={}", sidLog);
                lockService.releaseLocks(externalEventoId, sessionId);
            }
            return ResponseEntity.ok(response);

        } catch (org.springframework.web.client.RestClientResponseException ex) {
            // 401 → refresh + retry 1 vez
            if (ex.getRawStatusCode() == 401) {
                try {
                    authService.refreshToken();
                    JsonNode response = requestSupplier.get()
                            .retrieve()
                            .body(JsonNode.class);

                    if (response != null && response.has("resultado") && response.get("resultado").asBoolean()) {
                        log.info("Proxy: venta exitosa en cátedra (retry); liberando locks locales para session={}", sidLog);
                        lockService.releaseLocks(externalEventoId, sessionId);
                    }
                    return ResponseEntity.ok(response);
                } catch (org.springframework.web.client.RestClientResponseException ex2) {
                    return ResponseEntity.ok(errorAmigableConUpstream(ex2));
                } catch (org.springframework.web.client.RestClientException ex2) {
                    return ResponseEntity.ok(errorComunicacion());
                }
            }
            return ResponseEntity.ok(errorAmigableConUpstream(ex));

        } catch (org.springframework.web.client.RestClientException ex) {
            log.warn("Proxy: error de comunicación con cátedra en realizar-venta", ex);
            return ResponseEntity.ok(errorComunicacion());
        }
    }

    private com.fasterxml.jackson.databind.node.ObjectNode errorComunicacion() {
        var err = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        err.put("resultado", false);
        err.put("descripcion", "Error de comunicación con cátedra");
        return err;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode errorAmigableConUpstream(org.springframework.web.client.RestClientResponseException ex) {
        String rb = ex.getResponseBodyAsString();
        if (rb != null && rb.length() > 500) rb = rb.substring(0, 500) + "...";
        log.warn("Proxy: realizar-venta HTTP {} upstream={}", ex.getRawStatusCode(), rb);

        var err = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        err.put("resultado", false);
        err.put("descripcion", "Error HTTP " + ex.getRawStatusCode() + " en cátedra");
        if (rb != null && !rb.isBlank()) err.put("upstream", rb);
        return err;
    }

    @GetMapping("/listar-ventas")
    public ResponseEntity<?> listarVentas() {
        log.info("Proxy: listando ventas en cátedra...");
        try {
            JsonNode response = catedraRestClient.get()
                    .uri("/api/endpoints/v1/listar-ventas")
                    .retrieve()
                    .body(JsonNode.class);
            
            if (response == null || (response.isArray() && response.size() == 0)) {
                // No inyectar mocks: devolver arreglo vacío para reflejar el estado real
                return ResponseEntity.ok(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
            }
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.warn("Proxy: error consultando listado de ventas en cátedra: {}", ex.toString());
            // Mantener política amable pero sin mocks
            return ResponseEntity.ok(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
        }
    }

    // Eliminado: no devolver ventas mock; queremos reflejar el estado real de la cátedra

    @GetMapping("/listar-venta/{id}")
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
