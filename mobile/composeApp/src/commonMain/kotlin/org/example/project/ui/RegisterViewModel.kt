package org.example.project.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.data.AuthRepository
import org.example.project.model.RegisterRequest

class RegisterViewModel(private val authRepository: AuthRepository) {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var nombreAlumno by mutableStateOf("Juan Manuel Aidar")
    var descripcionProyecto by mutableStateOf("TF25")
    
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    fun register(scope: CoroutineScope, onRegisterSuccess: () -> Unit) {
        loading = true
        error = null
        scope.launch {
            val result = authRepository.register(
                RegisterRequest(username, password, firstName, lastName, email, nombreAlumno, descripcionProyecto)
            )
            loading = false
            if (result.isSuccess) {
                onRegisterSuccess()
            } else {
                error = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
