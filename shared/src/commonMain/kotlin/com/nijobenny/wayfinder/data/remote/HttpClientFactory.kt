package com.nijobenny.wayfinder.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shared Ktor client. The engine is chosen per platform: OkHttp on Android, Darwin on iOS. */
fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
    }
    defaultRequest {
        // Free OpenStreetMap services ask apps to identify themselves.
        header(HttpHeaders.UserAgent, "Wayfinder/1.0 (github.com/nijobenny/Wayfinder)")
    }
}
