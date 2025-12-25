package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class EventoResumido(
    val id: Long,
    val externalId: String?,
    val nombre: String,
    val descripcion: String,
    val fechaHora: String? = null,
    val precio: Double
)

@Serializable
data class Asiento(
    val id: String,
    val estado: String // "LIBRE", "OCUPADO", "RESERVADO"
)

@Serializable
 data class Venta(
     val id: Long,
     val externalEventId: String,
     val compradorEmail: String,
     val estado: String,
     val asientos: List<String>,
     val evento: EventoResumido? = null
 )
