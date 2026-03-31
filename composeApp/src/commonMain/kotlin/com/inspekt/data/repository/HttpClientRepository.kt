package com.inspekt.data.repository

import com.inspekt.domain.model.BodyType
import com.inspekt.domain.model.HttpRequest
import com.inspekt.domain.model.HttpResponse as DomainHttpResponse
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class HttpClientRepository {

    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
        expectSuccess = false // Don't throw on non-2xx
    }

    private fun ensureScheme(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    suspend fun execute(request: HttpRequest): Result<DomainHttpResponse> = runCatching {
        val startTime = currentTimeMillis()

        val response = client.request(ensureScheme(request.url)) {
            method = HttpMethod(request.method.value)

            // Query params
            request.queryParams
                .filter { it.enabled && it.key.isNotBlank() }
                .forEach { param -> parameter(param.key, param.value) }

            // Headers
            request.headers
                .filter { it.enabled && it.key.isNotBlank() }
                .forEach { h -> header(h.key, h.value) }

            // Body
            when (request.body.type) {
                BodyType.NONE -> Unit

                BodyType.JSON -> {
                    contentType(ContentType.Application.Json)
                    setBody(request.body.rawContent)
                }

                BodyType.RAW_TEXT -> {
                    contentType(ContentType.Text.Plain)
                    setBody(request.body.rawContent)
                }

                BodyType.FORM_URL_ENCODED -> {
                    val formData = parametersOf(
                        *request.body.formParams
                            .filter { it.enabled && it.key.isNotBlank() }
                            .map { it.key to listOf(it.value) }
                            .toTypedArray()
                    )
                    setBody(FormDataContent(formData))
                }

                BodyType.MULTIPART_FORM -> {
                    val parts = request.body.formParams
                        .filter { it.enabled && it.key.isNotBlank() }
                        .map { param -> FormPart(param.key, param.value) }
                    setBody(MultiPartFormDataContent(formData { parts.forEach { append(it) } }))
                }

                BodyType.BINARY -> {
                    // Binary content from rawContent treated as bytes
                    setBody(request.body.rawContent.encodeToByteArray())
                }
            }
        }

        val durationMs = currentTimeMillis() - startTime
        val bodyText = response.bodyAsText()
        val sizeBytes = bodyText.encodeToByteArray().size.toLong()

        DomainHttpResponse(
            statusCode = response.status.value,
            statusText = response.status.description,
            headers = response.headers.entries()
                .associate { (key, values) -> key to values.joinToString(", ") },
            body = bodyText,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            requestId = request.id
        )
    }

    fun close() = client.close()
}

expect fun currentTimeMillis(): Long
