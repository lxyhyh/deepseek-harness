package com.self.dshmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.self.dshmobile.data.EnvVar

/**
 * 环境变量页（方案 7.2.4）：编译环境键值可视化编辑。
 * 预置默认值（JAVA_HOME / ANDROID_HOME 等），可增删改；保存后写入容器（真机联调时落盘到容器 profile）。
 */
@Composable
fun EnvVarsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
) {
    var vars by remember { mutableStateOf(settings.envVars.toMutableList()) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    PageScaffold(
        title = "环境变量",
        onBack = onBack,
        actions = {
            TextButton(onClick = {
                settings.envVars = vars
            }) { Text("保存") }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoCard2(
                title = "编译环境变量",
                body = "这些变量会注入容器内的 dsh 环境，供编译工具（JAVA_HOME / ANDROID_HOME / NDK / PATH）使用。\n" +
                    "带 🔒 的为系统保留项，仅可查看，不可修改。",
            )

            vars.forEachIndexed { index, env ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = env.key,
                            onValueChange = { k -> vars[index] = env.copy(key = k) },
                            label = { Text("变量名") },
                            enabled = env.editable,
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = env.value,
                            onValueChange = { v -> vars[index] = env.copy(value = v) },
                            label = { Text("值") },
                            enabled = env.editable,
                            singleLine = true,
                        )
                    }
                    if (env.editable) {
                        IconButton(onClick = { vars.removeAt(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    } else {
                        Text("🔒", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            if (vars.isNotEmpty()) HorizontalDivider()

            // 新增
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    label = { Text("新变量名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    if (newKey.isNotBlank()) {
                        vars = vars.toMutableList().apply { add(EnvVar(newKey.trim(), newValue)) }
                        newKey = ""
                        newValue = ""
                    }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加")
                }
            }
            OutlinedTextField(
                value = newValue,
                onValueChange = { newValue = it },
                label = { Text("新变量值") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(
                onClick = {
                    vars = AppSettings.defaultEnvVars().toMutableList()
                    settings.envVars = vars
                },
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("恢复默认值")
            }
        }
    }
}
