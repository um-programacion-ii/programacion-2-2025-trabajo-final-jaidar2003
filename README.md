# TF25 — Guía rápida para levantar Backend, Proxy y Mobile

Este documento explica, de forma práctica, cómo levantar todo el entorno del proyecto: base de datos, backend, proxy y la app mobile (Android/iOS).


## 1) Requisitos
- Docker y Docker Compose
- Java 21 (Temurin u OpenJDK) + Maven 3.9+
- Android Studio (para Android)
- Xcode (para iOS, solo en macOS con CPU Apple/Intel compatible)

Puertos utilizados por defecto en tu máquina:
- Postgres: 5432
- Redis: 6379
- Backend: 8080
- Proxy: 8081
- Kafka test (si se usa la imagen local): 9093→9092 interno


## 2) Construir las apps Java (backend y proxy)
Desde la raíz del repo:

```bash
mvn -DskipTests package
```

Esto genera:
- backend/target/tf25-0.0.1-SNAPSHOT.jar
- proxy/target/tf25-proxy-0.0.1-SNAPSHOT.jar

El docker-compose mapea esos JARs dentro de contenedores slim de Java.


## 3) Levantar infraestructura + backend + proxy
Asegúrate de no tener servicios ocupando esos puertos. Luego:

```bash
docker compose up -d
```

Esto levanta:
- Postgres (tf25-postgres)
- Redis (tf25-redis)
- Kafka de test (tf25-kafka-test) — opcional, queda disponible
- Proxy (tf25-proxy)
- Backend (tf25-backend)

Logs rápidos:
```bash
docker compose logs -f postgres
docker compose logs -f redis
docker compose logs -f proxy
docker compose logs -f backend
```

Health/endpoints útiles (en tu host):
- Backend: http://localhost:8080/actuator/health
- Proxy:   http://localhost:8081/actuator/health


### 3.1) Configuración por defecto (infraestructura de la cátedra)
El docker-compose está preparado para hablar con la infraestructura de la cátedra:
- Kafka: 192.168.194.250:9092 (vía `SPRING_KAFKA_BOOTSTRAP_SERVERS`)
- Redis (cátedra) para asientos: `TF25_CATEDRA_REDIS_HOST` y `TF25_CATEDRA_REDIS_PORT`
- Autenticación cátedra (opcional):
  - `TF25_CATEDRA_BASE_URL`
  - `TF25_CATEDRA_BEARER_TOKEN`
  - o login automático: `TF25_CATEDRA_AUTH_LOGIN_PATH`, `TF25_CATEDRA_AUTH_USERNAME`, `TF25_CATEDRA_AUTH_PASSWORD`

Si tenés credenciales/URLs, podés exportarlas antes de levantar el compose o definirlas en un archivo `.env` al lado del `docker-compose.yml`.


### 3.2) Usar Kafka local de prueba (opcional)
Si NO tenés acceso a Kafka de la cátedra, podés apuntar backend/proxy al broker local del servicio `kafka-test`:
- Editá `docker-compose.yml` y cambiá estas variables:
  - En `proxy`:
    - `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-test:9092`
  - En `backend`:
    - `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-test:9092`
- También podrías desactivar cualquier dependencia externa de la cátedra según tu necesidad de prueba.

Luego recreá servicios:
```bash
docker compose up -d --force-recreate --no-deps proxy backend
```


## 4) Ejecutar el Mobile
El módulo móvil está en `mobile/`. Es un proyecto Kotlin Multiplatform con Compose.

### 4.1) URL del servidor que usa la app
La app consulta a una URL base definida por plataforma:
- Android: `mobile/composeApp/src/androidMain/kotlin/org/example/project/network/ApiConfig.android.kt`
  - actual: `actual val SERVER_URL: String = "http://10.0.2.2:8080"`
- iOS: `mobile/composeApp/src/iosMain/kotlin/org/example/project/network/ApiConfig.ios.kt`
  - actual: `actual val SERVER_URL: String = "http://localhost:8080"`

Por defecto, apuntan al BACKEND en 8080. Si tu flujo del app debe pasar por el PROXY, cambiá el puerto a 8081.

Recomendado:
- Para usar el PROXY:
  - Android: `http://10.0.2.2:8081`
  - iOS:     `http://localhost:8081`
- Para ir directo al BACKEND (solo si el app realmente no necesita el proxy):
  - Android: `http://10.0.2.2:8080`
  - iOS:     `http://localhost:8080`

Notas de red:
- Android Emulator: `10.0.2.2` es el alias a `localhost` de tu host.
- iOS Simulator: `localhost` llega a tu host directamente.
- Dispositivo físico (Android/iOS): usá la IP de tu host en la misma red Wi‑Fi, por ejemplo `http://192.168.0.23:8081`.


### 4.2) Correr en Android
1) Abrí Android Studio y el proyecto `mobile/` (o la raíz y seleccioná el módulo mobile).
2) Seleccioná un dispositivo (emulador o físico) y la configuración `run` del app.
3) Ejecutá.

Ver logs en Android:
- Logcat (Android Studio)
- Si necesitás imprimir más detalle, usá logs dentro de la app.


### 4.3) Correr en iOS
1) Requisitos: Xcode instalado (macOS), toolchains configuradas.
2) Desde Android Studio (KMP) o Xcode (si hay target iOS/iosApp):
   - Proyecto iOS: `mobile/iosApp/`.
   - Elegí un simulador y corré.

Si corrés en dispositivo físico, ajustá la `SERVER_URL` a la IP de tu host.


### 4.4) Comandos por consola (CLI) para levantar el Mobile

#### Android (Gradle + ADB)
Requisitos: Android SDK/ADB configurados y un emulador Android iniciado (o un dispositivo físico conectado con depuración USB).

```bash
# Desde la raíz del repo o dentro de mobile/
cd mobile

# Compilar debug
./gradlew :composeApp:clean :composeApp:assembleDebug

# Instalar en el emulador/dispositivo conectado
./gradlew :composeApp:installDebug

# (Opcional) Lanzar la app automáticamente vía ADB
# Reemplazá <applicationId> por el applicationId real (ver AndroidManifest o build.gradle.kts)
adb shell monkey -p <applicationId> -c android.intent.category.LAUNCHER 1
```

Cómo obtener el `applicationId`:
- Abrí `mobile/composeApp/src/androidMain/AndroidManifest.xml` y buscá el atributo `package`, o
- Revisá `android { defaultConfig { applicationId = "..." } }` en el `build.gradle.kts` del target Android.

Si preferís todo desde Android Studio: Run ▶ elegís el dispositivo y listo.

#### iOS (Simulador)
Opciones:
- Recomendado: abrir el proyecto iOS y correr desde Xcode (seleccionás un simulador y Run).
  - Ruta típica: `mobile/iosApp/`
- CLI (avanzado, puede variar según configuración del módulo iOS):
  1) Listar simuladores: `xcrun simctl list devices`
  2) Boot de un simulador (si no está corriendo): `xcrun simctl boot "iPhone 15"`
  3) Construir con Xcodebuild (ajustando el scheme si es `iosApp`):
     ```bash
     cd mobile/iosApp
     xcodebuild -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15' build
     ```
  4) Si tu configuración de KMP expone tareas Gradle para iOS (puede variar por plantilla), también podés explorar:
     ```bash
     cd mobile
     ./gradlew tasks --all | grep -i ios
     ```
     y usar tareas como `:composeApp:iosSimulatorArm64Binaries` / `...InstallDebug` cuando estén disponibles.

Notas:
- Asegurate de que `SERVER_URL` apunte al puerto correcto (8081 si querés pasar por el proxy; 8080 si vas directo al backend).
- Android Emulator usa `10.0.2.2` para alcanzar `localhost` del host. iOS Simulator puede usar `http://localhost`.


## 5) Flujo de prueba rápido
1) Levantá infra + apps (Sección 3).
2) Definí en el mobile la URL que corresponda (proxy o backend).
3) Desde el mobile, ejecutá acciones básicas y verificá respuestas:
   - 200/OK del backend/proxy
   - Revisa logs de contenedores si hay errores (Kafka/Redis/Postgres/HTTP).


## 6) Solución de problemas comunes
- Los contenedores están levantados pero el health marca `DOWN`:
  - Esperá unos segundos; Postgres y Redis tienen healthchecks.
  - Revisá `docker compose logs -f backend` y `docker compose logs -f proxy`.
- Kafka de cátedra no es accesible:
  - Usá la opción de Kafka local (Sección 3.2) o desactiva health de Kafka (ya viene `MANAGEMENT_HEALTH_KAFKA_ENABLED: "false"`).
- Puerto ya en uso:
  - Cambiá el mapeo de puertos en `docker-compose.yml` o liberá el puerto.
- El mobile en dispositivo físico no conecta:
  - Verificá que usás la IP del host y que no hay firewall bloqueando.
- Cambié la `SERVER_URL` pero no surte efecto:
  - Limpiá y reconstruí el proyecto mobile: `./gradlew :composeApp:clean :composeApp:assembleDebug` o desde Android Studio "Invalidate Caches / Restart".


## 7) Limpieza
Para detener:
```bash
docker compose down
```

Para bajar y borrar volúmenes (perdés datos de Postgres):
```bash
docker compose down -v
```

Para reconstruir JARs después de cambios en código Java:
```bash
mvn -DskipTests package

docker compose up -d --force-recreate --no-deps backend proxy
```


## 8) Variables útiles (resumen)
- Backend:
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`
  - `TF25_PROXY_BASE_URL` (backend → proxy)
- Proxy:
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`
  - `TF25_BACKEND_BASE_URL` (proxy → backend)
  - `TF25_CATEDRA_BASE_URL`, `TF25_CATEDRA_BEARER_TOKEN`
  - `TF25_CATEDRA_AUTH_LOGIN_PATH`, `TF25_CATEDRA_AUTH_USERNAME`, `TF25_CATEDRA_AUTH_PASSWORD`
  - `TF25_CATEDRA_REDIS_HOST`, `TF25_CATEDRA_REDIS_PORT`

Sugerencia: crear un archivo `.env` junto al `docker-compose.yml` con tus valores locales para no tocar el YAML.
