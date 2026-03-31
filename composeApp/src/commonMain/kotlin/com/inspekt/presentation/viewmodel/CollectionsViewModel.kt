package com.inspekt.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inspekt.data.repository.CollectionRepository
import com.inspekt.domain.model.*
import com.inspekt.domain.model.Collection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CollectionsUiState(
    val collections: List<Collection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNewCollectionDialog: Boolean = false,
    val newCollectionName: String = "",
    val showImportDialog: Boolean = false,
    val importJsonInput: String = "",
    val expandedIds: Set<String> = emptySet()
)

class CollectionsViewModel(
    private val repository: CollectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.collections.collect { cols ->
                _uiState.update { it.copy(collections = cols) }
            }
        }
        loadCollections()
    }

    private fun loadCollections() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.loadAll()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleFolder(id: String) {
        _uiState.update {
            val expanded = it.expandedIds.toMutableSet()
            if (id in expanded) expanded.remove(id) else expanded.add(id)
            it.copy(expandedIds = expanded)
        }
    }

    // ── New Collection ───────────────────────────────────────────────────────

    fun showNewCollectionDialog() {
        _uiState.update { it.copy(showNewCollectionDialog = true, newCollectionName = "") }
    }

    fun dismissNewCollectionDialog() {
        _uiState.update { it.copy(showNewCollectionDialog = false, newCollectionName = "") }
    }

    fun updateNewCollectionName(name: String) {
        _uiState.update { it.copy(newCollectionName = name) }
    }

    fun createCollection() {
        val name = _uiState.value.newCollectionName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val collection = Collection(name = name)
            repository.save(collection)
            _uiState.update { it.copy(showNewCollectionDialog = false, newCollectionName = "") }
        }
    }

    // ── Save request to collection ───────────────────────────────────────────

    fun saveRequestToCollection(collectionId: String, request: HttpRequest) {
        viewModelScope.launch {
            val collection = _uiState.value.collections.firstOrNull { it.id == collectionId }
                ?: return@launch
            val newItem = CollectionRequest(request = request, name = request.name)
            val updated = collection.copy(items = collection.items + newItem)
            repository.save(updated)
        }
    }

    fun deleteRequestFromCollection(collectionId: String, requestId: String) {
        viewModelScope.launch {
            val collection = _uiState.value.collections.firstOrNull { it.id == collectionId }
                ?: return@launch
            val updated = collection.copy(
                items = collection.items.filter { item ->
                    when (item) {
                        is CollectionRequest -> item.id != requestId
                        is CollectionFolder -> true
                    }
                }
            )
            repository.save(updated)
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            repository.delete(collectionId)
        }
    }

    // ── Import / Export ──────────────────────────────────────────────────────

    fun showImportDialog() {
        _uiState.update { it.copy(showImportDialog = true, importJsonInput = "") }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(showImportDialog = false, importJsonInput = "") }
    }

    fun updateImportJson(json: String) {
        _uiState.update { it.copy(importJsonInput = json) }
    }

    fun importCollection() {
        val json = _uiState.value.importJsonInput
        viewModelScope.launch {
            repository.importFromJson(json).fold(
                onSuccess = {
                    _uiState.update { it.copy(showImportDialog = false, importJsonInput = "", error = null) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(error = "Import failed: ${err.message}") }
                }
            )
        }
    }

    fun exportCollection(collection: Collection): String =
        repository.exportToJson(collection)

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
