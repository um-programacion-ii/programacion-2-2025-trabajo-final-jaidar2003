package org.example.project.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val rememberMe: Boolean = false
)

@Serializable
data class LoginResponse(
    @SerialName("id_token") val idToken: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val nombreAlumno: String,
    val descripcionProyecto: String
)

@Serializable
data class RegisterResponse(
    val creado: Boolean,
    val resultado: String,
    val token: String? = null
)
