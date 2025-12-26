package org.example.project.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.data.AuthRepository
import org.example.project.data.EventoRepository
import org.example.project.model.EventoResumido

class HomeViewModel(private val eventRepo: EventoRepository, private val authRepository: AuthRepository) {
    var events by mutableStateOf<List<EventoResumido>>(emptyList())
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    fun loadEvents(scope: CoroutineScope) {
        loading = true
        scope.launch {
            try {
                events = eventRepo.getEventos()
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                loading = false
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
