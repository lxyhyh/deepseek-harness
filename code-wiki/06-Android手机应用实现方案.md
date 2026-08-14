# 06-Android 手机应用实现方案与实施记录

## 项目状态（2026-08-14）

| 模块 | 状态 | 说明 |
|------|------|------|
| 方案文档 | ✅ 完成 | 架构、技术选型、功能范围、UI、沙箱、evt 等已定稿 |
| 应用原型 | ✅ 完成 | 7 页交互原型 HTML（已反馈优化） |
| Android 工程骨架 | ✅ 完成 | Compose + Material 3 + WebView 壳 |
| 主题/深色三态 | ✅ 完成 | SYSTEM / LIGHT / DARK |
| 首次安装向导 | ✅ 完成 | 3 步（欢迎/环境检查/完成） |
| 聊天主界面壳 | ✅ 完成 | 顶栏（容器状态）+ 侧边抽屉 + WebView 嵌入 dsh |
| 设置页 | ✅ 完成 | 深色模式三态、工作目录 |
| 工具管理页 | ✅ 实现 | 推荐组合、版本矩阵、可更新清单、一键全部测速 |
| 环境变量页 | ✅ 完成 | 键值编辑、预置默认值、恢复默认 |
| 备份恢复页 | ✅ 完成 | 勾选打包（配置/会话/容器环境）、历史列表、删除 |
| SDK 安装 | ✅ 完成 | cmdline-tools + build-tools 34.0.0 + platform android-35 |
| APK 编译 | ✅ 完成 | `app-debug.apk` 16.4MB（minSdk 26 / targetSdk 35） |
| rootfs 脚本 | ✅ 完成 | `deploy-rootfs.sh`（arm64 环境执行）|
| 沙箱降级 | ✅ 决策 | Android 不启用 bwrap/Landlock，靠 chroot 边界隔离 |
| Git 提交 | ⏳ 待提交 | 当前在 `android` 分支 |

## 关键架构决策

### 混合界面架构
- 原生壳（Kotlin + Compose + Material 3）+ WebView 嵌入 dsh 网页主体
- 对话 / 模型 / 思考强度 / API Key 复用 dsh 网页
- 原生层负责手机增强功能（容器管理、工具/镜像源、备份勾选等）

### 编译工具链（首推稳定组合）
- AGP 8.7.2 + Gradle 8.9 + JDK 17 + Build-Tools 34.0.0
- NDK 27（仅含 native 代码时需装）
- 版本矩阵内置到 ToolsCatalog

### 工具链安装策略
- A 类（基础）：随容器预装（git/curl/Node.js）
- B 类（按需）：SDK/NDK 在「工具管理」页安装（版本选择 + 镜像源切换）
- 下载源：清华 TUNA / 阿里云 / 中科大 / 官方源（一键全部测速）

### 安全与隔离
- 内层沙箱：Android 不启用 dsh 的 bwrap/Landlock（内核不支持）
- 隔离完全靠 chroot 边界 + 只读挂载
- bash 工具使用 `dsh-bash-local`（无沙箱模式）

### 备份策略
- 三勾选项：配置与凭据（默认勾）/ 会话记录（默认勾）/ 容器环境 + 已装工具
- 备份写入 Download 目录，文件名 `dsh-backup-<timestamp>.tar.gz`
- 历史列表可删除（仅移除记录，文件删除需真机 MediaStore 权限）

## 文件结构

```
android-app/
├── dsh-mobile/                    # Android 工程
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/self/dshmobile/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppSettings.kt      # SharedPreferences + env vars + backup history
│   │   │   │   │   ├── ContainerManager.kt # su 协议操作 chroot 容器
│   │   │   │   │   └── ToolsCatalog.kt     # 版本矩阵 + 镜像源
│   │   │   │   └── ui/
│   │   │   │       ├── App.kt              # 路由 + 状态管理
│   │   │   │       ├── ChatScreen.kt       # 聊天主界面（顶栏+抽屉+WebView）
│   │   │   │       ├── DshWebView.kt       # dsh Web UI 嵌入
│   │   │   │       ├── WizardScreen.kt     # 首次安装向导
│   │   │   │       ├── SettingsScreen.kt   # 设置页
│   │   │   │       ├── ToolsScreen.kt      # 工具管理页
│   │   │   │       ├── EnvVarsScreen.kt    # 环境变量页
│   │   │   │       ├── BackupScreen.kt     # 备份恢复页
│   │   │   │       ├── PageScaffold.kt     # 二级页面通用壳
│   │   │   │       └── theme/Theme.kt      # 深色三态主题
│   │   │   ├── AndroidManifest.xml
│   │   │   └── res/...
│   │   └── build.gradle.kts
│   ├── build.gradle.kts, settings.gradle.kts, gradle.properties
│   └── local.properties
└── rootfs/
    └── deploy-rootfs.sh            # arm64 上构建 Ubuntu 24.04 rootfs
```