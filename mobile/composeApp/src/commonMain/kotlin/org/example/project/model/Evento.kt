package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class EventoResumido(
    val id: Long,
    val externalId: String?,
    val nombre: String,
    val descripcion: String,
    val fechaHora: String? = null,
    val precio: Double,
    val cupo: Int? = null,
    val filaAsientos: Int? = null,
    val columnAsientos: Int? = null
)

@Serializable
data class Asiento(
    val id: String,
    val fila: Int,
    val columna: Int,
    val estado: String // "LIBRE", "OCUPADO", "RESERVADO"
)

@Serializable
 data class Venta(
     val id: Long,
     val externalEventId: String? = null,
     val compradorEmail: String? = null,
     val estado: String? = null,
     val asientos: List<String> = emptyList(),
     val ocupantes: List<String> = emptyList(),
     val eventoNombre: String? = null,
     val evento: EventoResumido? = null
 )
