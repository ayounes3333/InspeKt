package com.inspekt.domain.model

import kotlinx.serialization.Serializable

/**
 * A collection item — either a folder or a request.
 * Mirrors the Postman Collection v2.1 structure.
 */
@Serializable
sealed class CollectionItem {
    abstract val id: String
    abstract val name: String
}

@Serializable
data class CollectionFolder(
    override val id: String = generateId(),
    override val name: String = "New Folder",
    val description: String = "",
    val items: List<CollectionItem> = emptyList()
) : CollectionItem()

@Serializable
data class CollectionRequest(
    override val id: String = generateId(),
    override val name: String = "New Request",
    val request: HttpRequest = HttpRequest(),
    val description: String = ""
) : CollectionItem()

/**
 * A top-level Postman-compatible collection.
 */
@Serializable
data class Collection(
    val id: String = generateId(),
    val name: String = "New Collection",
    val description: String = "",
    val items: List<CollectionItem> = emptyList(),
    val variables: List<KeyValueParam> = emptyList()
)
