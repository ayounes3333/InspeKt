package com.inspekt.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inspekt.data.repository.EnvironmentRepository
import com.inspekt.domain.model.Environment
import com.inspekt.domain.model.EnvironmentVariable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EnvironmentsUiState(
    val environments: List<Environment> = emptyList(),
    val activeEnvironmentId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showNewDialog: Boolean = false,
    val newName: String = "",
    val showEditDialog: Boolean = false,
    val editingEnvironment: Environment? = null,
    val editName: String = "",
    val showImportDialog: Boolean = false,
    val importJsonInput: String = ""
) {
    val activeEnvironment: Environment?
        get() = environments.firstOrNull { it.id == activeEnvironmentId }
}

class EnvironmentsViewModel(
    private val repository: EnvironmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnvironmentsUiState())
    val uiState: StateFlow<EnvironmentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.environments.collect { envs ->
                _uiState.update { it.copy(environments = envs) }
            }
        }
        viewModelScope.launch {
            repository.activeEnvironmentId.collect { id ->
                _uiState.update { it.copy(activeEnvironmentId = id) }
            }
        }
        loadEnvironments()
    }

    private fun loadEnvironments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.loadAll()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ── Active Environment ─────────────────────────────────────────────────

    fun setActiveEnvironment(environmentId: String?) {
        viewModelScope.launch {
            repository.setActive(environmentId)
        }
    }

    // ── New Environment ────────────────────────────────────────────────────

    fun showNewDialog() {
        _uiState.update { it.copy(showNewDialog = true, newName = "") }
    }

    fun dismissNewDialog() {
        _uiState.update { it.copy(showNewDialog = false, newName = "") }
    }

    fun updateNewName(name: String) {
        _uiState.update { it.copy(newName = name) }
    }

    fun createEnvironment() {
        val name = _uiState.value.newName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val environment = Environment(name = name)
            repository.save(environment)
            _uiState.update { it.copy(showNewDialog = false, newName = "") }
        }
    }

    // ── Edit Environment ───────────────────────────────────────────────────

    fun showEditDialog(environment: Environment) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editingEnvironment = environment,
                editName = environment.name
            )
        }
    }

    fun dismissEditDialog() {
        _uiState.update {
            it.copy(showEditDialog = false, editingEnvironment = null, editName = "")
        }
    }

    fun updateEditName(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun saveEditedEnvironment() {
        val editing = _uiState.value.editingEnvironment ?: return
        val name = _uiState.value.editName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val updated = editing.copy(name = name)
            repository.save(updated)
            _uiState.update {
                it.copy(showEditDialog = false, editingEnvironment = null, editName = "")
            }
        }
    }

    // ── Variables ──────────────────────────────────────────────────────────

    fun addVariable(environmentId: String) {
        viewModelScope.launch {
            val env = _uiState.value.environments.firstOrNull { it.id == environmentId }
                ?: return@launch
            val updated = env.copy(variables = env.variables + EnvironmentVariable())
            repository.save(updated)
        }
    }

    fun updateVariable(environmentId: String, index: Int, variable: EnvironmentVariable) {
        viewModelScope.launch {
            val env = _uiState.value.environments.firstOrNull { it.id == environmentId }
                ?: return@launch
            val vars = env.variables.toMutableList()
            if (index < vars.size) {
                vars[index] = variable
                repository.save(env.copy(variables = vars))
            }
        }
    }

    fun removeVariable(environmentId: String, index: Int) {
        viewModelScope.launch {
            val env = _uiState.value.environments.firstOrNull { it.id == environmentId }
                ?: return@launch
            val vars = env.variables.toMutableList()
            if (index < vars.size) {
                vars.removeAt(index)
                repository.save(env.copy(variables = vars))
            }
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    fun deleteEnvironment(environmentId: String) {
        viewModelScope.launch {
            repository.delete(environmentId)
        }
    }

    // ── Import ─────────────────────────────────────────────────────────────

    fun showImportDialog() {
        _uiState.update { it.copy(showImportDialog = true, importJsonInput = "") }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(showImportDialog = false, importJsonInput = "") }
    }

    fun updateImportJson(json: String) {
        _uiState.update { it.copy(importJsonInput = json) }
    }

    fun importEnvironment() {
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
