# Android 手机应用实现方案

> 版本：v0.6（细化版：应用 UI 设计方案 + 实施排期）
> 日期：2026-08-13
> 分支：`android`

## 1. 一句话目标

**基于 dsh（DeepSeek Harness）框架，在 Android 手机上本地运行 agent，做一个"手机就是主机"的一体化 App** —— 用户平时像用 Operit AI 一样打开 App 使用，但底座是 dsh 的开源框架。

---

## 2. 已确认的决策（来自需求澄清）

| 决策点 | 结论 |
|---|---|
| 交付物 | **实现方案文档**（本文档），本轮不写代码、不打 APK |
| 架构形态 | **手机本地跑 dsh 核心**（手机即主机，参考 Operit 的 chroot 容器方案） |
| 操作入口 | **手机上装一个一体化 App**（不是纯浏览器） |
| AI 大脑 | **云端 API**（DeepSeek 等），联网使用，不依赖本地模型 |
| 核心能力 | AI 对话 + 工具调用、内置 Linux 执行环境、插件/MCP 生态 |
| 是否操控其它 App | **不要**（不做无障碍点击操控，降低权限与误操作风险） |
| 跨会话记忆 | **后续再加**（第一版用 dsh 自带会话日志即可） |
| 用途 | **自用**，不分发、不上商店 |
| 第一版范围 | **最小可用版**：能打开对话、能调工具、能跑 Linux 命令、能装 MCP 插件 |
| 安装形态 | **一体化 App**（自动装环境，体验接近 Operit） |
| 手机 root | **愿意 root**，root 方式为 **KernelSU 等框架**（通用 su 提权） |
| Linux 环境 | **chroot 完整 Ubuntu 24**（与 Operit 同款容器技术），**非 Termux** |
| rootfs 打包 | **随 APK 打包**（基础工具 + 常用工具链预装，容器包约 400–800MB 可接受）；**SDK/NDK 体积巨大，改为按需安装** |
| 预装工具 | Python 全套、Node.js 全套、Git/LFS、编译工具链、日常终端工具、JDK、Gradle、AGP、Java、Go、PHP |
| 编译用途 | 编译 Android 项目、AI 手机本地编译验证、跑 Go/PHP 程序 |
| SDK/NDK | **按需安装**；工具界面支持查看/选择版本、**卸载**释放空间；**自动推荐一套主流兼容组合**（AGP/Gradle/JDK/SDK） |
| 版本建议 | **内置版本兼容矩阵**（AGP↔Gradle↔JDK↔SDK）；需**注意部分工具官方无 arm64 版本** |
| 工具更新 | **手动一键更新**（工具管理页：先显示**可更新清单**（哪些工具能更新、当前版本→新版本），再一键更新，触发容器内 apt/工具链更新） |
| 环境变量 | **预置默认**（JAVA_HOME/ANDROID_HOME/PATH 等）+ **可视化编辑器** |
| AI 操作边界 | **隔离**：AI 只能在 chroot Ubuntu 内读写执行；对手机本体**只读 /sdcard/Download**（唯一可读文件夹），不可写手机本体 |
| 备份恢复 | **第一版包含一键备份/恢复**（Ubuntu 环境 + 配置 + 会话日志，可跨机恢复） |
| 应用 UI 结构 | **单聊天界面**（打开即聊天）；工具管理/设置收进右上角菜单（用户 2026-08-13 确认） |
| 应用 UI 风格 | **Android 原生风格（Material 3）**，自动适配系统主题（用户确认） |
| 深色模式 | **跟随系统自动切换 + App 内手动开关**（用户确认） |
| 名称/图标 | **先用临时名**（如 `dsh Mobile`）+ 默认图标，后续再设计（用户确认） |

---

## 3. 需求背景与参考对象

### 3.1 为什么在 dsh 之上做，而不是照搬 Operit

- Operit 是**闭源原生 App**：内置 chroot Linux + 本地模型推理 + 无障碍自动化，整套是私有实现，无法在 dsh 上复用。
- dsh 是**开源框架**：已具备 agent 循环、会话日志（session log）、工具系统、shell/subprocess 能力、skill 插件注册表、Web UI、MCP 工具消费等基础。
- 我们的方案：**把 dsh 当作底座，在手机本地把 dsh 跑起来**，再套一层移动端外壳，避免重复造轮子，同时获得 dsh 的会话/日志/插件生态。

### 3.2 参考 agent 的可借鉴点（不是照搬，是取舍）

| 参考对象 | 可借鉴能力 | 本项目取舍 |
|---|---|---|
| **Operit AI** | 手机本地一体化体验、**chroot Ubuntu 24 容器**、App 化安装、root/KernelSU 集成 | 借鉴"手机即主机 + chroot Ubuntu + App 一体化 + su 提权"；**不做**无障碍点击操控、不做本地模型推理 |
| **Hermes Agent** | 跨会话持久记忆（FTS5 + 摘要）、技能自动沉淀、cron 定时、MCP 集成、多后端 | 记忆、技能、定时 → **列入后续阶段**；MCP 集成 → **第一版纳入**（dsh 已有 MCP 相关 seam） |
| **OpenClaw** | 网关 + 技能插件生态、本地/云端双模型路线 | 借鉴"技能插件 = 手脚库"的思路；聊天渠道接入（微信/飞书等）→ 后续阶段 |
| **OpenCode** | 多模型切换、Plan/Build 双模式、Client/Server 分离 | 借鉴多模型可切换、任务规划模式；CLI/TUI 形态 → 本项目用 App 界面 |

---

## 4. 总体架构

```
┌──────────────────────────────────────────────────────────┐
│                    Android 手机（主机）                    │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │            一体化 App（Kotlin/Compose）              │  │
│  │  ├─ WebView 容器 → 加载 dsh Web UI（127.0.0.1:3080） │  │
│  │  ├─ 环境管理：首次启动自动引导 chroot + Ubuntu + dsh │  │
│  │  ├─ 提权管理：通过 su（KernelSU 框架）启动容器        │  │
│  │  ├─ 备份/恢复：一键打包/还原（环境+配置+日志）        │  │
│  │  └─ 设置页：API Key、模型选择、目录选择             │  │
│  └──────────────────┬─────────────────────────────────┘  │
│                     │ HTTP/WebSocket (localhost)          │
│  ┌──────────────────▼─────────────────────────────────┐  │
│  │    dsh 运行时（chroot 容器内的 Ubuntu 24 中运行）    │  │
│  │  ├─ Node.js (>=22.19)                               │  │
│  │  ├─ dsh 核心（agent 循环 / session / 工具 / 插件）    │  │
│  │  ├─ dsh Web 服务（端口 3080）                        │  │
│  │  └─ shell/subprocess 能力（执行 Linux 命令）         │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  chroot 容器：完整 Ubuntu 24（随 App 或脚本部署）      │  │
│  │  /bin /usr /etc /var /home /root                    │  │
│  │  Python / Node.js / git / apt 全部可用              │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  手机本体访问（隔离边界，仅只读）                     │  │
│  │  ├─ 只读挂载：/sdcard/Download（唯一可读）           │  │
│  │  └─ 其余手机目录（DCIM/Documents/系统等）不可访问     │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
             │
             ▼  HTTPS（仅指令与结果走云端）
     DeepSeek 等云端大模型 API
```

**关键点说明：**
- 手机里的 **chroot 容器（完整 Ubuntu 24）** 是"执行层"，dsh 的 shell 能力在这个环境里跑命令、读文件——这就是"内置 Linux 环境"，与 Operit 同款容器技术。
- App 用系统 WebView 加载 dsh 自带 Web UI，天然获得对话、工具卡片、会话历史展示，**不用重写 UI**。
- 所有数据（会话日志、配置）留在手机本地；只有请求大模型时，把对话指令发给云端 API（与 Operit 云端模式一致）。
- **隔离边界**：AI（dsh 及其 shell 工具）默认工作目录在 chroot 容器内部，只能读写容器内文件；对手机本体的访问被限制为**只读挂载 `/sdcard/Download`**（唯一可读目录），容器内不可写手机其它目录。
- **提权**：chroot 挂载需要 root，App 通过 `su`（KernelSU 框架提供）启动容器；日常对话无需反复提权（容器常驻），仅启动/备份等操作需要 root。
- 不做无障碍操控，App 权限极小，误操作风险集中在容器内部（容器可随时整体备份/恢复，见 8.4）。

---

## 5. 技术选型

| 层 | 选型 | 理由 |
|---|---|---|
| 手机 App | Kotlin + Jetpack Compose | 现代 Android 官方栈，一体化体验 |
| WebView | Android 系统 WebView + JavaScriptBridge | 复用 dsh Web UI，无需重做界面 |
| 提权 | `su`（KernelSU 框架） | 启动 chroot 容器需要 root；KernelSU 兼容通用 su 协议 |
| Linux 容器 | **chroot 完整 Ubuntu 24.04**（随 App 部署） | 与 Operit 同款；apt 全套可用，接近真电脑 |
| dsh 运行时 | Node.js ≥ 22.19（Ubuntu 内 apt 安装） | dsh 硬性要求（见仓库 AGENTS.md） |
| 数据 | dsh 自带 session 持久化（SQLite/JSONL） | 会话历史本地落盘，天然满足"模型可见⟺可记录" |
| 隔离 | 容器只读挂载 `/sdcard/Download`；其余手机目录不可见 | 满足"隔离 + 仅可读下载目录"的安全边界 |
| 备份 | 打包容器 + 配置 + 会话日志为一个归档 | 自用 root 折腾，需可整体恢复 |
| 构建 | Gradle + Android SDK（沙箱需先安装 SDK） | Android 官方构建链 |
| 目标设备 | 已 root 的 Android 手机（API 26+，约 Android 8.0+） | 需 KernelSU/su 才能跑 chroot |

---

## 6. 功能范围

### 6.1 第一版（最小可用版）—— 本轮方案的交付范围

| 模块 | 功能 | 备注 |
|---|---|---|
| 环境安装 | 首次启动自动引导：解包预装 rootfs（chroot Ubuntu 24 + 基础工具 + dsh） | 一体化 App 的核心体验；需 su 提权 |
| 提权管理 | 通过 su（KernelSU）启动容器，常驻运行 | 启动/备份/装 SDK·NDK 才需 root，日常对话不需反复提权 |
| 服务管理 | 一键启动/停止 dsh Web 服务（127.0.0.1:3080） | 断网重连稳定 |
| 对话界面 | WebView 加载 dsh Web UI，正常聊天 | 复用 dsh，不重写 UI |
| 工具调用 | dsh 自带工具（fs/shell/web/subprocess 等）开箱即用 | 对齐 dsh 能力 seam |
| 内置 Linux | chroot Ubuntu 24：Shell、脚本、apt 装包 | 与 Operit 同款容器 |
| 工具管理 | 编译工具链界面：查看/安装/卸载 SDK·NDK 等，自动推荐兼容组合 | 按需安装，内置版本矩阵，见 7.2.2/7.2.3 |
| 环境变量 | 预置默认 + 可视化编辑器（JAVA_HOME/ANDROID_HOME 等） | 编译环境一致性，见 7.2.4 |
| 插件/MCP | 能在 App 内配置并加载 skill 插件 / MCP 服务器 | 复用 dsh 插件注册表 |
| 隔离边界 | 容器内自由读写；手机本体仅**只读** `/sdcard/Download` | 满足用户确认的安全边界 |
| 备份/恢复 | 一键打包容器+配置+会话日志；可跨机恢复 | 自用 root 折腾的兜底 |
| 设置页 | API Key、模型选择、工作目录、启动/停止 | 原生设置页（非 WebView） |
| 安全 | 服务仅绑定 localhost；容器隔离；root 仅在必要时提权 | 综合 Operit 安全性 + 隔离策略 |

### 6.2 后续阶段（本方案仅规划，不在第一版）

- **跨会话持久记忆**（参考 Hermes：FTS5 + 摘要，注入系统提示词）
- **技能自动沉淀**（任务完成后生成可复用 skill）
- **定时任务**（dsh 已有 `schedule` 包可基于事件日志实现提醒/报告）
- **多模型切换**（dsh 已支持多 provider，做成设置项即可）
- **聊天渠道接入**（微信/飞书/Telegram 网关，参考 OpenClaw）
- **本地离线模型**（Ollama 对接，仅当用户有强机需求）

---

## 7. 详细方案：系统 / 工具 / 技能 / 插件 / MCP

> 本小节基于 dsh 仓库**实际存在的机制**编写（默认组合 `packages/bundle/base/cordis.patch.yml`、`web-app/cordis.patch.yml`、`packages/mcp/mcp-client`、`packages/skill/*`、`examples/mcp-memory/*`），不做臆造。

### 7.1 安装什么系统（手机侧运行环境）

| 层次 | 选型 | 说明 |
|---|---|---|
| 手机宿主 OS | Android 8.0+（API 26+），**已 root**（KernelSU 等框架） | chroot 需要 root 挂载容器 |
| Root 框架 | KernelSU（兼容通用 su 协议） | 用户已确认采用 KernelSU 等框架 |
| Linux 容器 | **chroot 完整 Ubuntu 24.04**（随 App 打包/脚本部署） | 与 Operit 同款容器技术；`apt` 全套可用 |
| Node.js | Node.js ≥ 22.19（Ubuntu 内 apt 安装） | dsh 硬性要求（仓库 AGENTS.md） |
| 包管理器 | pnpm / npm（Ubuntu 内安装） | 安装 dsh 及其 workspace 依赖 |

**为什么用 chroot Ubuntu 24 而不是 Termux：**
- Operit 用 chroot 跑完整 Ubuntu 24（约 380MB 安装包），用户希望与 Operit 同款体验（手机变真 Linux 工作站）。
- chroot 提供完整文件系统（`/bin /usr /etc /var`）、`apt`、系统级工具链，比 Termux 的包管理更接近真电脑。
- 代价：chroot 需要 root 权限——用户已确认愿意 root（KernelSU），此条件成立。

**chroot 容器部署方式（第一版建议）：**
- App 首次启动自动执行：`su -c` 挂载容器根目录 → 解压随包部署的 Ubuntu rootfs（**已预装基础工具 + dsh，见 7.2.1**）→ 挂载 `/proc` `/dev` `/dev/pts` 等 → 进入 chroot 校验 dsh 运行。
- 容器数据目录建议放在 `/data/local/dsh-container`（root 可访问、非易失分区）。
- 提权仅用于**启动容器 / 备份打包 / 按需安装 SDK·NDK**；容器常驻后，dsh 进程在容器内以普通用户运行，日常对话不需要 root。

### 7.2 安装什么工具（Linux 执行层常用工具）

> 这些工具由 dsh 的 shell 能力（`tool-bash` → `dsh-bash-sandbox` → `dsh-subprocess-local`）在 **chroot Ubuntu 24 容器内**执行。默认组合已启用 `tool-bash`、`tool-fs`、`tool-web` 等（见 base/cordis.patch.yml）。
>
> **打包策略：** 工具分两类——**A. 随容器预装**（`apt` 装好后随 rootfs 整体打包，开箱即用，用 apt 更新）；**B. 按需安装**（体积巨大或极少用，首次需要时由 App 引导下载）。

#### 7.2.1 A 类：随容器预装（开箱即用）

| 工具 | 用途 | 安装来源 |
|---|---|---|
| `git` + `git-lfs` | 版本管理、拉取插件/技能/项目 | `apt install git git-lfs` |
| `python3` + `pip` + 常用开发库 | 跑 Python 脚本（dsh 有 python/ SDK） | `apt install python3 python3-pip` |
| `nodejs` ≥ 22 + `npm` | 运行 dsh 本体 + MCP 服务器（stdio） | 官方 arm64 包（nodesource）装 LTS ≥ 22 |
| `curl` / `wget` | 网络、下载 | `apt install curl wget` |
| `openssh-client` | （可选）远程排障 | `apt install openssh-client` |
| `ripgrep` / `fd-find` | 快速搜索（dsh 的 fs-search 工具可调用） | `apt install ripgrep fd-find` |
| `build-essential` | 编译原生依赖 | `apt install build-essential` |
| `go` | 跑 Go 程序 | `apt install golang`（或官方 arm64 tarball） |
| `php` + `composer` | 跑 PHP 程序 | `apt install php composer` |
| `bash` / `coreutils` / `file` / `tar` | shell 基础、备份打包 | Ubuntu 自带 |

> **架构注意（用户重点提示）：** 以上全部选择 **arm64** 版本（`apt` 在 arm64 rootfs 内默认就是 arm64 架构）。但要核实**部分工具官方是否提供 arm64**：Node.js/Python/Go/JDK/Gradle 官方都有 Linux arm64；个别 CLI 工具若无官方 arm64 二进制，需用源码编译或选社区 arm64 构建（在版本矩阵中标注）。

#### 7.2.2 B 类：按需安装（编译工具链，体积巨大）

> SDK/NDK 体积达 2GB+，**不随容器预装**；由 App 的"工具界面"按需安装。这些工具是"编译 Android 项目 / AI 手机本地编译验证"的核心，装好后在容器内与 dsh 无缝配合。

| 工具 | 用途 | 安装方式 | arm64 可用性 |
|---|---|---|---|
| JDK（17/21） | Gradle/AGP 编译运行需要 | `apt install openjdk-17-jdk` 或官方 | ✅ 有 arm64 |
| Gradle | 构建工具 | 官方发行（含 aarch64 版本） | ✅ 有 arm64 |
| AGP（Android Gradle Plugin） | Android 构建插件 | Gradle 内声明版本（随项目） | ✅ 纯 Java |
| Android SDK（platforms/build-tools/platform-tools） | 编译 Android 项目 | Android Studio 官方命令行工具 `sdkmanager` | ⚠️ 官方 build-tools 的 `aapt2` **无 arm64 Linux 版**；需社区构建或 Box64（见 7.2.5） |
| Android NDK | 编译原生 C/C++ 代码 | `sdkmanager` 下载 | ⚠️ 官方 Linux 仅 x86_64 host；arm64 host 需社区构建或 Box64（见 7.2.5） |

**工具界面功能（App 原生页，非 WebView）：**
- 列出全部可装工具 + 当前已装版本 + 占用空间。
- **自动推荐一套主流兼容组合**（首次使用时）：如 AGP 8.x + Gradle 8.x + JDK 17 + SDK 35 + NDK 27。
- 支持**手动选择版本**安装；支持**卸载**已装 SDK/NDK 版本释放空间。
- **内置版本兼容矩阵**（详见 7.2.3），给出"当前选择组合是否兼容"的提示。
- 对 aapt2/NDK 等无官方 arm64 的工具，界面展示**替代安装方案**（社区构建 / Box64）并标记来源（见 7.2.5）。

#### 7.2.3 版本兼容矩阵（内置，离线可用）

> 编译工具链存在**强版本耦合**：AGP ↔ Gradle ↔ JDK ↔ Android SDK Build-Tools 之间有官方兼容表。用户要求"根据网上版本关联给出建议"，方案采用**内置矩阵**（离线可靠），发版时随 App 更新。

- 矩阵内容（官方兼容关系，2026-08 校准，AGP 8.x/9.x 均要求 JDK 17 起）：
  - AGP 8.4 ↔ Gradle 8.6 ↔ JDK 17 ↔ Build-Tools 34.0.0
  - AGP 8.6 ↔ Gradle 8.7 ↔ JDK 17 ↔ Build-Tools 34.0.0
  - AGP 8.7 ↔ Gradle 8.9 ↔ JDK 17 ↔ Build-Tools 34.0.0 ↔ 默认 NDK 27.0.12077973
  - AGP 8.10–8.13 ↔ Gradle 8.11.1 / 8.13 ↔ JDK 17 ↔ Build-Tools 35.0.0
  - AGP 9.0–9.2 ↔ Gradle 9.1.0 / 9.3.1 / 9.4.1 ↔ JDK 17 ↔ Build-Tools 36.0.0
  - 发版时以 Android 官方「AGP 版本说明」页为准同步。
- **首推稳定组合（2026 主流，工具界面默认推荐）：** AGP 8.7.2 + Gradle 8.9 + JDK 17 + Build-Tools 34.0.0 +（仅含 native 代码项目需要）NDK 27.0.12077973——老项目兼容面广、aapt2 的 arm64 drop-in 覆盖最稳。
- 工具界面在选择版本时，根据矩阵**自动提示兼容组合 / 不兼容警告**，避免装出跑不起来的组合。
- **注意事项（用户重点提示）：** 矩阵中标注每个工具的 **arm64 可用性**——官方无 arm64 版本的工具，要么提供替代安装源，要么在界面明示"该版本无 arm64，无法在手机安装"，不让用户装了个装不上的东西。

#### 7.2.4 环境变量管理（预置默认 + 可视化编辑）

> 编译工具强依赖环境变量（`JAVA_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT`、`ANDROID_NDK_HOME`、`GRADLE_USER_HOME`、`PATH` 等），且 SDK/NDK 目录结构敏感。方案采用两层管理：

- **预置默认**：App 部署容器时自动写入 shell 配置（`/etc/profile.d/` 或 `~/.bashrc`），默认如下：
  - `JAVA_HOME=/usr/lib/jvm/...`（按实际 JDK 路径）
  - `ANDROID_HOME=/opt/android-sdk`、`ANDROID_SDK_ROOT=$ANDROID_HOME`、`ANDROID_NDK_HOME=$ANDROID_HOME/ndk/<ver>`
  - `GRADLE_USER_HOME=/root/.gradle`（或用户目录）
  - `PATH` 前缀加入 `$JAVA_HOME/bin`、`$ANDROID_HOME/cmdline-tools/latest/bin`、`$ANDROID_HOME/platform-tools`、`$GOROOT/bin`、`$PATH`
- **可视化编辑器**：App 设置页提供"环境变量"编辑界面——列出当前变量（键/值/来源），支持新增/修改/删除；修改后写入上述 shell 配置并提示"重启容器生效"。
- **一致性要求**：dsh 的 shell 工具（`tool-bash`）执行命令时继承容器内已设置的环境变量，确保 AI 编译时拿到的是同一套环境。

**注意：** 首次部署后先跑 `apt update && apt upgrade`，再按需安装工具；不要安装完整桌面环境（非必需，浪费空间）。

#### 7.2.5 aapt2 / NDK 的 ARM64 替代策略（详案，已核实 2026-08-13）

> **结论先行：Google 官方发布的 build-tools（含 aapt2）只有 linux-x86_64 版本；NDK 官方也只有 linux-x86_64 host 工具链。两者官方均无 arm64 Linux 版。** 但均有成熟替代方案，不会卡死编译链路。AOSP 自身只随发布 linux-x86 prebuilt（Commit451 项目 2026-05 实测确认，Soong 无法产出 glibc-arm64 host 工具）。

**按项目类型分层（先判断装什么）：**

| 项目类型 | 需要 aapt2？ | 需要 NDK？ | arm64 处理 |
|---|---|---|---|
| 纯 Java/Kotlin 项目（无 native 代码） | ✅ | ❌ **不需要** | 只解决 aapt2；NDK 完全不装（省 1GB+） |
| 含 C/C++（JNI / native）项目 | ✅ | ✅ | 两者都要解决 |

**方案 A：社区预编译 drop-in 二进制（首选，零编译）**
- **aapt2 / aidl / zipalign / split-select**：项目 `Commit451/android-arm-build-tools`
  - 用 CMake + `gcc-aarch64-linux-gnu` 交叉编译出的 **glibc-arm64** 二进制，与官方 SDK 目录同构；
  - "drop-in"：把文件替换进 `<SDK>/build-tools/<version>/` 对应文件名即可，Gradle/AGP **无需任何改动**；
  - 持续维护（2026-06 仍在更新），已覆盖到 build-tools 37.0.0；
  - 备用源：`hamza72x/android-sdk-linux-arm64`（⚠️ 2026-04 已放弃维护，仅作兜底）。
- **NDK host 工具链（如确需完整官方工具链）**：`SnowNF/ndk-aarch64-linux`（r29，从 AOSP `llvm-toolchain` 源码构建 arm64 host）——构建复杂、体积大，仅在必须时选用。

**方案 B：Box64 转译官方 x86_64 二进制（兜底，零编译、任何版本可用）**
- 容器内安装 Box64（apt 或源码编译），并在 chroot 内**手动注册 binfmt_misc**（Android 无 systemd，需 `mount -t binfmt_misc` + 写入 register 规则）；
- 用 `box64 <path>/aapt2` 直接运行官方 x86_64 的 aapt2/zipalign（社区已在 arm64 Debian chroot 实测跑通完整命令行 APK 构建）；
- **纯 Java 工具 d8 / apksigner 是 Java 程序，arm64 原生直接跑，无需转译**；
- 优点：不依赖社区版本、任何 build-tools/NDK 版本都能用；缺点：转译有约 20–50% 性能损耗，NDK 的 clang/lld 等复杂工具转译可能有兼容问题，编译大项目明显变慢。

**方案 C：混合工具链（NDK 专用，跳过 NDK host 工具）**
- 不运行 NDK 里的 x86_64 host 工具；改用**系统原生 arm64 clang / cmake / ninja**，只借用 NDK 的 **sysroot / 头文件 / libc++ / native_app_glue**；
- 用自定义 CMake toolchain 文件交叉编译到 `aarch64-linux-android` 目标（社区 `babaolu/arm-ndk` 已验证：S23 Ultra 上跑通带 Vulkan 的 native 工程）；
- 适用于"需要 native 但不想折腾 NDK host 工具"的场景。

**第一版推荐落地路径：**
1. 工具界面默认推荐 **AGP 8.7.2 + Gradle 8.9 + JDK 17 + Build-Tools 34.0.0**；Build-Tools 下载后自动用**方案 A** 的 drop-in 二进制替换 aapt2/aidl/zipalign（界面校验 SHA）；
2. 纯 Java/Kotlin 项目：只装 SDK，**不装 NDK**；
3. 含 native 项目：先试 方案 A（社区 NDK）→ 不行再 方案 C（混合工具链）→ 最后 方案 B（Box64 全转译）；
4. 工具界面把"官方无 arm64"标为黄标，显示"已用社区 arm64 版 / Box64 转译"的替代来源，可一键切换。

**实测证据（供实现阶段引用）：**
- felix021 gist（2026-05）：arm64 Debian chroot 内 Box64 转译 aapt2/zipalign + 原生 d8/apksigner，纯命令行完成 APK 构建；
- Commit451 MIGRATION（2026-05-20）：确认 AOSP 只发 linux-x86 prebuilt、Soong 无 glibc-arm64 输出，改 CMake 交叉编译后产出可用的 glibc-arm64 aapt2。

### 7.3 技能（skill）方案

dsh 技能机制（见 `packages/skill/skill`、`skill-filesystem`、`tool-skill`）：
- 技能 = `SKILL.md`（或扁平 Markdown）文件，放在规定的 skill 根目录，由 `dsh-skill-filesystem` 扫描，`dsh-tool-skill` 向模型暴露目录 + `skill` 加载工具。
- **发现优先级（rank）**（官方文档确认）：

| Rank | 来源 | 路径 |
|---|---|---|
| 100 | 项目 | `<projectRoot>/.dsh/skills` |
| 200 | 项目 | `<projectRoot>/.agents/skills` |
| 300 | 自定义 | `Config.customSkillDirs` |
| 400 | 用户 | `<dshHome>/skills` |
| 500 | 用户 | `<agentsHome>/skills` |
| 600 | 内置 | `Config.bundledSkillDir` |

**第一版技能策略：**
- 在 App 内设一个"技能库"入口（映射到 `<dshHome>/skills` 或 `customSkillDirs`），用户把 `SKILL.md` 放进去即生效。
- 复用仓库自带示例技能（如 `examples/` 或社区技能）；`dsh-skill-badge` 默认禁用，不启用。
- Web UI 已有 `ui-skill` 界面，App 里开箱可用，无需自建技能管理 UI。

### 7.4 插件（plugin）方案

dsh 一切皆插件（Cordis 组合）。默认组合（`dsh-base` + `dsh-web-app`）已内置大量能力，**第一版无需自研插件**，只需"挑开/关"：

**第一版默认开启（base 组合已含）：** llm/session/typert、settings、credentials、subprocess、sandbox(+policy)、bash(+sandbox)、tool-bash、tool-fs、tool-fs-search、tool-web、tool-todo、tool-skill、goal、plan-mode、compaction、subagent、workflow、tool-jobs、timeout-policy、web(search) 等。

**第一版要改的开关：**
| 行 id | 默认 | 第一版动作 | 理由 |
|---|---|---|---|
| `skill-badge` | disabled | 保持 disabled | 不需要徽章技能 |
| `web` / `tool-web` | enabled（fetch:false） | 保持，可开 fetch | 搜索可用；fetch 默认关（SSRF 防护），按需开 |
| `hmr`（web） | disabled | 保持 disabled | 生产运行无需热重载 |
| `session-query-sqlite` | `:memory:` + openAt:never | **改为持久化**（`path` 指向手机本地文件 + `openAt: first-search`） | 让"会话全文搜索"可用，落盘手机 |

**安装第三方插件的官方入口：**
- `dsh plugin --profile <name> add <package-or-git-spec>`（CLI reference 确认）
- 插件通过自己的 `cordis.patch.yml` 贡献层；App 设置页可直接调用该命令或写配置文件。

### 7.5 MCP（Model Context Protocol）方案

dsh 原生支持 MCP 客户端桥接：`@deepseek-ai/dsh-mcp-client`（见 `packages/mcp/mcp-client/README`）。
- 传输：`stdio`（本机启动子进程，如 `npx -y @modelcontextprotocol/server-xxx`）或 `streamable-http`（连接远程 URL）。
- 模型看到的工具名：`mcp__<serverName>__<rawName>`。
- 配置方式：在 profile 的 `cordis.patch.yml`（或 overlay）加一行插件实例；App 设置页可提供"添加 MCP 服务器"表单，写入该配置。

**第一版建议预置的 MCP（全部自选、默认关，用户按需开）：**

| 服务器 | 类型 | 说明 | 参考配置 |
|---|---|---|---|
| MCP Reference Memory（官方 `@modelcontextprotocol/server-memory`） | stdio | 本地知识图谱记忆（实体/关系/观察） | `examples/mcp-memory/mcp-reference-memory.cordis.yml` |
| Memorix | stdio | 本地启发式记忆（无 LLM 依赖） | `examples/mcp-memory/memorix.cordis.yml` |
| filesystem（官方） | stdio | 文件系统工具（补充 dsh 自带 fs） | 通用模板 |
| fetch（官方） | stdio | 网页抓取（若 dsh 内置 fetch 保持关闭） | 通用模板 |

> ⚠️ 重要事实（已核实）：仓库 `packages/mcp/mcp-client/README` 明确 —— **"Tools 是唯一被桥接的 MCP 能力；Resources 和 Prompts 尚无 harness 消费接口，暂缓"**。所以第一版只承诺"MCP 工具可用"，不承诺 MCP 的 resource/prompt 能力。
>
> ⚠️ 记忆相关的 MCP 示例是"默认关闭的第三方参考配置"，收录不代表官方背书；是否启用由用户决定。这些配置只在第一版提供"入口"，**真正的跨会话记忆功能仍列入后续阶段**（见 6.2）。

**App 设置页需要提供的 MCP 管理能力：**
- 列出已配置的 MCP 服务器（`serverName` / transport / 状态）
- 添加/删除：填 serverName + transport +（stdio: command/args/env/cwd）或（http: url/headers）
- 展示模型可见工具名 `mcp__<serverName>__<tool>`，方便用户理解

### 7.6 安全与隔离方案（root + chroot 场景）

> root 权限很大，chroot 提供了一层边界但不是安全沙箱。本项目采用"隔离为主 + 最小提权 + 备份兜底"三层策略。

| 层次 | 措施 |
|---|---|
| 容器隔离 | AI（dsh 及其 shell 工具）只能在 **chroot Ubuntu 容器内**读写/执行；容器是独立文件系统，天然隔离手机本体 |
| 只读挂载 | 手机本体的 `/sdcard/Download` 以**只读**方式挂载进容器，是**唯一**可访问的手机目录；DCIM/Documents/系统分区等均不可见 |
| 最小提权 | `su` 仅在**启动容器 / 备份打包 / 首次部署**时使用；日常对话时 dsh 在容器内以普通用户（非 root）运行 |
| 端口封闭 | dsh Web 服务只绑定 `127.0.0.1:3080`，不对外网/局域网开放 |
| 凭据保管 | DeepSeek API Key 走 dsh 的 credentials 机制（`$DSH_HOME/.credentials.yaml`），不写入代码与日志 |
| 备份兜底 | 容器 + 配置 + 会话日志可一键打包恢复（见 7.7），即使容器损坏也能回到最近状态 |

**明确不做的安全边界（与用户确认一致）：**
- 不做无障碍点击操控其它 App（避免把 root + 自动化叠加到系统 UI）。
- 不授予 AI 对手机本体的写权限；图库/文档目录默认不可访问（后续如需，再走临时授权）。

### 7.7 备份 / 恢复方案（第一版纳入）

**备份内容（一个归档包）：**
1. chroot Ubuntu 容器（`/data/local/dsh-container`）
2. dsh 配置（`$DSH_HOME`：settings、credentials、presets）
3. 会话日志与技能（`$DSH_HOME/sessions`、`skills`）

**备份方式：**
- App 内"一键备份"：`su -c` 停止容器 → `tar czf` 打包上述目录 → 存到 `/sdcard/Download/dsh-backup-<日期>.tar.gz`（方便用户拷贝走或换机）。
- 恢复流程：新手机装 App → 选择备份文件 → `su -c` 解包到原路径 → 重启容器 → 会话与配置原样恢复。

**触发与提醒：**
- 手动一键备份（主界面入口）+ 建议"升级/更换容器前先备份"提示。
- 不做自动定时备份（第一版）；后续可加"每周自动备份到 Download"。

### 7.8 应用 UI 设计方案（混合模式：原生壳 + dsh 网页主体）

> **界面架构（用户 2026-08-14 拍板）：混合 —— 原生壳 + 网页主体。** 聊天主体直接嵌入 dsh 自带 Web UI（不重写，思考强度、模型/密钥/地址设置、plan、子代理、技能等 dsh 功能全部免费保留、随 dsh 升级自动更新）；原生层（Material 3 壳 + 抽屉 + 功能页）只做手机增强：容器状态、工具管理、镜像源、环境变量、备份勾选、首次安装向导、文件发送（系统选择器）。
>
> 因此 UI 工作集中在：**聊天壳（顶栏/状态/抽屉）+ 设置 + 工具管理 + 环境变量 + 备份 + 首次安装向导** 这几块原生页面 + **WebView 桥接**（文件发送、容器状态注入网页主体）。

**已确认的 UI 决策（用户 2026-08-13 / 2026-08-14）：**

| 决策点 | 结论 |
|---|---|
| 界面架构 | **混合**：原生壳 + dsh 网页主体（WebView 嵌 dsh Web UI，聊天与模型相关功能随 dsh 自带） |
| 主界面结构 | **单聊天界面**：打开 App 直接就是聊天；工具管理/设置等收进右上角菜单（抽屉） |
| 视觉风格 | 原生壳用 **Material 3 原生风格**，跟随系统动态色/字体；网页主体保持 dsh Web UI 原样 |
| 深色模式 | **跟随系统自动切换** + 设置页提供**手动开关**（跟随系统 / 浅色 / 深色） |
| 思考强度 | 由 **dsh 网页主体自带**（ModelSelect：模型 · 思考强度两级选择，含"跟随模型默认"）；不重复造 |
| 模型/Key/地址 | 由 **dsh 网页主体自带**（设置 → 模型：添加提供方、填 API 密钥、改 endpoint）；原生设置页只放**跳转入口**（用户 2026-08-14 确认：全部交给 dsh 网页界面，不原生重做表单） |
| 保留的 dsh 功能 | **plan 规划模式 / 子代理 Subagents / 技能 Skills / MCP 工具** 为确认重点（用户 2026-08-14），混合架构下全部随 dsh 网页主体保留、随 dsh 升级自动更新 |
| 文件发送 | **系统文件选择器（SAF）**：点"发送文件"弹 Android 系统选择器，选中的文件复制进容器工作区再交 AI（见 7.6 隔离边界） |
| 下载源 | 工具下载源做成**设置项**：用户可选 清华 / 阿里 / 中科大 / 官方，界面显示各源**连通状态**，提供**「全部测速」按钮**（一次测所有源延迟） |
| 备份内容 | **可勾选打包**：容器环境 / 配置 / 会话记录 / 已装工具 四项可勾，**默认只勾 配置+会话**（小快备份），可自行加容器/工具 |
| 名称/图标 | 临时名 `dsh Mobile` + 默认应用图标；后续再单独设计 |

**页面清单与交互：**

| 页面 | 内容 | 关键交互 |
|---|---|---|
| ① 聊天主界面 | 原生顶栏（右上角**容器状态绿点** + 菜单按钮）+ **网页主体**（dsh Web UI：对话 + composer）；composer **输入框单独一行（在上）**，**工具栏一行（在下）**：模型/思考强度选择器 + 发文件 + 语音 + 发送；顶部仅一条栏，不叠加 | 原生壳与网页主体通过 WebView 桥接（容器状态、文件发送）；网页主体内一切交互复用 dsh |
| ② 右上角菜单（抽屉） | 工具管理 / 镜像源 / 环境变量 / 备份恢复 / 设置 / 重启容器 / 更新工具 | 列出入口，点击跳对应页；"重启容器"为操作项、"更新工具"跳 ⑤ 工具管理页 |
| ③ 首次安装向导 | 多步引导：解包 rootfs → 填 API Key → 启动服务 → 进入聊天 | 每步进度 + 失败可重试；完成后直达① |
| ④ 设置页 | **模型与密钥（入口，跳 dsh 网页主体设置）**、工作目录、服务端口、深色模式开关、工具下载源 | 下载源单选 + **「全部测速」按钮**（一次测所有源，逐行显示延迟 ms + 连通状态）；模型入口跳网页主体设置 |
| ⑤ 工具管理页 | **可更新清单（显示哪些工具能更新：当前版本 → 新版本）**、一键更新（带进度条）、SDK/NDK 已装列表（版本/占用空间）、安装（推荐组合/**自选版本**）、卸载、arm64 替代来源标注 | 版本可下拉选择；替代来源标黄（见 7.2.5）；镜像源见 ④ |
| ⑥ 环境变量编辑器 | 键/值列表，新增/修改/删除 | 改后提示"重启容器生效"（见 7.2.4） |
| ⑦ 备份/恢复页 | **打包内容勾选**（默认只勾配置+会话）、一键备份、历史备份**恢复 / 删除（带确认弹窗）**、进度条 | 勾选变化实时更新预计体积；备份产物说明 + 换机引导（见 7.7） |

**UI 设计要点：**
- 原生壳用 Compose + Material 3 组件（TopAppBar / NavigationDrawer / Card / Dialog / Snackbar / Checkbox / RadioButton），不引第三方 UI 库；
- 深色模式：`isSystemInDarkTheme()` + 手动覆盖的 `darkTheme` 状态（跟随/浅/深三态），主题色用 Material3 默认配色；
- **原生层与网页主体职责分离**：一切"dsh 能力"（模型/思考强度/密钥/plan/子代理/技能）交给网页主体，原生层不复制、不拦截，只做壳与手机增强；
- 原生层向网页主体注入：容器状态（运行中/停止）、文件发送（SAF 选中后复制进容器工作区并回填路径给 composer）；
- 全中文界面；面向非开发者的文案（如"正在安装 Linux 环境，约需几分钟"）。

**UI 设计工作拆分（独立排期）：**
1. 设计阶段（页面结构 + 交互流程 + 原型）：对应阶段 1 的 UI 设计任务；
2. 实现阶段（各页 Compose 编码 + WebView 桥接）：随阶段 2 各功能页一起落地；
3. 验收阶段（完整 UI 流程走查）：见 9.验证方式第 3 条。

---

## 8. 实施步骤（分阶段任务）

> 以下为"若推进到实现"时的路线图；当前交付物为本方案文档。

### 阶段 0：环境准备
- [ ] 沙箱安装 Android SDK + 构建工具（约 20-30 分钟，Java 22 + Gradle 8.14 已具备）
- [ ] 在已 root（KernelSU）的真机上验证 chroot 方案可行性：`su -c` 进入 Ubuntu 24 rootfs，安装 Node ≥ 22 并跑通 dsh 最小例子（作为可行性验证）

### 阶段 1：最小骨架
- [ ] **UI 设计先行（独立工作项，见 7.8）**：页面结构 + 交互流程 + 原型（聊天主界面/右上角菜单/设置/工具管理/环境变量/备份/安装向导），Material 3 主题 + 深色模式（跟随+手动三态）
- [ ] 新建 Android 工程（Kotlin + Compose），实现"WebView 加载 dsh Web UI"的最小壳
- [ ] 实现聊天主界面壳：顶部状态条（容器状态/联网）+ 右上角菜单（抽屉）入口
- [ ] 实现"首次安装向导"：`su -c` 部署 chroot Ubuntu 24 rootfs + Node.js + dsh 核心（自动化脚本 + 引导 UI）
- [ ] 实现服务守护：`su -c` 启动 chroot 容器 + dsh（`dsh web`）、健康检查、停止、日志查看

### 阶段 2：功能落地（各功能页均为原生 Compose UI + WebView 桥接，见 7.8 页面清单 ④–⑦）
- [ ] 设置页 UI：**模型与密钥（入口，跳 dsh 网页主体设置）** / 工作目录 / **深色模式开关** / 服务端口 / **工具下载源（清华/阿里/中科大/官方 + 连通状态 + 「全部测速」按钮）**
- [ ] 实现隔离边界：只读挂载 `/sdcard/Download`，验证其它手机目录不可访问；**文件发送经系统选择器（SAF）复制进容器工作区**后回填路径给 composer
- [ ] 备份/恢复页 UI：一键备份/恢复 + **打包内容勾选（默认只勾配置+会话）** + 历史备份删除（带确认弹窗），可跨机恢复
- [ ] 工具管理页 UI：**可更新清单（哪些工具能更新 + 当前版本→新版本）** + **一键更新（带进度条）**、查看/安装/卸载 SDK·NDK 等，内置版本兼容矩阵 + 自动推荐组合 + **自选版本下拉** + arm64 替代来源标注（见 7.2.2/7.2.3/7.2.5）
- [ ] 环境变量编辑器 UI：预置默认 + 可视化增删改（见 7.2.4），验证 dsh shell 工具继承同一环境
- [ ] 验证：对话、工具调用、Shell 命令、skill/MCP 加载在 App 内全部可用
- [ ] 崩溃/重启恢复：服务异常自动拉起，会话日志不丢

### 阶段 3：验收与收尾
- [ ] 在已 root 真机验证全流程（含重启手机后容器自动拉起）
- [ ] 端到端编译验证：App 内克隆一个 Android 项目 → 自动安装兼容的 SDK/NDK 组合 → 本地 `gradle build` 出 APK
- [ ] 打包自用 APK（自签名即可，无需上商店）
- [ ] 补充使用文档（面向非开发者：安装→填 Key→使用→编译）

---

## 9. 验证方式（如何证明"能在手机上用"）

1. **可行性验证（阶段 0）**：在已 root（KernelSU）真机上，`su -c` 进入 chroot Ubuntu 24，能启动 dsh 并完成一次对话 → 证明"手机本地跑 dsh + chroot 容器"成立。
2. **隔离验证（阶段 2）**：容器内确认 `/sdcard/Download` 可读、其它手机目录（如 DCIM、Documents）不可访问、不可写手机本体。
3. **功能验收（阶段 2）**：App 内完成一个端到端任务——例如"在容器内新建一个文件夹，写一个脚本并运行，输出结果到对话里"。
4. **UI 流程走查（阶段 2/3，见 7.8）**：首次安装向导 → 聊天主界面 → 右上角菜单 → 设置（含深色模式手动切换即时生效）→ 工具管理 → 环境变量编辑 → 备份/恢复，全流程可走通、全中文、无卡死。
5. **编译工具链验收（阶段 3）**：App 内通过工具界面自动推荐并安装 SDK/NDK 组合 → 克隆一个 Android 项目 → `gradle build` 成功产出 APK（验证 arm64 工具链 + 环境变量 + 版本矩阵端到端可用）。
6. **稳定性**：App 杀掉重开后，会话历史仍在（依赖 dsh session 持久化）。
7. **备份恢复**：一键备份 → 清空容器 → 一键恢复 → 会话与配置原样还原。
8. **安全**：端口仅 localhost 监听（`netstat` 验证）；`su` 提权仅在启动/备份/装 SDK 时发生（日志可查）。

---

## 10. 风险与对策

| 风险 | 等级 | 对策 |
|---|---|---|
| 普通手机性能不足以支撑 Node + dsh 长跑 | 中 | 最小可用版只跑轻量任务；本地模型不纳入第一版；建议用闲置机跑 |
| chroot + KernelSU 环境兼容问题（rootfs 拉取、su 提权） | 中 | 阶段 0 先做真机可行性验证；卡住则回退 proot（免 root 但更重） |
| 部分编译工具**官方无 arm64 版本**（aapt2/NDK 已核实确认） | 中 | 版本矩阵标注 arm64 可用性；aapt2/NDK 用"社区 drop-in + Box64 兜底 + 纯 Java 免 NDK"策略（见 7.2.5） |
| SDK/NDK 按需安装后占用大量空间 | 中 | 工具界面支持**卸载版本**释放空间；首推自动推荐的最小兼容组合 |
| root 误操作风险（容器边界被突破） | 中 | 隔离 + 只读挂载 + 最小提权 + 一键备份兜底（见 7.6/7.7） |
| 容器包 + 按需下载体积大、首启/首次编译耗时长 | 低 | rootfs 压缩包化，按需下载仅 SDK/NDK；进度可视化 |
| WebView 与 dsh Web UI 交互（剪贴板/文件选择） | 低 | 用 WebView JavaScriptBridge 补齐原生能力桥 |
| 云端 API 依赖网络 | 低 | 明确告知离线不可用；后续可加本地模型路线 |
| 沙箱无 Android SDK | 中 | 已确认可安装；如需实际打包，先执行阶段 0 |

---

## 11. 与参考对象的边界（不做什么）

- **不做**无障碍点击操控其它 App（安全原因，用户已确认"不要"）。
- **不做**手机本地大模型推理（第一版；用户选云端 API，且普通手机跑不动大模型）。
- **不重写**对话界面（复用 dsh Web UI）。
- **不改** dsh 核心循环（遵守仓库约定：插件优先，不轻易动 agent-loop）。
- **不放开** AI 对手机本体的写权限（仅只读 `/sdcard/Download`，用户已确认隔离边界）。

---

## 12. 待办 / 开放问题

- [ ] 用户确认本方案文档后，再决定是否进入"阶段 0 可行性验证"或直接开始搭建 App 工程。
- [ ] 确定 App 名称与包名（自用即可，如 `com.self.dshmobile`）。
- [x] 内置版本矩阵：已按官方数据校准 AGP 8.4–9.2 ↔ Gradle ↔ JDK 17 ↔ Build-Tools 34/35/36 的对应关系，并给出首推稳定组合（见 7.2.3）。
- [x] aapt2 / NDK host 工具链的 arm64 官方支持情况已核实（2026-08-13）：官方均无 arm64 Linux 版；替代源与落地路径见 7.2.5（社区 drop-in → 混合工具链 → Box64）。
- [ ] 真机实测 Community drop-in 二进制（Commit451 aapt2）在 chroot Ubuntu 24 内的 `gradle build` 全流程（作为阶段 3 验收项）。
- [ ] 应用 UI：方向已定（单聊天界面 + Material 3 + 深色三态，见 7.8）；进入实现前先出**页面原型/线框图**（7.8 页面清单 ①–⑦），并确认 App 名称与图标（先临时名 `dsh Mobile`）。
- [ ] 确定 rootfs 随 APK 打包的压缩方式与体积优化（基础工具 + dsh 预装，目标 400–800MB）。
- [ ] 若后续要做跨会话记忆，需按 dsh 的"模型可见⟺已记录"原则设计新的 session 事件（届时参考 `.agents/notes` 与 session 文档）。
