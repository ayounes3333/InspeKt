package com.inspekt.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a single key-value pair used in headers, query params, form data, etc.
 */
@Serializable
data class KeyValueParam(
    val id: String = generateId(),
    val key: String = "",
    val value: String = "",
    val description: String = "",
    val enabled: Boolean = true
)

/**
 * Supported HTTP methods.
 */
@Serializable
enum class HttpMethod(val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS")
}

/**
 * The body type for a request.
 */
@Serializable
enum class BodyType {
    NONE,
    JSON,
    FORM_URL_ENCODED,
    MULTIPART_FORM,
    RAW_TEXT,
    BINARY
}

/**
 * Represents the body of an HTTP request.
 */
@Serializable
data class RequestBody(
    val type: BodyType = BodyType.NONE,
    val rawContent: String = "",
    val formParams: List<KeyValueParam> = emptyList()
)

/**
 * Represents a fully-configured HTTP request.
 */
@Serializable
data class HttpRequest(
    val id: String = generateId(),
    val name: String = "New Request",
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val headers: List<KeyValueParam> = emptyList(),
    val queryParams: List<KeyValueParam> = emptyList(),
    val body: RequestBody = RequestBody(),
    val description: String = ""
)

/**
 * The result of executing an HTTP request.
 */
data class HttpResponse(
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, String>,
    val body: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val requestId: String
)

/**
 * Error state when a request fails.
 */
data class RequestError(
    val message: String,
    val cause: Throwable? = null
)

expect fun generateId(): String
