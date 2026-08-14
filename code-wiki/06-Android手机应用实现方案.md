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
| 内置容器 | ✅ 完成 | arm64 rootfs + Node.js 22 + dsh 预装，归档嵌入 APK assets，首次解压即用 |
| APK 编译 | ✅ 完成 | `app-debug.apk` 378MB（含内置容器归档 366MB，分 4 块嵌入） |
| rootfs 脚本 | ✅ 完成 | `deploy-rootfs.sh`（x86 沙箱构建 arm64 归档）+ `swap-arm64.sh` |
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

### 内置容器（随 App 打包，无需下载）
- 容器 rootfs 以内置归档随 APK 打包，首次部署解压即用，不依赖联网下载
- 构建方式：在 x86-64 沙箱下载 Ubuntu noble arm64 cloud-image rootfs + Node.js 22.14 arm64 二进制 + dsh 纯 JS 产物（pnpm deploy），拼装打包为 `dsh-container-arm64.tar.gz`（366MB，解压后 1.5GB）
- 原生模块架构对齐：`swap-arm64.sh` 把 ripgrep / sharp / koffi / node-addon-require-builtin 替换为 arm64 变体；node-pty 用 aarch64 交叉工具链 + arm64 Node 头文件 `node-gyp` 交叉编译 `pty.node`；删除 x64 回退包瘦身
- 嵌入与部署：
  - 归档分 4 块（`assets/dsh-container-arm64.tar.gz.00~03`）打进 APK（分块避免大文件打包触发构建 OOM）
  - `ContainerManager.deployFromAssets()` 逐块经 `su` 拼接到 `/data/local/dsh-container/.deploy.tar.gz`，再 `tar xzf` 解压（宿主 toybox tar），最后删除临时归档
  - 首次安装向导在环境检查后出现「开始部署（内置 Ubuntu 容器）」按钮 + 解压进度
- 启动：`su` 内 mount proc/sys/dev → `chroot` → `setsid nohup ... dsh web --host 127.0.0.1 --port 3080`
- 体积：APK 由 16.4MB 增至 378MB（内置容器占 366MB）

## 文件结构

```
android-app/
├── dsh-mobile/                    # Android 工程
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── assets/
│   │   │   │   └── dsh-container-arm64.tar.gz.00~03   # 内置容器归档（分 4 块）
│   │   │   ├── java/com/self/dshmobile/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppSettings.kt      # SharedPreferences + env vars + backup history
│   │   │   │   │   ├── ContainerManager.kt # su 协议 + 内置归档解压部署 + chroot 启动
│   │   │   │   │   └── ToolsCatalog.kt     # 版本矩阵 + 镜像源
│   │   │   │   └── ui/
│   │   │   │       ├── App.kt              # 路由 + 状态管理
│   │   │   │       ├── ChatScreen.kt       # 聊天主界面（顶栏+抽屉+WebView）
│   │   │   │       ├── DshWebView.kt       # dsh Web UI 嵌入
│   │   │   │       ├── WizardScreen.kt     # 首次安装向导（含内置容器部署）
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
    ├── deploy-rootfs.sh            # x86 沙箱构建 arm64 内置容器归档
    └── swap-arm64.sh               # 替换原生模块为 arm64 变体
```