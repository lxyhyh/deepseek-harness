package com.self.dshmobile.data

/**
 * 工具目录与版本兼容矩阵（离线内置，来源见方案 7.2.3）。
 * 编译工具链存在强版本耦合：AGP ↔ Gradle ↔ JDK ↔ Build-Tools。
 */
object ToolsCatalog {

    /** 首推稳定组合（2026 主流）：兼容面广、aapt2 的 arm64 drop-in 覆盖最稳 */
    val recommended = RecommendedCombo(
        agp = "8.7.2",
        gradle = "8.9",
        jdk = "17",
        buildTools = "34.0.0",
        ndk = "27.0.12077973", // 仅含 native 代码项目需要
    )

    /** 版本矩阵（官方兼容关系；发版时按 Android 官方「AGP 版本说明」同步） */
    val matrix = listOf(
        Combo("AGP 8.4", "Gradle 8.6", "JDK 17", "Build-Tools 34.0.0"),
        Combo("AGP 8.6", "Gradle 8.7", "JDK 17", "Build-Tools 34.0.0"),
        Combo("AGP 8.7", "Gradle 8.9", "JDK 17", "Build-Tools 34.0.0", "NDK 27"),
        Combo("AGP 8.10–8.13", "Gradle 8.11.1 / 8.13", "JDK 17", "Build-Tools 35.0.0"),
        Combo("AGP 9.0–9.2", "Gradle 9.1.0 / 9.3.1 / 9.4.1", "JDK 17", "Build-Tools 36.0.0"),
    )

    /** 可安装工具（B 类：按需安装，见方案 7.2.2）。arm64 标注官方可用性。 */
    val tools = listOf(
        ToolItem("JDK", version = "17（可切 21）", installed = false, sizeMb = 350,
            arm64 = "官方有 arm64", source = "apt / 官方发行"),
        ToolItem("Gradle", version = "8.9（可切 8.14.3）", installed = false, sizeMb = 130,
            arm64 = "官方有 arm64", source = "官方发行（含 aarch64）"),
        ToolItem("AGP", version = "8.7.2（随项目声明）", installed = false, sizeMb = 80,
            arm64 = "纯 Java，无架构问题", source = "Gradle 内声明"),
        ToolItem("Build-Tools", version = "34.0.0", installed = false, sizeMb = 60,
            arm64 = "aapt2 官方无 arm64（社区 drop-in）", source = "sdkmanager + 社区 drop-in"),
        ToolItem("Android NDK", version = "27", installed = false, sizeMb = 1200,
            arm64 = "官方无 arm64 host（社区/混合/Box64）", source = "sdkmanager + 替代方案"),
    )
}

data class RecommendedCombo(
    val agp: String,
    val gradle: String,
    val jdk: String,
    val buildTools: String,
    val ndk: String,
)

data class Combo(
    val agp: String,
    val gradle: String,
    val jdk: String,
    val buildTools: String,
    val ndk: String? = null,
)

data class ToolItem(
    val name: String,
    val version: String,
    val installed: Boolean,
    val sizeMb: Int,
    val arm64: String,
    val source: String,
)

/** 工具下载源（见方案 7.8 ④） */
data class Mirror(
    val key: String,
    val name: String,
)

val mirrors = listOf(
    Mirror("tsinghua", "清华 TUNA 镜像"),
    Mirror("aliyun", "阿里云镜像"),
    Mirror("ustc", "中科大镜像"),
    Mirror("official", "官方源"),
)
