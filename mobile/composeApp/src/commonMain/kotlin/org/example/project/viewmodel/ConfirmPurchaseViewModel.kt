package org.example.project.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.data.AuthRepository
import org.example.project.data.VentaRepository

class ConfirmPurchaseViewModel(
    private val ventaRepo: VentaRepository,
    private val authRepository: AuthRepository
) {
    var email by mutableStateOf(authRepository.getEmail() ?: "")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var showConfirmDialog by mutableStateOf(false)

    val occupantNames = mutableStateMapOf<String, String>()
    val occupantLastNames = mutableStateMapOf<String, String>()

    fun confirmPurchase(scope: CoroutineScope, sessionId: String, seats: List<String>, onSuccess: () -> Unit) {
        showConfirmDialog = false
        loading = true
        scope.launch {
            try {
                val ocupantes = seats.map { seatId ->
                    "${occupantNames[seatId] ?: ""} ${occupantLastNames[seatId] ?: ""}".trim()
                }
                ventaRepo.confirmarVenta(sessionId, email, ocupantes)
                onSuccess()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun isFormValid(seats: List<String>): Boolean {
        return email.isNotBlank() && seats.all { seatId ->
            (occupantNames[seatId]?.isNotBlank() ?: false) &&
            (occupantLastNames[seatId]?.isNotBlank() ?: false)
        }
    }
}
