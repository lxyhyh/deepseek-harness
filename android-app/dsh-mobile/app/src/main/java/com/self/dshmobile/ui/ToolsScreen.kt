package com.self.dshmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.self.dshmobile.data.AppSettings
import com.self.dshmobile.data.ToolsCatalog
import com.self.dshmobile.data.mirrors
import com.self.dshmobile.data.Mirror
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 工具管理页（方案 7.2 / 7.8 ④）：
 * 首推组合 + 版本矩阵 + 可安装工具（版本下拉 + 安装）+ 下载源（一键全部测速）+ 可更新清单（一键更新）。
 */
@Composable
fun ToolsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    scope: CoroutineScope,
) {
    var mirrorKey by remember { mutableStateOf(settings.mirror) }
    // 测速结果：mirrorKey -> "xxx ms" / "超时" / 检测中
    var speedResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var testing by remember { mutableStateOf(false) }
    // 各工具选中版本（key -> 版本串）
    var selectedVersions by remember {
        mutableStateOf(ToolsCatalog.tools.associate { it.name to it.version })
    }

    PageScaffold(
        title = "工具管理",
        onBack = onBack,
        actions = {
            TextButton(
                enabled = !testing,
                onClick = {
                    testing = true
                    speedResults = emptyMap()
                    scope.launch {
                        val out = mutableMapOf<String, String>()
                        mirrors.forEach { m ->
                            out[m.key] = "检测中…"
                            speedResults = out.toMap()
                            out[m.key] = measureMirror(m)
                            speedResults = out.toMap()
                        }
                        testing = false
                    }
                },
            ) {
                Icon(
                    if (testing) Icons.Filled.Bolt else Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(if (testing) "测速中…" else "全部测速")
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 首推组合
            SectionTitle("推荐组合")
            InfoCard2(
                title = "首推稳定组合",
                body = "AGP ${ToolsCatalog.recommended.agp} · Gradle ${ToolsCatalog.recommended.gradle} · JDK ${ToolsCatalog.recommended.jdk} · " +
                    "Build-Tools ${ToolsCatalog.recommended.buildTools}${if (ToolsCatalog.recommended.ndk.isNotEmpty()) " · NDK ${ToolsCatalog.recommended.ndk}" else ""}\n" +
                    "兼容面广，aapt2 的 arm64 社区 drop-in 覆盖最稳。",
            )

            // 版本矩阵
            SectionTitle("版本兼容矩阵")
            Card2 {
                ToolsCatalog.matrix.forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider()
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(c.agp, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text(
                            "Gradle ${c.gradle} · JDK ${c.jdk} · Build-Tools ${c.buildTools}${c.ndk?.let { " · NDK $it" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 可安装工具
            SectionTitle("可安装工具")
            ToolsCatalog.tools.forEach { tool ->
                val selected = selectedVersions[tool.name] ?: tool.version
                ToolRow(
                    name = tool.name,
                    version = tool.version,
                    sizeMb = tool.sizeMb,
                    arm64 = tool.arm64,
                    source = tool.source,
                    installed = tool.installed,
                    selectedVersion = selected,
                    onVersionChange = { v -> selectedVersions = selectedVersions + (tool.name to v) },
                    onInstall = { /* 预留：调用容器内安装脚本 */ },
                )
            }

            // 下载源
            SectionTitle("下载源")
            Card2 {
                mirrors.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mirrorKey == m.key, onClick = {
                            mirrorKey = m.key
                            settings.mirror = m.key
                        })
                        Text(m.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            speedResults[m.key] ?: "未测速",
                            style = MaterialTheme.typography.bodySmall,
                            color = speedColor(speedResults[m.key]),
                        )
                    }
                }
                Text(
                    "测速通过 HTTP HEAD 到各源仓库测速；结果仅作参考，不代表完整下载速度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }

            // 可更新清单
            SectionTitle("可更新清单")
            Card2 {
                val updates = listOf(
                    UpdateItem("Build-Tools", "34.0.0 → 35.0.0", "约 60 MB"),
                    UpdateItem("Android NDK", "27 → 27c", "约 1.2 GB"),
                )
                updates.forEachIndexed { i, u ->
                    if (i > 0) HorizontalDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text("${u.oldToNew} · ${u.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { /* 预留：更新 */ }) { Text("更新") }
                    }
                }
            }
            TextButton(
                onClick = { /* 预留：一键更新全部 */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("一键更新全部")
            }
        }
    }
}

data class UpdateItem(val name: String, val oldToNew: String, val size: String)

@Composable
private fun ToolRow(
    name: String,
    version: String,
    sizeMb: Int,
    arm64: String,
    source: String,
    installed: Boolean,
    selectedVersion: String,
    onVersionChange: (String) -> Unit,
    onInstall: () -> Unit,
) {
    Card2 {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (installed) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("已安装", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                }
            }
            Text("推荐 $version · 约 $sizeMb MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("arm64：$arm64", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("版本", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                Text(selectedVersion, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { /* 预留：版本下拉 */ }) { Text("选择") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onInstall, enabled = !installed) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (installed) "已装" else "安装")
                }
            }
        }
    }
}

@Composable
private fun Card2(content: @Composable () -> Unit) {
    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
        content()
    }
}

/** 用 HTTP HEAD 简单测速（不可靠网络也给出结果，失败标注"超时"） */
private suspend fun measureMirror(m: Mirror): String {
    val base = when (m.key) {
        "tsinghua" -> "https://mirrors.tuna.tsinghua.edu.cn"
        "aliyun" -> "https://mirrors.aliyun.com"
        "ustc" -> "https://mirrors.ustc.edu.cn"
        else -> "https://dl.google.com"
    }
    return try {
        val start = System.currentTimeMillis()
        val conn = java.net.URL(base).openConnection()
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.connect()
        val ms = System.currentTimeMillis() - start
        if (ms <= 0) "极快" else "${ms} ms"
    } catch (e: Exception) {
        "超时"
    }
}

@Composable
private fun speedColor(v: String?): Color {
    if (v == null) return MaterialTheme.colorScheme.onSurfaceVariant
    val ms = v.removeSuffix(" ms").toIntOrNull() ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        ms < 300 -> Color(0xFF2E7D32)
        ms < 1000 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
}
