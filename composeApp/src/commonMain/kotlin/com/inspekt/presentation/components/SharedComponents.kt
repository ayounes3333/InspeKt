package com.inspekt.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inspekt.domain.model.KeyValueParam
import com.inspekt.presentation.theme.InspeKtColors
import com.inspekt.presentation.theme.methodColor
import com.inspekt.presentation.theme.statusColor

/**
 * A single row for editing a key-value pair (header, query param, form field …).
 */
@Composable
fun KeyValueRow(
    param: KeyValueParam,
    onUpdate: (KeyValueParam) -> Unit,
    onRemove: () -> Unit,
    keyPlaceholder: String = "Key",
    valuePlaceholder: String = "Value",
    modifier: Modifier = Modifier,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = InspeKtColors.Surface1,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = InspeKtColors.Surface0.copy(alpha = 0.25f),
        unfocusedContainerColor = InspeKtColors.Surface0.copy(alpha = 0.15f),
    )

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Checkbox(
            checked = param.enabled,
            onCheckedChange = { onUpdate(param.copy(enabled = it)) },
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = InspeKtColors.Surface2,
            ),
        )

        OutlinedTextField(
            value = param.key,
            onValueChange = { onUpdate(param.copy(key = it)) },
            placeholder = { Text(keyPlaceholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            colors = fieldColors,
            shape = MaterialTheme.shapes.small,
        )

        OutlinedTextField(
            value = param.value,
            onValueChange = { onUpdate(param.copy(value = it)) },
            placeholder = { Text(valuePlaceholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            colors = fieldColors,
            shape = MaterialTheme.shapes.small,
        )

        OutlinedTextField(
            value = param.description,
            onValueChange = { onUpdate(param.copy(description = it)) },
            placeholder = { Text("Description", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(0.7f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            colors = fieldColors,
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

/**
 * A monospace text editor used for JSON bodies and response display.
 */
@Composable
fun CodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    placeholder: String = "",
) {
    val colors = MaterialTheme.colorScheme

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface,
        ),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = InspeKtColors.Overlay0,
                        ),
                    )
                }
                inner()
            }
        },
        modifier = modifier,
    )
}

/**
 * HTTP method badge chip — colour-coded.
 */
@Composable
fun MethodBadge(method: String) {
    val color = methodColor(method)
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            method.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * HTTP status code badge — colour-coded.
 */
@Composable
fun StatusBadge(statusCode: Int, statusText: String) {
    val color = statusColor(statusCode)
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            "$statusCode $statusText",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * A section header with optional trailing action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = InspeKtColors.Subtext0)
        action?.invoke()
    }
}
