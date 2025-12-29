package org.example.project.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.data.local.TokenManager

actual fun createHttpClient(tokenManager: TokenManager): HttpClient {
    val serverHost = try { Url(SERVER_URL).host } catch (_: Throwable) { null }
    return HttpClient(Darwin) {
        defaultRequest {
            // Intentar mantener la conexión abierta
            headers.append("Connection", "keep-alive")
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenManager.token
                    if (token != null) BearerTokens(token, "") else null
                }
                sendWithoutRequest { request ->
                    serverHost == null || request.url.host == serverHost
                }
            }
        }
    }
}

