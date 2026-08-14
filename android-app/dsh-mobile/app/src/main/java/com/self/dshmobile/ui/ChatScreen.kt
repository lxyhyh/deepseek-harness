package com.self.dshmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.self.dshmobile.data.ContainerManager
import com.self.dshmobile.data.SuResult
import kotlinx.coroutines.launch

/**
 * ① 聊天主界面：原生顶栏（容器状态绿点 + 菜单）+ dsh 网页主体（见方案 7.8）。
 * 模型 / 思考强度 / 密钥等全部复用 dsh 网页主体，原生层只做壳与手机增强。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    containerRunning: Boolean,
    drawerOpen: Boolean,
    onDrawerChange: (Boolean) -> Unit,
    onNavigate: (Route) -> Unit,
    onRefreshContainer: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val drawerState = rememberDrawerState(if (drawerOpen) DrawerValue.Open else DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(16.dp)) {
                    Text("dsh Mobile", style = MaterialTheme.typography.titleLarge)
                    Text("手机即主机 · 容器状态：${if (containerRunning) "运行中" else "未运行"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                DrawerItem(Icons.Filled.Settings, "设置", "深色模式 / 下载源 / 模型入口") {
                    onDrawerChange(false); onNavigate(Route.Settings)
                }
                DrawerItem(Icons.Filled.Build, "工具管理", "SDK/NDK 安装 / 一键更新") {
                    onDrawerChange(false); onNavigate(Route.Tools)
                }
                DrawerItem(Icons.Filled.Language, "环境变量", "编译环境键值编辑") {
                    onDrawerChange(false); onNavigate(Route.EnvVars)
                }
                DrawerItem(Icons.Filled.Archive, "备份与恢复", "一键备份 / 历史恢复") {
                    onDrawerChange(false); onNavigate(Route.Backup)
                }
                HorizontalDivider()
                DrawerItem(Icons.Filled.Refresh, "重启容器", "停止并重新拉起 dsh 服务") {
                    onDrawerChange(false)
                    scope.launch {
                        snackbarHostState.showSnackbar("正在重启容器…")
                        ContainerManager.restartContainer { result ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    when (result) {
                                        is SuResult.NoRoot -> "需要 root（KernelSU/Magisk）才能操作容器"
                                        is SuResult.Fail -> "重启失败：${result.message}"
                                        is SuResult.Ok -> "容器已重启"
                                    }
                                )
                                onRefreshContainer()
                            }
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("dsh Mobile") },
                    actions = {
                        // 容器状态绿点：运行中=绿，未运行=灰（方案 7.8 ①）
                        Box(
                            Modifier
                                .padding(end = 8.dp)
                                .size(10.dp)
                                .background(
                                    color = if (containerRunning) Color(0xFF2E7D32) else Color(0xFF9E9E9E),
                                    shape = CircleShape,
                                )
                        )
                        IconButton(onClick = { onDrawerChange(true) }) {
                            Icon(Icons.Filled.Menu, contentDescription = "菜单")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (containerRunning) {
                    // 网页主体：一切交互复用 dsh（对话 / 模型 / 思考强度 / 文件发送由原生桥接补齐）
                    DshWebView(Modifier.fillMaxSize())
                } else {
                    ContainerOffline(onStart = {
                        scope.launch {
                            snackbarHostState.showSnackbar("正在启动容器…（需要 root）")
                            ContainerManager.startContainer { result ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        when (result) {
                                            is SuResult.NoRoot -> "需要 root 设备才能启动容器"
                                            is SuResult.Fail -> "启动失败：${result.message}"
                                            is SuResult.Ok -> "容器已启动"
                                        }
                                    )
                                    onRefreshContainer()
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(title)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
    )
}

/** 容器未运行时提示页（未 root 设备 / 首次安装后未启动均会看到） */
@Composable
private fun ContainerOffline(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("容器未运行", style = MaterialTheme.typography.titleLarge)
                Text(
                    "dsh 的 Ubuntu 环境还没启动。启动后即可在手机上对话、跑命令、编译项目。\n" +
                        "（此操作需要已 root 的手机：KernelSU / Magisk 等框架）",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onStart, modifier = Modifier.align(Alignment.End)) {
                    Text("启动容器")
                }
            }
        }
    }
}
