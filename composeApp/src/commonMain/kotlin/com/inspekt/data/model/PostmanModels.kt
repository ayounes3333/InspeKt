package com.inspekt.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Postman Collection Format v2.1 JSON models.
 * Spec: https://schema.postman.com/collection/json/v2.1.0/draft-07/collection.json
 */

@Serializable
data class PostmanCollection(
    val info: PostmanInfo,
    val item: List<PostmanItem> = emptyList(),
    val variable: List<PostmanVariable> = emptyList()
)

@Serializable
data class PostmanInfo(
    @SerialName("_postman_id") val postmanId: String = "",
    val name: String,
    val description: String = "",
    val schema: String = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
)

@Serializable
data class PostmanItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    // If request is present, this is a leaf request item; if item is present, it's a folder
    val request: PostmanRequest? = null,
    val item: List<PostmanItem>? = null
)

@Serializable
data class PostmanRequest(
    val method: String = "GET",
    val header: List<PostmanHeader> = emptyList(),
    val body: PostmanBody? = null,
    val url: PostmanUrl,
    val description: String = ""
)

@Serializable
data class PostmanHeader(
    val key: String,
    val value: String,
    val description: String = "",
    val disabled: Boolean = false
)

@Serializable
data class PostmanUrl(
    val raw: String = "",
    val protocol: String = "",
    val host: List<String> = emptyList(),
    val path: List<String> = emptyList(),
    val query: List<PostmanQuery> = emptyList(),
    val variable: List<PostmanVariable> = emptyList()
)

@Serializable
data class PostmanQuery(
    val key: String = "",
    val value: String = "",
    val description: String = "",
    val disabled: Boolean = false
)

@Serializable
data class PostmanBody(
    val mode: String = "raw", // raw, urlencoded, formdata, file, graphql
    val raw: String = "",
    val urlencoded: List<PostmanFormParam>? = null,
    val formdata: List<PostmanFormParam>? = null,
    val options: PostmanBodyOptions? = null
)

@Serializable
data class PostmanBodyOptions(
    val raw: PostmanRawOptions? = null
)

@Serializable
data class PostmanRawOptions(
    val language: String = "json"
)

@Serializable
data class PostmanFormParam(
    val key: String = "",
    val value: String = "",
    val description: String = "",
    val disabled: Boolean = false,
    val type: String = "text"
)

@Serializable
data class PostmanVariable(
    val id: String = "",
    val key: String = "",
    val value: String = "",
    val description: String = "",
    val disabled: Boolean = false
)
