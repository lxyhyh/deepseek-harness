package com.self.dshmobile.data

import android.content.Context
import androidx.core.content.edit
import com.self.dshmobile.ui.theme.DarkMode
import org.json.JSONArray
import org.json.JSONObject

/** 环境变量键值对（编译环境用，预置默认值 + 可视化编辑，见方案 7.2.4） */
data class EnvVar(val key: String, val value: String, val editable: Boolean = true)

/** 应用内设置（SharedPreferences）：深色模式、首次安装标记、下载源、工作目录、环境变量、备份历史 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("dsh_settings", Context.MODE_PRIVATE)

    /** 是否已完成首次安装（决定启动进聊天页还是向导） */
    var initialized: Boolean
        get() = prefs.getBoolean("initialized", false)
        set(value) = prefs.edit { putBoolean("initialized", value) }

    /** 深色模式：SYSTEM / LIGHT / DARK（对应 [com.self.dshmobile.ui.theme.DarkMode]） */
    var darkMode: String
        get() = prefs.getString("dark_mode", DarkMode.SYSTEM.name) ?: DarkMode.SYSTEM.name
        set(value) = prefs.edit { putString("dark_mode", value) }

    /** 工具下载源：tsinghua / aliyun / ustc / official */
    var mirror: String
        get() = prefs.getString("mirror", "tsinghua") ?: "tsinghua"
        set(value) = prefs.edit { putString("mirror", value) }

    /** 容器工作目录（默认为容器内 /root/workspace） */
    var workspaceDir: String
        get() = prefs.getString("workspace_dir", "/root/workspace") ?: "/root/workspace"
        set(value) = prefs.edit { putString("workspace_dir", value) }

    /** 环境变量列表（预置默认值，见 [defaultEnvVars]） */
    var envVars: List<EnvVar>
        get() {
            val raw = prefs.getString("env_vars", null) ?: return defaultEnvVars()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            EnvVar(
                                key = o.getString("key"),
                                value = o.optString("value", ""),
                                editable = o.optBoolean("editable", true),
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                defaultEnvVars()
            }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach {
                val o = JSONObject()
                o.put("key", it.key)
                o.put("value", it.value)
                o.put("editable", it.editable)
                arr.put(o)
            }
            prefs.edit { putString("env_vars", arr.toString()) }
        }

    /** 备份历史（文件名列表，新→旧） */
    var backupHistory: List<String>
        get() {
            val raw = prefs.getString("backup_history", null) ?: return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) add(arr.getString(i))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { arr.put(it) }
            prefs.edit { putString("backup_history", arr.toString()) }
        }

    companion object {
        /** 预置编译环境变量（容器内 SDK/NDK 默认位置，见方案 7.2.4） */
        fun defaultEnvVars(): List<EnvVar> = listOf(
            EnvVar("JAVA_HOME", "/opt/jdk-17", editable = false),
            EnvVar("ANDROID_HOME", "/opt/android-sdk", editable = false),
            EnvVar("ANDROID_SDK_ROOT", "/opt/android-sdk", editable = false),
            EnvVar("ANDROID_NDK_HOME", "/opt/android-sdk/ndk/27.0.12077973", editable = true),
            EnvVar("PATH", "/opt/jdk-17/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:\$PATH", editable = true),
        )
    }
}
