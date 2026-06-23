package com.inspekt.presentation.screens.environments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspekt.domain.model.Environment
import com.inspekt.domain.model.EnvironmentVariable
import com.inspekt.presentation.theme.InspeKtColors
import com.inspekt.presentation.viewmodel.EnvironmentsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EnvironmentsPanel(
    modifier: Modifier = Modifier
) {
    val vm: EnvironmentsViewModel = koinViewModel()
    val state by vm.uiState.collectAsState()

    if (state.showNewDialog) {
        NewEnvironmentDialog(
            name = state.newName,
            onNameChange = vm::updateNewName,
            onCreate = vm::createEnvironment,
            onDismiss = vm::dismissNewDialog
        )
    }

    if (state.showImportDialog) {
        ImportEnvironmentDialog(
            input = state.importJsonInput,
            onInputChange = vm::updateImportJson,
            onImport = vm::importEnvironment,
            onDismiss = vm::dismissImportDialog
        )
    }

    if (state.showEditDialog && state.editingEnvironment != null) {
        EditEnvironmentDialog(
            name = state.editName,
            onNameChange = vm::updateEditName,
            environment = state.editingEnvironment!!,
            onAddVariable = { vm.addVariable(state.editingEnvironment!!.id) },
            onUpdateVariable = { idx, v -> vm.updateVariable(state.editingEnvironment!!.id, idx, v) },
            onRemoveVariable = { idx -> vm.removeVariable(state.editingEnvironment!!.id, idx) },
            onSave = vm::saveEditedEnvironment,
            onDismiss = vm::dismissEditDialog
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Environments", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = vm::showNewDialog) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = vm::showImportDialog) {
                    Text("\u2193", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        HorizontalDivider()

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.environments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No environments yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::showNewDialog) { Text("Create Environment") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(state.environments, key = { _, env -> env.id }) { _, environment ->
                    EnvironmentRow(
                        environment = environment,
                        isActive = environment.id == state.activeEnvironmentId,
                        onSelect = { vm.setActiveEnvironment(environment.id) },
                        onEdit = { vm.showEditDialog(environment) },
                        onDelete = { vm.deleteEnvironment(environment.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvironmentRow(
    environment: Environment,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isActive,
                onClick = onSelect,
                modifier = Modifier.size(20.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = InspeKtColors.Surface2
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    environment.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${environment.variables.size} variables",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text("⋮")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Composable
private fun NewEnvironmentDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Environment") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Environment name") },
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
private fun EditEnvironmentDialog(
    name: String,
    onNameChange: (String) -> Unit,
    environment: Environment,
    onAddVariable: () -> Unit,
    onUpdateVariable: (Int, EnvironmentVariable) -> Unit,
    onRemoveVariable: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = InspeKtColors.Surface1,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = InspeKtColors.Surface0.copy(alpha = 0.25f),
        unfocusedContainerColor = InspeKtColors.Surface0.copy(alpha = 0.15f),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Environment") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Environment name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Variables", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onAddVariable) { Text("+ Add") }
                }

                if (environment.variables.isEmpty()) {
                    Text(
                        "No variables defined",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(environment.variables, key = { _, v -> v.id }) { index, variable ->
                            EnvironmentVariableRow(
                                variable = variable,
                                onUpdate = { onUpdateVariable(index, it) },
                                onRemove = { onRemoveVariable(index) },
                                colors = fieldColors
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EnvironmentVariableRow(
    variable: EnvironmentVariable,
    onUpdate: (EnvironmentVariable) -> Unit,
    onRemove: () -> Unit,
    colors: TextFieldColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(
            checked = variable.enabled,
            onCheckedChange = { onUpdate(variable.copy(enabled = it)) },
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = InspeKtColors.Surface2,
            )
        )

        OutlinedTextField(
            value = variable.key,
            onValueChange = { onUpdate(variable.copy(key = it)) },
            placeholder = { Text("Variable", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            colors = colors,
            shape = MaterialTheme.shapes.small,
        )

        OutlinedTextField(
            value = variable.value,
            onValueChange = { onUpdate(variable.copy(value = it)) },
            placeholder = { Text("Value", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            colors = colors,
            shape = MaterialTheme.shapes.small,
        )

        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove",
                tint = InspeKtColors.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ImportEnvironmentDialog(
    input: String,
    onInputChange: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Environment from JSON") },
        text = {
            Column {
                Text("Paste your Postman Environment JSON:", style = MaterialTheme.typography.bodyMedium)
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
                        placeholder = { Text("{ \"name\": \"...\", \"values\": [...] }") }
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
