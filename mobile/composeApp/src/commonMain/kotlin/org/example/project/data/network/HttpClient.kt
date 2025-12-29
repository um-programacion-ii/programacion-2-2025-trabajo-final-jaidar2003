package org.example.project.network

import io.ktor.client.HttpClient
import org.example.project.data.local.TokenManager

// Expect/Actual para configurar el engine por plataforma
expect fun createHttpClient(tokenManager: TokenManager): HttpClient
