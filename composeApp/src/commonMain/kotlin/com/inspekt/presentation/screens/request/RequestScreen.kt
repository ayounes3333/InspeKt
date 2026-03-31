package com.inspekt.presentation.screens.request

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inspekt.domain.model.*
import com.inspekt.presentation.components.*
import com.inspekt.presentation.screens.response.ResponsePanel
import com.inspekt.presentation.viewmodel.RequestTab
import com.inspekt.presentation.viewmodel.RequestUiState
import com.inspekt.presentation.viewmodel.RequestViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RequestScreen(
    modifier: Modifier = Modifier,
    onSaveToCollection: (HttpRequest) -> Unit = {}
) {
    val vm: RequestViewModel = koinViewModel()
    val state by vm.uiState.collectAsState()

    if (state.showCurlImportDialog) {
        CurlImportDialog(
            input = state.curlInput,
            onInputChange = vm::updateCurlInput,
            onImport = vm::importFromCurl,
            onDismiss = vm::dismissCurlImportDialog
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── URL Bar ─────────────────────────────────────────────────────────
        UrlBar(
            state = state,
            onMethodChange = vm::updateMethod,
            onUrlChange = vm::updateUrl,
            onSend = vm::sendRequest,
            onNewRequest = vm::newRequest,
            onImportCurl = vm::showCurlImportDialog,
            onSave = { onSaveToCollection(state.request) }
        )

        HorizontalDivider()

        // ── Tabs + Body ─────────────────────────────────────────────────────
        Row(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.weight(1f)) {
                RequestTabRow(
                    activeTab = state.activeTab,
                    onTabSelected = vm::setActiveTab
                )
                Box(modifier = Modifier.weight(1f)) {
                    when (state.activeTab) {
                        RequestTab.PARAMS -> QueryParamsTab(
                            params = state.request.queryParams,
                            onAdd = vm::addQueryParam,
                            onUpdate = vm::updateQueryParam,
                            onRemove = vm::removeQueryParam
                        )
                        RequestTab.HEADERS -> HeadersTab(
                            headers = state.request.headers,
                            onAdd = vm::addHeader,
                            onUpdate = vm::updateHeader,
                            onRemove = vm::removeHeader
                        )
                        RequestTab.BODY -> BodyTab(
                            body = state.request.body,
                            onTypeChange = vm::updateBodyType,
                            onRawChange = vm::updateBodyRaw,
                            onAddParam = vm::addBodyFormParam,
                            onUpdateParam = vm::updateBodyFormParam,
                            onRemoveParam = vm::removeBodyFormParam
                        )
                        RequestTab.AUTH -> AuthTab()
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Response ─────────────────────────────────────────────────────────
        ResponsePanel(
            isLoading = state.isLoading,
            response = state.response,
            error = state.error,
            modifier = Modifier.weight(1f)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrlBar(
    state: RequestUiState,
    onMethodChange: (HttpMethod) -> Unit,
    onUrlChange: (String) -> Unit,
    onSend: () -> Unit,
    onNewRequest: () -> Unit,
    onImportCurl: () -> Unit,
    onSave: () -> Unit
) {
    var methodMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        // Toolbar row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.request.name,
                onValueChange = {},
                label = { Text("Request name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            TextButton(onClick = onNewRequest) { Text("New") }
            TextButton(onClick = onImportCurl) { Text("Import cURL") }
            TextButton(onClick = onSave) { Text("Save") }
        }

        Spacer(Modifier.height(8.dp))

        // URL row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method dropdown
            Box {
                OutlinedButton(onClick = { methodMenuExpanded = true }) {
                    Text(state.request.method.value)
                }
                DropdownMenu(
                    expanded = methodMenuExpanded,
                    onDismissRequest = { methodMenuExpanded = false }
                ) {
                    HttpMethod.entries.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.value) },
                            onClick = {
                                onMethodChange(method)
                                methodMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // URL field
            OutlinedTextField(
                value = state.request.url,
                onValueChange = onUrlChange,
                placeholder = { Text("https://api.example.com/endpoint") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Send button
            Button(
                onClick = onSend,
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun RequestTabRow(
    activeTab: RequestTab,
    onTabSelected: (RequestTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = activeTab.ordinal,
        edgePadding = 0.dp
    ) {
        RequestTab.entries.forEach { tab ->
            Tab(
                selected = tab == activeTab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun QueryParamsTab(
    params: List<KeyValueParam>,
    onAdd: () -> Unit,
    onUpdate: (Int, KeyValueParam) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        params.forEachIndexed { index, param ->
            KeyValueRow(
                param = param,
                onUpdate = { onUpdate(index, it) },
                onRemove = { onRemove(index) },
                keyPlaceholder = "Param",
                valuePlaceholder = "Value"
            )
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("+ Add Param") }
    }
}

@Composable
private fun HeadersTab(
    headers: List<KeyValueParam>,
    onAdd: () -> Unit,
    onUpdate: (Int, KeyValueParam) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        headers.forEachIndexed { index, header ->
            KeyValueRow(
                param = header,
                onUpdate = { onUpdate(index, it) },
                onRemove = { onRemove(index) },
                keyPlaceholder = "Header",
                valuePlaceholder = "Value"
            )
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("+ Add Header") }
    }
}

@Composable
private fun BodyTab(
    body: RequestBody,
    onTypeChange: (BodyType) -> Unit,
    onRawChange: (String) -> Unit,
    onAddParam: () -> Unit,
    onUpdateParam: (Int, KeyValueParam) -> Unit,
    onRemoveParam: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Body type selector
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BodyType.entries.forEach { type ->
                FilterChip(
                    selected = body.type == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        HorizontalDivider()

        when (body.type) {
            BodyType.NONE -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No body", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            BodyType.JSON, BodyType.RAW_TEXT -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    CodeEditor(
                        value = body.rawContent,
                        onValueChange = onRawChange,
                        placeholder = if (body.type == BodyType.JSON)
                            "{\n  \"key\": \"value\"\n}" else "Enter raw text...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            BodyType.FORM_URL_ENCODED, BodyType.MULTIPART_FORM -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    body.formParams.forEachIndexed { index, param ->
                        KeyValueRow(
                            param = param,
                            onUpdate = { onUpdateParam(index, it) },
                            onRemove = { onRemoveParam(index) }
                        )
                    }
                    TextButton(
                        onClick = onAddParam,
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("+ Add Field") }
                }
            }
            BodyType.BINARY -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Binary file upload not yet supported in this version",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun AuthTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Authentication", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Add Authorization headers manually in the Headers tab.\n" +
                "Bearer token: Authorization: Bearer <token>\n" +
                "Basic auth: Authorization: Basic <base64>",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CurlImportDialog(
    input: String,
    onInputChange: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from cURL") },
        text = {
            Column {
                Text(
                    "Paste your cURL command below:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    CodeEditor(
                        value = input,
                        onValueChange = onInputChange,
                        placeholder = "curl -X POST https://api.example.com/data \\\n  -H 'Content-Type: application/json' \\\n  -d '{\"key\":\"value\"}'",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onImport, enabled = input.isNotBlank()) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
