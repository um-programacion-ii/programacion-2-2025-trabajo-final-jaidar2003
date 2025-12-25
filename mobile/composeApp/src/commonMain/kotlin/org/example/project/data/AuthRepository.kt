package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.data.local.TokenManager
import org.example.project.model.LoginRequest
import org.example.project.model.LoginResponse
import org.example.project.model.RegisterRequest
import org.example.project.model.RegisterResponse
import org.example.project.network.SERVER_URL

class AuthRepository(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun login(request: LoginRequest): Result<Unit> {
        return try {
            val response = client.post("$SERVER_URL/api/authenticate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.value == 200) {
                val loginResponse = response.body<LoginResponse>()
                tokenManager.token = loginResponse.idToken
                Result.success(Unit)
            } else {
                Result.failure(Exception("Login failed: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun register(request: RegisterRequest): Result<Unit> {
        return try {
            val response = client.post("$SERVER_URL/api/v1/agregar_usuario") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.value == 200) {
                val registerResponse = response.body<RegisterResponse>()
                if (registerResponse.creado) {
                    // Si el registro devuelve un token, lo guardamos para loguear automáticamente
                    if (registerResponse.token != null) {
                        tokenManager.token = registerResponse.token
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Registration failed: ${registerResponse.resultado}"))
                }
            } else {
                Result.failure(Exception("Registration failed: ${response.status}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    fun logout() {
        tokenManager.token = null
    }
    
    fun isLoggedIn(): Boolean {
        return tokenManager.token != null
    }
}
