package com.inspekt.data.repository

import com.inspekt.data.model.PostmanEnvironment
import com.inspekt.domain.model.Environment
import com.inspekt.domain.model.EnvironmentVariable
import com.inspekt.domain.model.generateId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

class EnvironmentRepository(private val storageDir: String) {

    private val dirPath = "$storageDir/environments".toPath()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _environments = MutableStateFlow<List<Environment>>(emptyList())
    val environments: StateFlow<List<Environment>> = _environments

    private val _activeEnvironmentId = MutableStateFlow<String?>(null)
    val activeEnvironmentId: StateFlow<String?> = _activeEnvironmentId

    suspend fun loadAll() = withContext(Dispatchers.Default) {
        val loaded = mutableListOf<Environment>()
        if (FileSystem.SYSTEM.exists(dirPath)) {
            FileSystem.SYSTEM.list(dirPath)
                .filter { it.name.endsWith(".json") }
                .forEach { path ->
                    runCatching {
                        val text = FileSystem.SYSTEM.source(path).buffer().readUtf8()
                        json.decodeFromString<Environment>(text)
                    }.onSuccess { loaded.add(it) }
                }
        }
        _environments.value = loaded
        loadActiveId()
    }

    suspend fun save(environment: Environment) = withContext(Dispatchers.Default) {
        if (!FileSystem.SYSTEM.exists(dirPath)) {
            FileSystem.SYSTEM.createDirectories(dirPath)
        }
        val file = (dirPath / "${sanitizeFileName(environment.name)}_${environment.id.take(8)}.json")
        FileSystem.SYSTEM.sink(file).buffer().use {
            it.writeUtf8(json.encodeToString(environment))
        }

        val current = _environments.value.toMutableList()
        val index = current.indexOfFirst { it.id == environment.id }
        if (index >= 0) current[index] = environment else current.add(environment)
        _environments.value = current
    }

    suspend fun delete(environmentId: String) = withContext(Dispatchers.Default) {
        if (FileSystem.SYSTEM.exists(dirPath)) {
            FileSystem.SYSTEM.list(dirPath)
                .filter { it.name.contains(environmentId.take(8)) }
                .forEach { FileSystem.SYSTEM.delete(it) }
        }
        _environments.value = _environments.value.filter { it.id != environmentId }
        if (_activeEnvironmentId.value == environmentId) {
            _activeEnvironmentId.value = null
            saveActiveId()
        }
    }

    suspend fun setActive(environmentId: String?) = withContext(Dispatchers.Default) {
        _activeEnvironmentId.value = environmentId
        saveActiveId()
    }

    private fun saveActiveId() {
        val settingsFile = "$storageDir/settings.json".toPath()
        val settings = if (!FileSystem.SYSTEM.exists(settingsFile)) {
            """{"activeEnvironmentId":null}"""
        } else {
            FileSystem.SYSTEM.source(settingsFile).buffer().use { it.readUtf8() }
        }
        val updated = if (_activeEnvironmentId.value != null) {
            settings.replace(
                Regex("\"activeEnvironmentId\"\\s*:\\s*null"),
                "\"activeEnvironmentId\":\"${_activeEnvironmentId.value}\""
            ).ifEmpty {
                """{"activeEnvironmentId":"${_activeEnvironmentId.value}"}"""
            }
        } else {
            settings.replace(
                Regex("\"activeEnvironmentId\"\\s*:\\s*\"[^\"]*\""),
                "\"activeEnvironmentId\":null"
            )
        }
        if (!FileSystem.SYSTEM.exists(storageDir.toPath())) {
            FileSystem.SYSTEM.createDirectories(storageDir.toPath())
        }
        FileSystem.SYSTEM.sink(settingsFile).buffer().use { it.writeUtf8(updated) }
    }

    private fun loadActiveId() {
        val settingsFile = "$storageDir/settings.json".toPath()
        if (FileSystem.SYSTEM.exists(settingsFile)) {
            val text = FileSystem.SYSTEM.source(settingsFile).buffer().use { it.readUtf8() }
            val match = Regex("\"activeEnvironmentId\"\\s*:\\s*\"([^\"]*?)\"").find(text)
            _activeEnvironmentId.value = match?.groupValues?.get(1)
        }
    }

    suspend fun importFromJson(jsonString: String): Result<Environment> {
        return runCatching {
            val postmanEnv = json.decodeFromString<PostmanEnvironment>(jsonString)
            val environment = Environment(
                id = postmanEnv.id.ifBlank { generateId() },
                name = postmanEnv.name,
                variables = postmanEnv.values.map { v ->
                    EnvironmentVariable(
                        id = generateId(),
                        key = v.key,
                        value = v.value,
                        enabled = v.enabled,
                        description = v.description
                    )
                }
            )
            save(environment)
            environment
        }
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
}
