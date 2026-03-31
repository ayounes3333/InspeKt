package com.inspekt.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inspekt.data.repository.HttpClientRepository
import com.inspekt.domain.model.*
import com.inspekt.util.CurlParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestUiState(
    val request: HttpRequest = HttpRequest(),
    val isLoading: Boolean = false,
    val response: HttpResponse? = null,
    val error: String? = null,
    val activeTab: RequestTab = RequestTab.PARAMS,
    val showCurlImportDialog: Boolean = false,
    val curlInput: String = ""
)

enum class RequestTab { PARAMS, HEADERS, BODY, AUTH }

class RequestViewModel(
    private val httpRepository: HttpClientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestUiState())
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    fun loadRequest(request: HttpRequest) {
        _uiState.update { it.copy(request = request, response = null, error = null) }
    }

    fun updateUrl(url: String) {
        _uiState.update { it.copy(request = it.request.copy(url = url)) }
    }

    fun updateMethod(method: HttpMethod) {
        _uiState.update { it.copy(request = it.request.copy(method = method)) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(request = it.request.copy(name = name)) }
    }

    // ── Query Params ────────────────────────────────────────────────────────

    fun addQueryParam() {
        _uiState.update {
            it.copy(request = it.request.copy(
                queryParams = it.request.queryParams + KeyValueParam()
            ))
        }
    }

    fun updateQueryParam(index: Int, param: KeyValueParam) {
        _uiState.update {
            val params = it.request.queryParams.toMutableList()
            if (index < params.size) params[index] = param
            it.copy(request = it.request.copy(queryParams = params))
        }
    }

    fun removeQueryParam(index: Int) {
        _uiState.update {
            val params = it.request.queryParams.toMutableList()
            if (index < params.size) params.removeAt(index)
            it.copy(request = it.request.copy(queryParams = params))
        }
    }

    // ── Headers ─────────────────────────────────────────────────────────────

    fun addHeader() {
        _uiState.update {
            it.copy(request = it.request.copy(
                headers = it.request.headers + KeyValueParam()
            ))
        }
    }

    fun updateHeader(index: Int, header: KeyValueParam) {
        _uiState.update {
            val headers = it.request.headers.toMutableList()
            if (index < headers.size) headers[index] = header
            it.copy(request = it.request.copy(headers = headers))
        }
    }

    fun removeHeader(index: Int) {
        _uiState.update {
            val headers = it.request.headers.toMutableList()
            if (index < headers.size) headers.removeAt(index)
            it.copy(request = it.request.copy(headers = headers))
        }
    }

    // ── Body ─────────────────────────────────────────────────────────────────

    fun updateBodyType(type: BodyType) {
        _uiState.update {
            it.copy(request = it.request.copy(body = it.request.body.copy(type = type)))
        }
    }

    fun updateBodyRaw(content: String) {
        _uiState.update {
            it.copy(request = it.request.copy(body = it.request.body.copy(rawContent = content)))
        }
    }

    fun addBodyFormParam() {
        _uiState.update {
            it.copy(request = it.request.copy(
                body = it.request.body.copy(
                    formParams = it.request.body.formParams + KeyValueParam()
                )
            ))
        }
    }

    fun updateBodyFormParam(index: Int, param: KeyValueParam) {
        _uiState.update {
            val params = it.request.body.formParams.toMutableList()
            if (index < params.size) params[index] = param
            it.copy(request = it.request.copy(body = it.request.body.copy(formParams = params)))
        }
    }

    fun removeBodyFormParam(index: Int) {
        _uiState.update {
            val params = it.request.body.formParams.toMutableList()
            if (index < params.size) params.removeAt(index)
            it.copy(request = it.request.copy(body = it.request.body.copy(formParams = params)))
        }
    }

    // ── Tab ─────────────────────────────────────────────────────────────────

    fun setActiveTab(tab: RequestTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ── Send ─────────────────────────────────────────────────────────────────

    fun sendRequest() {
        val req = _uiState.value.request
        if (req.url.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, response = null) }
            httpRepository.execute(req).fold(
                onSuccess = { response ->
                    _uiState.update { it.copy(isLoading = false, response = response) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Unknown error") }
                }
            )
        }
    }

    // ── cURL Import ──────────────────────────────────────────────────────────

    fun showCurlImportDialog() {
        _uiState.update { it.copy(showCurlImportDialog = true) }
    }

    fun dismissCurlImportDialog() {
        _uiState.update { it.copy(showCurlImportDialog = false, curlInput = "") }
    }

    fun updateCurlInput(input: String) {
        _uiState.update { it.copy(curlInput = input) }
    }

    fun importFromCurl() {
        val curl = _uiState.value.curlInput
        CurlParser.parse(curl).fold(
            onSuccess = { request ->
                _uiState.update {
                    it.copy(request = request, showCurlImportDialog = false, curlInput = "")
                }
            },
            onFailure = { err ->
                _uiState.update { it.copy(error = "cURL parse error: ${err.message}") }
            }
        )
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    fun newRequest() {
        _uiState.update { RequestUiState() }
    }
}
