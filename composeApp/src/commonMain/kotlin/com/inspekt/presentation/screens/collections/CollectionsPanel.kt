package com.inspekt.presentation.screens.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspekt.domain.model.*
import com.inspekt.domain.model.Collection
import com.inspekt.presentation.components.MethodBadge
import com.inspekt.presentation.viewmodel.CollectionsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CollectionsPanel(
    onRequestSelected: (HttpRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: CollectionsViewModel = koinViewModel()
    val state by vm.uiState.collectAsState()

    // Dialogs
    if (state.showNewCollectionDialog) {
        NewCollectionDialog(
            name = state.newCollectionName,
            onNameChange = vm::updateNewCollectionName,
            onCreate = vm::createCollection,
            onDismiss = vm::dismissNewCollectionDialog
        )
    }

    if (state.showImportDialog) {
        ImportCollectionDialog(
            input = state.importJsonInput,
            onInputChange = vm::updateImportJson,
            onImport = vm::importCollection,
            onDismiss = vm::dismissImportDialog
        )
    }

    state.error?.let { err ->
        LaunchedEffect(err) {
            // Show snackbar or similar — for now just clear after a timeout
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Collections", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = vm::showNewCollectionDialog) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = vm::showImportDialog) {
                    Text("↓", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        HorizontalDivider()

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.collections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No collections yet", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::showNewCollectionDialog) { Text("Create Collection") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                state.collections.forEach { collection ->
                    item(key = collection.id) {
                        CollectionRow(
                            collection = collection,
                            expandedIds = state.expandedIds,
                            onToggle = vm::toggleFolder,
                            onRequestSelected = onRequestSelected,
                            onDelete = { vm.deleteCollection(collection.id) },
                            onExport = { vm.exportCollection(collection) }
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Collection Row (expandable)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun CollectionRow(
    collection: Collection,
    expandedIds: Set<String>,
    onToggle: (String) -> Unit,
    onRequestSelected: (HttpRequest) -> Unit,
    onDelete: () -> Unit,
    onExport: () -> String
) {
    val isExpanded = collection.id in expandedIds
    var menuExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf("") }

    if (showExportDialog) {
        ExportDialog(json = exportedJson, onDismiss = { showExportDialog = false })
    }

    Column {
        // Collection header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(collection.id) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isExpanded) "▼" else "▶",
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.labelMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(collection.name, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (collection.description.isNotBlank()) {
                    Text(collection.description, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Text("${collection.items.size} items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text("⋮")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Export (Postman JSON)") },
                        onClick = {
                            menuExpanded = false
                            exportedJson = onExport()
                            showExportDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }

        // Expanded items
        if (isExpanded) {
            collection.items.forEach { item ->
                CollectionItemRow(
                    item = item,
                    depth = 1,
                    expandedIds = expandedIds,
                    onToggle = onToggle,
                    onRequestSelected = onRequestSelected
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Composable
private fun CollectionItemRow(
    item: CollectionItem,
    depth: Int,
    expandedIds: Set<String>,
    onToggle: (String) -> Unit,
    onRequestSelected: (HttpRequest) -> Unit
) {
    val indent = (depth * 16).dp

    when (item) {
        is CollectionFolder -> {
            val isExpanded = item.id in expandedIds
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(item.id) }
                        .padding(start = indent, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isExpanded) "▼ " else "▶ ",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 8.dp))
                    Text(item.name, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isExpanded) {
                    item.items.forEach { child ->
                        CollectionItemRow(child, depth + 1, expandedIds, onToggle, onRequestSelected)
                    }
                }
            }
        }
        is CollectionRequest -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestSelected(item.request) }
                    .padding(start = indent, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MethodBadge(item.request.method.value)
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Dialogs
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun NewCollectionDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Collection") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Collection name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ImportCollectionDialog(
    input: String,
    onInputChange: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Postman Collection") },
        text = {
            Column {
                Text("Paste your Postman Collection v2.1 JSON:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        placeholder = { Text("{ \"info\": {...}, \"item\": [...] }") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onImport, enabled = input.isNotBlank()) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExportDialog(json: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Collection JSON") },
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                OutlinedTextField(
                    value = json,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
