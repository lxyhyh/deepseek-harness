package com.self.dshmobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.self.dshmobile.data.AppSettings
import com.self.dshmobile.data.ContainerManager
import com.self.dshmobile.ui.theme.DarkMode
import com.self.dshmobile.ui.theme.DshMobileTheme
import kotlinx.coroutines.launch

/** 应用内路由（单 Activity + 状态导航） */
sealed class Route(val path: String) {
    data object Chat : Route("chat")
    data object Wizard : Route("wizard")
    data object Settings : Route("settings")
    data object Tools : Route("tools")
    data object EnvVars : Route("envvars")
    data object Backup : Route("backup")
}

@Composable
fun App() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var darkMode by remember { mutableStateOf(DarkMode.valueOf(settings.darkMode)) }
    var initialized by remember { mutableStateOf(settings.initialized) }
    var route by remember { mutableStateOf(if (settings.initialized) Route.Chat else Route.Wizard) }
    var drawerOpen by remember { mutableStateOf(false) }
    var containerRunning by remember { mutableStateOf(ContainerManager.isContainerRunning()) }

    DshMobileTheme(darkMode) {
        // 非聊天/向导页按返回键回聊天
        BackHandler(enabled = route !is Route.Chat && route !is Route.Wizard) {
            route = Route.Chat
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            val baseModifier = Modifier.padding(innerPadding)
            when (route) {
                is Route.Wizard -> WizardScreen(
                    onFinish = {
                        settings.initialized = true
                        initialized = true
                        route = Route.Chat
                    },
                    onSkipToChat = {
                        settings.initialized = true
                        initialized = true
                        route = Route.Chat
                    },
                    scope = scope,
                )
                is Route.Settings -> SettingsScreen(
                    settings = settings,
                    onBack = { route = Route.Chat },
                )
                is Route.Tools -> ToolsScreen(
                    settings = settings,
                    onBack = { route = Route.Chat },
                    scope = scope,
                )
                is Route.EnvVars -> EnvVarsScreen(
                    settings = settings,
                    onBack = { route = Route.Chat },
                )
                is Route.Backup -> BackupScreen(
                    settings = settings,
                    onBack = { route = Route.Chat },
                    scope = scope,
                    snackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                )
                else -> ChatScreen(
                    modifier = baseModifier,
                    containerRunning = containerRunning,
                    drawerOpen = drawerOpen,
                    onDrawerChange = { drawerOpen = it },
                    onNavigate = { route = it },
                    onRefreshContainer = { containerRunning = ContainerManager.isContainerRunning() },
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                )
            }
        }
    }
}
