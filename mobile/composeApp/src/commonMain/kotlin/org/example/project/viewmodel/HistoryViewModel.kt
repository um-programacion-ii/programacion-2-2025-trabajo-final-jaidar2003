package org.example.project.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.data.AuthRepository
import org.example.project.data.VentaRepository
import org.example.project.model.Venta

class HistoryViewModel(private val ventaRepo: VentaRepository, private val authRepository: AuthRepository) {
    var ventas by mutableStateOf<List<Venta>>(emptyList())
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    fun loadHistory(scope: CoroutineScope) {
        loading = true
        scope.launch {
            try {
                val email = authRepository.getEmail()
                ventas = ventaRepo.getVentas(email)
            } catch (e: Exception) {
                error = e.message ?: "Error al cargar historial"
            } finally {
                loading = false
            }
        }
    }
}
