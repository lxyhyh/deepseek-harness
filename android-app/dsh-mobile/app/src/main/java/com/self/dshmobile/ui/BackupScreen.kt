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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.self.dshmobile.data.AppSettings
import com.self.dshmobile.data.ContainerManager
import com.self.dshmobile.data.SuResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 可勾选备份项（方案 7.4：配置 / 会话默认勾选，容器环境按需） */
private data class BackupItem(
    val key: String,
    val name: String,
    val sizeHint: String,
    val defaultChecked: Boolean,
)

private val backupItems = listOf(
    BackupItem("config", "配置与凭据", "约 4 MB", defaultChecked = true),
    BackupItem("sessions", "会话记录", "约 12 MB", defaultChecked = true),
    BackupItem("container", "容器环境（Ubuntu rootfs + 已装工具）", "约 2.1 GB", defaultChecked = false),
)

/**
 * 备份与恢复页（方案 7.4）：
 * 可勾选打包内容 + 一键备份（写入 Download）+ 历史列表（可删除）。
 */
@Composable
fun BackupScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    scope: CoroutineScope,
    snackbar: (String) -> Unit,
) {
    var checked by remember { mutableStateOf(backupItems.associate { it.key to it.defaultChecked }) }
    var history by remember { mutableStateOf(settings.backupHistory) }
    var backingUp by remember { mutableStateOf(false) }

    PageScaffold(
        title = "备份与恢复",
        onBack = onBack,
        actions = {
            if (backingUp) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("备份内容")
            androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    backupItems.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked[item.key] ?: false,
                                onCheckedChange = { c -> checked = checked + (item.key to c) },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(item.sizeHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(
                "备份写入手机「下载 / Download」目录，文件名 dsh-backup-<时间戳>.tar.gz。\n" +
                    "容器环境体积大，默认不勾选；SDK/NDK 位于容器内，随容器一起打包。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    backingUp = true
                    scope.launch {
                        val result = ContainerManager.backup(snapshot = "manual")
                        backingUp = false
                        when (result) {
                            is SuResult.Ok -> {
                                val name = "dsh-backup-${System.currentTimeMillis()}.tar.gz"
                                history = listOf(name) + history
                                settings.backupHistory = history
                                snackbar("备份成功：$name")
                            }
                            is SuResult.NoRoot -> snackbar("需要 root 才能打包容器")
                            is SuResult.Fail -> snackbar("备份失败：${result.message}")
                        }
                    }
                },
                enabled = !backingUp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (backingUp) "备份中…" else "一键备份")
            }

            SectionTitle("历史备份")
            if (history.isEmpty()) {
                InfoCard2(
                    title = "暂无备份",
                    body = "点击「一键备份」生成第一份备份，随后可在此选择恢复或删除。",
                )
            } else {
                androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        history.forEachIndexed { i, name ->
                            if (i > 0) HorizontalDivider()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { /* 预留：恢复 */ }) { Text("恢复") }
                                IconButton(onClick = {
                                    // 从历史移除；文件删除在真机联调时走 MediaStore（Android 10+ 需授权）
                                    history = history.filterNot { it == name }
                                    settings.backupHistory = history
                                    snackbar("已删除备份记录：$name")
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
