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
    var gridFilas by mutableStateOf(10)
    var gridColumnas by mutableStateOf(10)

    fun loadAsientos(scope: CoroutineScope, externalId: String, filas: Int? = null, columnas: Int? = null) {
        loading = true
        scope.launch {
            try {
                val (ocupados, sesion) = ventaRepo.getAsientos(externalId)

                // Mapear ocupados por id para lookup rápido
                val ocupadosPorId = ocupados.associateBy { it.id }

                // Determinar tamaño de la sala
                // - Si el endpoint provee filas/columnas (>0), usamos EXACTAMENTE esos valores.
                // - Si no, inferimos el tamaño máximo a partir de los asientos recibidos.
                val rowsHint = filas ?: 0
                val colsHint = columnas ?: 0
                var targetFilas: Int
                var targetColumnas: Int

                if (rowsHint > 0 && colsHint > 0) {
                    targetFilas = rowsHint
                    targetColumnas = colsHint
                } else {
                    var maxFilaDetectada = 0
                    var maxColDetectada = 0
                    for (a in ocupados) {
                        if (a.fila > maxFilaDetectada) maxFilaDetectada = a.fila
                        if (a.columna > maxColDetectada) maxColDetectada = a.columna
                    }
                    // Si no hay datos -> no dibujar grilla
                    if (maxFilaDetectada <= 0 && maxColDetectada <= 0 && ocupados.isEmpty()) {
                        gridFilas = 0
                        gridColumnas = 0
                        asientos = emptyList()
                        sessionId = sesion
                        return@launch
                    }
                    targetFilas = maxFilaDetectada
                    targetColumnas = maxColDetectada
                }

                // Reconstruir grilla completa: lo que no venga = LIBRE
                val todos = mutableListOf<Asiento>()
                for (fila in 1..targetFilas) {
                    for (col in 1..targetColumnas) {
                        val id = "r${fila}c${col}"
                        val existente = ocupadosPorId[id]
                        if (existente != null) {
                            todos += existente
                        } else {
                            todos += Asiento(
                                id = id,
                                fila = fila,
                                columna = col,
                                estado = "LIBRE"
                            )
                        }
                    }
                }

                gridFilas = targetFilas
                gridColumnas = targetColumnas
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
