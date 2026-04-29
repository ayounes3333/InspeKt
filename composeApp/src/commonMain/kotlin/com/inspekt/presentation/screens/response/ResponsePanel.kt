package com.inspekt.presentation.screens.response

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspekt.domain.model.HttpResponse
import com.inspekt.presentation.components.CodeEditor
import com.inspekt.presentation.components.StatusBadge
import com.inspekt.presentation.theme.InspeKtColors

enum class ResponseTab { BODY, HEADERS, INFO }

@Composable
fun ResponsePanel(
    isLoading: Boolean,
    response: HttpResponse?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Status bar
        ResponseStatusBar(isLoading, response, error)

        HorizontalDivider(color = InspeKtColors.Surface0)

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sending request…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InspeKtColors.Subtext0,
                        )
                    }
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = InspeKtColors.Red.copy(alpha = 0.10f),
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = InspeKtColors.Red,
                                modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(
                                    "Request Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InspeKtColors.Red,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InspeKtColors.Subtext1,
                                )
                            }
                        }
                    }
                }
            }
            response != null -> ResponseContent(response = response, modifier = Modifier)
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "↗",
                            style = MaterialTheme.typography.displaySmall,
                            color = InspeKtColors.Surface2,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Send a request to see the response",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InspeKtColors.Overlay0,
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Status bar
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResponseStatusBar(
    isLoading: Boolean,
    response: HttpResponse?,
    error: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = InspeKtColors.Mantle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Response",
                style = MaterialTheme.typography.titleSmall,
                color = InspeKtColors.Subtext0,
            )

            response?.let { r ->
                StatusBadge(r.statusCode, r.statusText)

                Text(
                    "${r.durationMs} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = InspeKtColors.Subtext0,
                )
                Text(
                    formatSize(r.sizeBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = InspeKtColors.Subtext0,
                )
            }

            if (isLoading) {
                Spacer(Modifier.weight(1f))
                LinearProgressIndicator(
                    modifier = Modifier.weight(1f).height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = InspeKtColors.Surface0,
                )
            }

            if (error != null) {
                Surface(
                    color = InspeKtColors.Red.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Text(
                        "Error",
                        color = InspeKtColors.Red,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Response content (tabs)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResponseContent(response: HttpResponse, modifier: Modifier) {
    var activeTab by remember { mutableStateOf(ResponseTab.BODY) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = InspeKtColors.Surface0) },
        ) {
            ResponseTab.entries.forEach { tab ->
                Tab(
                    selected = tab == activeTab,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            tab.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (tab == activeTab) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = InspeKtColors.Overlay1,
                )
            }
        }

        when (activeTab) {
            ResponseTab.BODY    -> ResponseBodyTab(response)
            ResponseTab.HEADERS -> ResponseHeadersTab(response)
            ResponseTab.INFO    -> ResponseInfoTab(response)
        }
    }
}

@Composable
private fun ResponseBodyTab(response: HttpResponse) {
    val prettyBody = remember(response.body) { prettyPrintJson(response.body) }

    Surface(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        color = InspeKtColors.Surface0.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
    ) {
        CodeEditor(
            value = prettyBody,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ResponseHeadersTab(response: HttpResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        response.headers.entries.sortedBy { it.key }.forEachIndexed { index, (key, value) ->
            Surface(
                color = if (index % 2 == 0) Color.Transparent
                        else InspeKtColors.Surface0.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        key,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(200.dp),
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodySmall,
                        color = InspeKtColors.Text,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseInfoTab(response: HttpResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InfoRow("Status", "${response.statusCode} ${response.statusText}")
        InfoRow("Duration", "${response.durationMs} ms")
        InfoRow("Size", formatSize(response.sizeBytes))
        InfoRow("Content-Type", response.headers["content-type"]
            ?: response.headers["Content-Type"] ?: "unknown")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Surface(
        color = InspeKtColors.Surface0.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = InspeKtColors.Subtext0,
                modifier = Modifier.width(120.dp),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = InspeKtColors.Text,
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}

/**
 * Attempts to pretty-print a JSON string. Returns the original string if not valid JSON.
 */
private fun prettyPrintJson(raw: String): String {
    if (raw.isBlank()) return raw
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return raw

    return try {
        val sb = StringBuilder()
        var indent = 0
        var inString = false
        var i = 0

        while (i < trimmed.length) {
            val c = trimmed[i]
            when {
                c == '"' && (i == 0 || trimmed[i - 1] != '\\') -> {
                    inString = !inString
                    sb.append(c)
                }
                inString -> sb.append(c)
                c == '{' || c == '[' -> {
                    sb.append(c).append('\n')
                    indent++
                    sb.append("  ".repeat(indent))
                }
                c == '}' || c == ']' -> {
                    sb.append('\n')
                    indent--
                    sb.append("  ".repeat(indent)).append(c)
                }
                c == ',' -> {
                    sb.append(c).append('\n').append("  ".repeat(indent))
                }
                c == ':' -> sb.append(c).append(' ')
                c != ' ' && c != '\n' && c != '\r' && c != '\t' -> sb.append(c)
            }
            i++
        }
        sb.toString()
    } catch (e: Exception) {
        raw
    }
}
