package com.self.dshmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.self.dshmobile.data.ContainerManager
import com.self.dshmobile.data.SuResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 首次安装向导（方案 6.1）：引导用户完成环境检查与容器部署说明。
 * API Key 不再在向导内填写，而是引导去 dsh 网页「模型设置」填（用户拍板）。
 */
@Composable
fun WizardScreen(
    onFinish: () -> Unit,
    onSkipToChat: () -> Unit,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    // 环境检查结果：null=未检测，true=通过，false=不通过
    var rootOk by remember { mutableStateOf<Boolean?>(null) }
    var deployedOk by remember { mutableStateOf<Boolean?>(null) }
    var checking by remember { mutableStateOf(false) }
    // 内置容器部署进度：-1=空闲，0..100=部署中
    var deployProgress by remember { mutableIntStateOf(-1) }
    var deployError by remember { mutableStateOf<String?>(null) }

    when (step) {
        0 -> WelcomeStep(
            onNext = { step = 1 },
            onSkip = onSkipToChat,
        )
        1 -> EnvironmentStep(
            rootOk = rootOk,
            deployedOk = deployedOk,
            checking = checking,
            deployProgress = deployProgress,
            deployError = deployError,
            onCheck = {
                checking = true
                scope.launch {
                    rootOk = ContainerManager.hasRoot()
                    deployedOk = ContainerManager.isContainerDeployed()
                    checking = false
                }
            },
            onDeploy = {
                deployError = null
                ContainerManager.deployFromAssets(
                    context = context,
                    onProgress = { p -> deployProgress = p },
                    onDone = { r ->
                        when (r) {
                            is SuResult.Ok -> deployedOk = true
                            is SuResult.Fail -> deployError = r.message
                            SuResult.NoRoot -> deployError = "需要 root（KernelSU/Magisk）才能部署容器"
                        }
                        deployProgress = -1
                    },
                )
            },
            onNext = { step = 2 },
            onBack = { step = 0 },
        )
        else -> FinishStep(onFinish = onFinish)
    }
}

@Composable
private fun StepIndicator(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val active = i == current
            val done = i < current
            Box(
                Modifier
                    .size(if (active) 10.dp else 8.dp)
                    .background(
                        color = when {
                            done -> MaterialTheme.colorScheme.primary
                            active -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                    )
            )
        }
    }
}

@Composable
private fun WizardScaffold(
    title: String,
    subtitle: String,
    icon: ImageVector,
    indicator: Int,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepIndicator(indicator)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("上一步") }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, onSkip: () -> Unit) {
    WizardScaffold(
        title = "欢迎使用 dsh Mobile",
        subtitle = "把手机变成你的开发主机。\n在 Ubuntu 容器里跑 dsh，随时对话、跑命令、编译项目。",
        icon = Icons.Filled.PhoneAndroid,
        indicator = 0,
    ) {
        InfoCard(
            icon = Icons.Filled.Security,
            title = "需要已 Root 的手机",
            body = "本应用依赖 chroot 容器（KernelSU / Magisk 等 root 框架）。未 root 的设备只能浏览界面，无法启动容器。",
        )
        InfoCard(
            icon = Icons.Filled.CheckCircle,
            title = "内置 Ubuntu 环境",
            body = "Ubuntu 24.04（arm64）容器已随 App 内置，预装 Node.js 与 dsh，首次解压即用、无需下载，约占用 1–2 GB 空间。",
        )
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("开始部署")
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("跳过，直接进入（稍后部署）")
        }
    }
}

@Composable
private fun EnvironmentStep(
    rootOk: Boolean?,
    deployedOk: Boolean?,
    checking: Boolean,
    deployProgress: Int,
    deployError: String?,
    onCheck: () -> Unit,
    onDeploy: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    WizardScaffold(
        title = "环境检查",
        subtitle = "检查设备是否具备部署条件",
        icon = Icons.Filled.Security,
        indicator = 1,
        onBack = onBack,
    ) {
        if (rootOk == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("点击下方按钮开始检查：", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "· Root 权限（su）\n· 容器 rootfs 是否已部署",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
                if (checking) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("检测中…")
                } else {
                    Text("开始检测")
                }
            }
        } else {
            CheckRow(label = "Root 权限", ok = rootOk, detail = if (rootOk == true) "已获得 su 权限" else "未检测到 su（KernelSU/Magisk）")
            CheckRow(label = "容器 rootfs", ok = deployedOk, detail = when (deployedOk) {
                true -> "已部署（可在 设置 中重启容器）"
                false -> "尚未部署"
                null -> "未知"
            })

            // 有 root 但未部署：一键解压内置容器（无需下载）
            if (rootOk == true && deployedOk == false) {
                if (deployProgress < 0) {
                    Button(onClick = onDeploy, modifier = Modifier.fillMaxWidth()) {
                        Text("开始部署（内置 Ubuntu 容器）")
                    }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("正在解压内置容器… $deployProgress%", style = MaterialTheme.typography.bodyMedium)
                            LinearProgressIndicator(
                                progress = { deployProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (deployError != null) {
                    InfoCard(
                        icon = Icons.Filled.ErrorOutline,
                        title = "部署失败",
                        body = deployError!!,
                    )
                }
                InfoCard(
                    icon = Icons.Filled.CheckCircle,
                    title = "无需联网下载",
                    body = "Ubuntu 24.04（arm64）+ Node.js + dsh 已随 App 内置，解压即可用，约占用 1.5 GB 空间。",
                )
            }

            val allOk = rootOk == true && deployedOk == true
            if (!allOk) {
                InfoCard(
                    icon = Icons.Filled.ErrorOutline,
                    title = "还需配置 API Key",
                    body = "进入聊天页后，点击模型名称 → 「模型设置」，填入你的 DeepSeek API Key（存于 dsh 网页设置，向导内不再重复填写）。",
                )
            }
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(if (allOk) "完成，进入聊天" else "继续，进入聊天")
            }
        }
    }
}

@Composable
private fun FinishStep(onFinish: () -> Unit) {
    WizardScaffold(
        title = "准备就绪",
        subtitle = "基础配置完成，可以开始使用。",
        icon = Icons.Filled.CheckCircle,
        indicator = 2,
    ) {
        InfoCard(
            icon = Icons.Filled.Security,
            title = "接下来的事",
            body = "进入聊天页后，若容器未运行会提示启动。模型设置（API Key / 模型 / 思考强度）在 dsh 网页主体中完成。",
        )
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("进入 dsh Mobile")
        }
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean?, detail: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (ok == true) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = if (ok == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
