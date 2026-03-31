package com.inspekt.data.repository

import com.inspekt.domain.model.Collection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

/**
 * Manages persistence and in-memory state of all collections.
 * Collections are stored as individual Postman-compatible JSON files on disk.
 */
class CollectionRepository(private val storageDir: String) {

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections

    suspend fun loadAll() = withContext(Dispatchers.Default) {
        val dir = storageDir.toPath()
        val loaded = mutableListOf<Collection>()
        if (FileSystem.SYSTEM.exists(dir)) {
            FileSystem.SYSTEM.list(dir)
                .filter { it.name.endsWith(".json") }
                .forEach { path ->
                    val text = FileSystem.SYSTEM.source(path).buffer().readUtf8()
                    PostmanSerializer.importCollection(text)
                        .onSuccess { loaded.add(it) }
                }
        }
        _collections.value = loaded
    }

    suspend fun save(collection: Collection) = withContext(Dispatchers.Default) {
        val dir = storageDir.toPath()
        if (!FileSystem.SYSTEM.exists(dir)) {
            FileSystem.SYSTEM.createDirectories(dir)
        }
        val file = (storageDir + "/${sanitizeFileName(collection.name)}_${collection.id.take(8)}.json").toPath()
        val jsonText = PostmanSerializer.exportCollection(collection)
        FileSystem.SYSTEM.sink(file).buffer().use { it.writeUtf8(jsonText) }

        // Update in-memory list
        val current = _collections.value.toMutableList()
        val index = current.indexOfFirst { it.id == collection.id }
        if (index >= 0) current[index] = collection else current.add(collection)
        _collections.value = current
    }

    suspend fun delete(collectionId: String) = withContext(Dispatchers.Default) {
        val current = _collections.value
        val target = current.firstOrNull { it.id == collectionId } ?: return@withContext

        // Remove the file
        val dir = storageDir.toPath()
        if (FileSystem.SYSTEM.exists(dir)) {
            FileSystem.SYSTEM.list(dir)
                .filter { it.name.contains(collectionId.take(8)) }
                .forEach { FileSystem.SYSTEM.delete(it) }
        }

        _collections.value = current.filter { it.id != collectionId }
    }

    suspend fun importFromJson(jsonText: String): Result<Collection> {
        val result = PostmanSerializer.importCollection(jsonText)
        result.onSuccess { save(it) }
        return result
    }

    fun exportToJson(collection: Collection): String =
        PostmanSerializer.exportCollection(collection)

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
}
