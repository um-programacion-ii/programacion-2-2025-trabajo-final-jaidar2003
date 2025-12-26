package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.example.project.model.EventoResumido
import org.example.project.network.SERVER_URL

class EventoRepository(private val client: HttpClient) {
    
    suspend fun getEventos(): List<EventoResumido> {
        val response = client.get("$SERVER_URL/api/eventos")
        if (response.status.value == 200) {
            return response.body()
        } else if (response.status.value == 401 || response.status.value == 403) {
            throw Exception("Sesión expirada. Por favor inicie sesión nuevamente.")
        } else {
            throw Exception("Error al cargar eventos: ${response.status}")
        }
    }

    suspend fun getEvento(id: Long): EventoResumido {
        val response = client.get("$SERVER_URL/api/eventos/$id")
        if (response.status.value == 200) {
            return response.body()
        } else {
            throw Exception("Error al cargar evento: ${response.status}")
        }
    }
}
