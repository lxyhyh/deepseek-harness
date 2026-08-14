package com.self.dshmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.self.dshmobile.data.AppSettings
import com.self.dshmobile.ui.theme.DarkMode

/**
 * 设置页（方案 7.8 ②）：
 * 深色模式三态 / 工作目录 / 关于。工具更新与下载源在「工具管理」页。
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
) {
    var darkMode by remember { mutableStateOf(DarkMode.valueOf(settings.darkMode)) }
    var workspace by remember { mutableStateOf(settings.workspaceDir) }

    PageScaffold(title = "设置", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("外观")
            SettingRow(title = "深色模式", subtitle = "跟随系统 / 浅色 / 深色") {
                // 三态 radio 用点击行切换，语义清晰
                DarkModeOptions(
                    current = darkMode,
                    onSelect = {
                        darkMode = it
                        settings.darkMode = it.name
                    },
                )
            }

            SectionTitle("容器")
            SettingRow(
                title = "工作目录",
                subtitle = "dsh 在容器内的默认工作区（相对 /root）",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(workspace, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { /* 预留：目录选择器 */ }) { Text("编辑") }
                }
            }
            InfoCard2(
                title = "容器状态",
                body = "容器启动 / 停止 / 重启在聊天页右上角菜单操作。\n需要已 Root 设备（KernelSU / Magisk）。",
            )

            SectionTitle("关于")
            InfoCard2(
                title = "dsh Mobile 0.1.0",
                body = "基于 @deepseek-ai/dsh 的「手机即主机」客户端。\n聊天 / 模型 / 思考强度 / API Key 均在 dsh 网页主体中配置。",
            )
        }
    }
}

@Composable
private fun DarkModeOptions(current: DarkMode, onSelect: (DarkMode) -> Unit) {
    Column {
        DarkModeOption(DarkMode.SYSTEM, "跟随系统", current == DarkMode.SYSTEM) { onSelect(DarkMode.SYSTEM) }
        DarkModeOption(DarkMode.LIGHT, "浅色", current == DarkMode.LIGHT) { onSelect(DarkMode.LIGHT) }
        DarkModeOption(DarkMode.DARK, "深色", current == DarkMode.DARK) { onSelect(DarkMode.DARK) }
    }
}

@Composable
private fun DarkModeOption(value: DarkMode, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            label,
            style = if (selected) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
