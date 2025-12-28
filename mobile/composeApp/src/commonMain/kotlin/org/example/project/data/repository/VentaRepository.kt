package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.example.project.model.Asiento
import org.example.project.model.Venta
import org.example.project.network.SERVER_URL
import org.example.project.network.PROXY_URL

@Serializable
data class BloqueoRequest(val asientosIds: List<String>)

@Serializable
data class BloqueoResultado(val asientoId: String, val estado: String, val mensaje: String?)

@Serializable
data class BloqueoResponse(val resultados: List<BloqueoResultado>)

@Serializable
data class ConfirmarVentaRequest(val compradorEmail: String, val nombresOcupantes: List<String>)

@Serializable
data class ConfirmarVentaResponse(
    val ventaId: Long,
    val externalEventoId: String,
    val sessionId: String,
    val estado: String,
    val mensaje: String
)

class VentaRepository(private val client: HttpClient) {

    suspend fun getAsientos(externalEventoId: String): Pair<List<Asiento>, String> {
        val response = client.get("$SERVER_URL/api/eventos/$externalEventoId/asientos")
        if (response.status.value != 200) throw Exception("Error al cargar asientos: ${response.status}")
        val asientos = response.body<List<Asiento>>()
        val sessionId = response.headers["X-Session-Id"]
            ?: throw IllegalStateException("Falta header X-Session-Id")
        return Pair(asientos, sessionId)
    }

    suspend fun bloquearAsientos(externalEventoId: String, sessionId: String, asientosIds: List<String>): BloqueoResponse {
        val response = client.post("$SERVER_URL/api/eventos/$externalEventoId/bloqueos") {
            header("X-Session-Id", sessionId)
            contentType(ContentType.Application.Json)
            setBody(BloqueoRequest(asientosIds))
        }
        if (response.status.value == 200) {
            return response.body()
        } else {
            throw Exception("Error al bloquear asientos: ${response.status}")
        }
    }

    suspend fun confirmarVenta(sessionId: String, email: String, ocupantes: List<String>): ConfirmarVentaResponse {
        val response = client.post("$SERVER_URL/api/ventas/confirmar") {
            header("X-Session-Id", sessionId)
            contentType(ContentType.Application.Json)
            setBody(ConfirmarVentaRequest(email, ocupantes))
        }
        if (response.status.value == 200) {
            return response.body()
        } else {
            throw Exception("Error al confirmar venta: ${response.status}")
        }
    }

    suspend fun getVentas(email: String? = null): List<Venta> {
        val response = client.get("$SERVER_URL/api/ventas") {
            if (email != null) {
                url {
                    parameters.append("email", email)
                }
            }
        }
        if (response.status.value == 200) {
            return response.body()
        } else if (response.status.value == 401 || response.status.value == 403) {
            throw Exception("Sesión expirada.")
        } else {
            throw Exception("Error al cargar historial: ${response.status}")
        }
    }
}
