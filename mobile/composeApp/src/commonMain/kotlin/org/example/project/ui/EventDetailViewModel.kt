package org.example.project.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.data.VentaRepository
import org.example.project.model.Asiento

class EventDetailViewModel(private val ventaRepo: VentaRepository) {
    var asientos by mutableStateOf<List<Asiento>>(emptyList())
    var sessionId by mutableStateOf<String?>(null)
    var selectedSeats by mutableStateOf(setOf<String>())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadAsientos(scope: CoroutineScope, eventId: Long) {
        loading = true
        scope.launch {
            try {
                val (ocupados, sesion) = ventaRepo.getAsientos(eventId)

                // Mapear ocupados por id para lookup rápido
                val ocupadosPorId = ocupados.associateBy { it.id }

                // Inferir tamaño de la sala desde los ids con formato r{fila}c{col}
                val regex = Regex("""r(\d+)c(\d+)""")
                var maxFila = 0
                var maxCol = 0
                for (a in ocupados) {
                    val m = regex.matchEntire(a.id)
                    if (m != null) {
                        val f = m.groupValues[1].toInt()
                        val c = m.groupValues[2].toInt()
                        if (f > maxFila) maxFila = f
                        if (c > maxCol) maxCol = c
                    }
                }
                // Valores mínimos razonables si no hay datos suficientes
                if (maxFila < 9) maxFila = 9
                if (maxCol < 6) maxCol = 6

                // Reconstruir grilla completa: lo que no venga = LIBRE
                val todos = mutableListOf<Asiento>()
                for (fila in 1..maxFila) {
                    for (col in 1..maxCol) {
                        val id = "r${fila}c${col}"
                        val existente = ocupadosPorId[id]
                        if (existente != null) {
                            todos += existente
                        } else {
                            todos += Asiento(
                                id = id,
                                estado = "LIBRE"
                            )
                        }
                    }
                }

                asientos = todos
                sessionId = sesion
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun toggleSeat(seatId: String) {
        if (selectedSeats.contains(seatId)) {
            selectedSeats -= seatId
            error = null
        } else if (selectedSeats.size < 4) {
            selectedSeats += seatId
            error = null
        } else {
            error = "No puedes seleccionar más de 4 asientos"
        }
    }

    fun confirmSelectedSeats(scope: CoroutineScope, externalId: String, onConfirm: (List<String>, String) -> Unit) {
        scope.launch {
            loading = true
            try {
                val resp = ventaRepo.bloquearAsientos(externalId, sessionId!!, selectedSeats.toList())
                
                // Verificar si todos los asientos fueron bloqueados correctamente (OK o BLOQUEADO)
                val fallidos = resp.resultados.filter { 
                    it.estado != "OK" && it.estado != "BLOQUEADO" && it.estado != "YA_BLOQUEADO_OTRO"
                }

                if (fallidos.isEmpty()) {
                    onConfirm(selectedSeats.toList(), sessionId!!)
                } else {
                    val msg = fallidos.joinToString { "${it.asientoId}: ${it.mensaje ?: it.estado}" }
                    error = "No se pudieron bloquear algunos asientos: $msg"
                }
            } catch (e: Exception) {
                error = "Error al bloquear: ${e.message}"
            } finally {
                loading = false
            }
        }
    }
}
