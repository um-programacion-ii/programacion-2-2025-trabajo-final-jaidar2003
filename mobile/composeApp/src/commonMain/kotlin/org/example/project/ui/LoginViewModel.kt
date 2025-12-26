package org.example.project.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.example.project.data.AuthRepository
import org.example.project.model.LoginRequest

class LoginViewModel(private val authRepository: AuthRepository) {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    fun login(scope: CoroutineScope, onLoginSuccess: () -> Unit) {
        loading = true
        error = null
        scope.launch {
            val result = authRepository.login(LoginRequest(username, password))
            loading = false
            if (result.isSuccess) {
                onLoginSuccess()
            } else {
                error = "Login failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
