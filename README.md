# TF25 – Backend y Proxy (Guía rápida de puesta en marcha)

Este repositorio contiene dos aplicaciones Spring Boot:
- backend (8080)
- proxy (8081)

La cátedra migró el servidor y ya responde. Para estar visibles en la red y probar fin‑a‑fin, seguí los pasos de esta guía.

## 1) Conectividad de red
- Unirse a la red ZeroTier: `sudo zerotier-cli join 0cccb752f7298a10`
- Ver el estado: `zerotier-cli listnetworks` (debe mostrar `OK` y `PORT_DEVICE` asignado)
- Hacer ping al servidor provisto por la cátedra. Ejemplo: `ping -c 3 192.168.194.250` (ajustar IP si corresponde).

## 2) Perfiles y puertos
- Backend: corre en `8080` (`backend/src/main/resources/application.yml` → `server.port: 8080`).
- Proxy: corre en `8081` (`proxy/src/main/resources/application.yml`/`application-catedra.yml` → `server.port: 8081`).
- Perfil recomendado para pruebas con la cátedra: `catedra`.

## 3) Variables de entorno (proxy)
Archivo `proxy/src/main/resources/application-catedra.yml`:
- Kafka: `spring.kafka.bootstrap-servers: 192.168.194.250:9092` (ajustar si cambió).
- Backend local: `tf25.backend.base-url: http://localhost:8080`.
- Cátedra (remoto): `tf25.catedra.base-url: ${TF25_CATEDRA_BASE_URL:http://192.168.194.250:8080}`.
- Autenticación contra la cátedra:
  - Token estático opcional: `TF25_CATEDRA_BEARER_TOKEN` (si se define, NO hace login; usarlo si te piden “generar un nuevo token”).
  - Login automático: setear `TF25_CATEDRA_AUTH_USERNAME` y `TF25_CATEDRA_AUTH_PASSWORD`, y opcionalmente `TF25_CATEDRA_AUTH_LOGIN_PATH` (default `/api/authenticate`).
- Kafka topic: `kafka.topic.eventos: NOMBRE_DEL_TOPIC_REAL` → reemplazar por el nombre real provisto por la cátedra.

Tip: copiá `.env.sample` a `.env` en la raíz y completá los valores. `docker-compose` y/o tu IDE pueden usarlo automáticamente.

## 4) Arranque de servicios
- Backend: `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=catedra`
- Proxy: `cd proxy && ./mvnw spring-boot:run -Dspring-boot.run.profiles=catedra`
- Alternativa JAR: `java -jar target/tf25-0.0.1-SNAPSHOT.jar --spring.profiles.active=catedra`

## 5) Healthchecks rápidos
- Backend: `curl -i http://localhost:8080/actuator/health`
- Proxy: `curl -i http://localhost:8081/actuator/health`

## 6) Endpoints funcionales
- Eventos (backend expone público):
  - `curl -i http://localhost:8080/api/eventos`
- Proxy hacia cátedra (ajustar paths según tus controladores):
  - Asientos (ejemplo): `curl -i "http://localhost:8081/api/proxy/asientos?eventoId=..."`
  - Bloqueo de asientos (POST vía proxy):
    ```bash
    curl -i -X POST http://localhost:8081/api/proxy/bloqueos \
      -H 'Content-Type: application/json' \
      -d '{
        "eventoId": 123,
        "asientos": [{"fila":1,"columna":2},{"fila":1,"columna":3}],
        "minutos": 5
      }'
    ```
  - Si la cátedra exige token y usás valor estático, exportá: `export TF25_CATEDRA_BEARER_TOKEN="<BEARER_TOKEN_NUEVO>"` y reiniciá el proxy.
  - Si usás login: exportá `TF25_CATEDRA_AUTH_USERNAME` y `TF25_CATEDRA_AUTH_PASSWORD` y verificá el login en logs.

## 7) Qué esperar tras la migración
- El error 500 en bloqueo no debería ocurrir. Si aparece 401/403, es token inválido o expirado (regenerá token o usá login automático).
- Si aparece 5xx, revisá conectividad entre proxy y cátedra (`TF25_CATEDRA_BASE_URL`) y los logs del proxy.

## 8) Logs útiles
- Backend: ver salida de Spring al iniciar; endpoints públicos: `/api/eventos`, `/api/mock/**`, `/actuator/*`.
- Proxy: buscar logs de `CatedraAuthService` (login/refresh) y `BloqueoAsientosProxyService`.
- Kafka: confirmar topic y broker accesible.

## 9) Troubleshooting rápido
- Ping OK pero `curl` tarda/timeout: revisar firewall/puertos 8080/8081 en el nuevo proveedor.
- 401/403 al bloquear: renovar token (estático o login) y reiniciar el proxy.
- 404 desde proxy: confirmar rutas de `BloqueoAsientosProxyController`/`AsientosProxyController` y que `tf25.catedra.base-url` apunta al servicio correcto.
- 500 persistente: capturar body y logs del proxy y backend; validar payload contra `PeticionBloqueoAsientosRemotaDto`.

## 10) Seguridad y perfiles
- `backend/src/main/java/org/example/tf25/config/SecurityConfig.java` está activo cuando el perfil NO es `catedra`.
- Para entorno cátedra, se activa `SecurityConfigCatedra`. Ejecutar con `--spring.profiles.active=catedra` asegura las reglas correctas.

## 11) Qué necesitamos de vos para validar fin‑a‑fin
Por favor compartí:
- ¿Podés hacer ping al servidor nuevo y unirte a ZeroTier `0cccb752f7298a10`?
- ¿Usarás token estático (enviarnos el valor) o credenciales de login para configurar el proxy?
- ¿Cuál es el nombre real del topic Kafka y el bootstrap server si difiere de `192.168.194.250:9092`?
- Resultados de:
  - `curl -i http://localhost:8081/actuator/health`
  - `curl -i http://localhost:8080/actuator/health`
  - Un intento de bloqueo (request/response) para verificar fin‑a‑fin.
