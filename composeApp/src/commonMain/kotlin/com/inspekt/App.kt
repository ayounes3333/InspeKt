package com.inspekt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inspekt.presentation.navigation.AppDestination
import com.inspekt.presentation.screens.collections.CollectionsPanel
import com.inspekt.presentation.screens.environments.EnvironmentsPanel
import com.inspekt.presentation.screens.request.RequestScreen
import com.inspekt.presentation.viewmodel.RequestViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root composable of the InspeKt application.
 * On desktop it shows a two-panel split layout (collections + request).
 * On mobile (smaller screen) it uses a bottom navigation bar.
 */
@Composable
fun App(isDesktop: Boolean = false) {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (isDesktop) {
                DesktopLayout()
            } else {
                MobileLayout()
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Desktop: side-by-side panels
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun DesktopLayout() {
    val requestVm: RequestViewModel = koinViewModel()
    var leftTab by remember { mutableStateOf(0) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel: Collections + Environments tabs
        Surface(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight(),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = leftTab) {
                    Tab(
                        selected = leftTab == 0,
                        onClick = { leftTab = 0 },
                        text = { Text("Collections") }
                    )
                    Tab(
                        selected = leftTab == 1,
                        onClick = { leftTab = 1 },
                        text = { Text("Environments") }
                    )
                }

                when (leftTab) {
                    0 -> CollectionsPanel(
                        onRequestSelected = { request -> requestVm.loadRequest(request) }
                    )
                    1 -> EnvironmentsPanel()
                }
            }
        }

        VerticalDivider()

        // Right panel: Request + Response
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            RequestScreen(
                onSaveToCollection = { /* TODO: Show collection picker dialog */ }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Mobile: bottom navigation
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MobileLayout() {
    val requestVm: RequestViewModel = koinViewModel()
    var currentDestination by remember { mutableStateOf(AppDestination.REQUEST) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination == AppDestination.REQUEST,
                    onClick = { currentDestination = AppDestination.REQUEST },
                    icon = { Text("⚡") },
                    label = { Text("Request") }
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.COLLECTIONS,
                    onClick = { currentDestination = AppDestination.COLLECTIONS },
                    icon = { Text("") },
                    label = { Text("Collections") }
                )
                NavigationBarItem(
                    selected = currentDestination == AppDestination.ENVIRONMENTS,
                    onClick = { currentDestination = AppDestination.ENVIRONMENTS },
                    icon = { Text("⚙") },
                    label = { Text("Environments") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentDestination) {
                AppDestination.REQUEST -> RequestScreen(
                    onSaveToCollection = { /* TODO: Show collection picker dialog */ }
                )
                AppDestination.COLLECTIONS -> CollectionsPanel(
                    onRequestSelected = { request ->
                        requestVm.loadRequest(request)
                        currentDestination = AppDestination.REQUEST
                    }
                )
                AppDestination.ENVIRONMENTS -> EnvironmentsPanel()
            }
        }
    }
}
