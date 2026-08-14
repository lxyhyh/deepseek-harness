package com.self.dshmobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** 深色模式三态：跟随系统 / 浅色 / 深色（用户确认：跟随系统 + App 内手动开关） */
enum class DarkMode { SYSTEM, LIGHT, DARK }

@Composable
fun DshMobileTheme(darkMode: DarkMode, content: @Composable () -> Unit) {
    val dark = when (darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
