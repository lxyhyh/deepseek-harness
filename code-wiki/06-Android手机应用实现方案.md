# Android 手机应用实现方案

> 版本：v0.2（细化版：系统/工具/技能/插件/MCP 详案）
> 日期：2026-08-13
> 分支：`android`

## 1. 一句话目标

**基于 dsh（DeepSeek Harness）框架，在 Android 手机上本地运行 agent，做一个"手机就是主机"的一体化 App** —— 用户平时像用 Operit AI 一样打开 App 使用，但底座是 dsh 的开源框架。

---

## 2. 已确认的决策（来自需求澄清）

| 决策点 | 结论 |
|---|---|
| 交付物 | **实现方案文档**（本文档），本轮不写代码、不打 APK |
| 架构形态 | **手机本地跑 dsh 核心**（手机即主机，参考 openclaw-termux 模式） |
| 操作入口 | **手机上装一个一体化 App**（不是纯浏览器） |
| AI 大脑 | **云端 API**（DeepSeek 等），联网使用，不依赖本地模型 |
| 核心能力 | AI 对话 + 工具调用、内置 Linux 执行环境、插件/MCP 生态 |
| 是否操控其它 App | **不要**（不做无障碍点击操控，降低权限与误操作风险） |
| 跨会话记忆 | **后续再加**（第一版用 dsh 自带会话日志即可） |
| 用途 | **自用**，不分发、不上商店 |
| 第一版范围 | **最小可用版**：能打开对话、能调工具、能跑 Linux 命令、能装 MCP 插件 |
| 安装形态 | **一体化 App**（自动装环境，体验接近 Operit；不用用户手动装 Termux） |

---

## 3. 需求背景与参考对象

### 3.1 为什么在 dsh 之上做，而不是照搬 Operit

- Operit 是**闭源原生 App**：内置 chroot Linux + 本地模型推理 + 无障碍自动化，整套是私有实现，无法在 dsh 上复用。
- dsh 是**开源框架**：已具备 agent 循环、会话日志（session log）、工具系统、shell/subprocess 能力、skill 插件注册表、Web UI、MCP 工具消费等基础。
- 我们的方案：**把 dsh 当作底座，在手机本地把 dsh 跑起来**，再套一层移动端外壳，避免重复造轮子，同时获得 dsh 的会话/日志/插件生态。

### 3.2 参考 agent 的可借鉴点（不是照搬，是取舍）

| 参考对象 | 可借鉴能力 | 本项目取舍 |
|---|---|---|
| **Operit AI** | 手机本地一体化体验、chroot Linux 环境、App 化安装 | 借鉴"手机即主机 + App 一体化"；**不做**无障碍点击操控、不做本地模型推理 |
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
│  │  ├─ 环境管理：首次启动自动引导安装 dsh 运行时         │  │
│  │  ├─ 服务守护：后台启动/停止 dsh 服务                 │  │
│  │  └─ 设置页：API Key、模型选择、目录选择             │  │
│  └──────────────────┬─────────────────────────────────┘  │
│                     │ HTTP/WebSocket (localhost)          │
│  ┌──────────────────▼─────────────────────────────────┐  │
│  │     dsh 运行时（在手机内 Linux 环境中运行）           │  │
│  │  ├─ Node.js (>=22.19)                               │  │
│  │  ├─ dsh 核心（agent 循环 / session / 工具 / 插件）    │  │
│  │  ├─ dsh Web 服务（端口 3080）                        │  │
│  │  └─ shell/subprocess 能力（执行 Linux 命令）         │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Linux 运行环境（Termux 或 proot 轻量发行版）          │  │
│  │  文件系统 / Python / git / 常用工具                  │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
             │
             ▼  HTTPS（仅指令与结果走云端）
     DeepSeek 等云端大模型 API
```

**关键点说明：**
- 手机里的 Linux 环境是"执行层"，dsh 的 shell 能力在这个环境里跑命令、读文件——这就是"内置 Linux 环境"。
- App 用系统 WebView 加载 dsh 自带 Web UI，天然获得对话、工具卡片、会话历史展示，**不用重写 UI**。
- 所有数据（会话日志、配置）留在手机本地；只有请求大模型时，把对话指令发给云端 API（与 Operit 云端模式一致）。
- 不做无障碍操控，因此 App 权限极小（仅网络 + 本地文件），误操作风险低。

---

## 5. 技术选型

| 层 | 选型 | 理由 |
|---|---|---|
| 手机 App | Kotlin + Jetpack Compose | 现代 Android 官方栈，一体化体验 |
| WebView | Android 系统 WebView + JavaScriptBridge | 复用 dsh Web UI，无需重做界面 |
| Linux 环境 | Termux（推荐） 或 proot-distro（Ubuntu） | Termux 最轻、apt 可用；后续需要更完整发行版时用 proot |
| dsh 运行时 | Node.js ≥ 22.19（Termux 提供） | dsh 硬性要求（见仓库 AGENTS.md） |
| 数据 | dsh 自带 session 持久化（SQLite/JSONL） | 会话历史本地落盘，天然满足"模型可见⟺可记录" |
| 构建 | Gradle + Android SDK（沙箱需先安装 SDK） | Android 官方构建链 |
| 目标设备 | 普通 Android 手机（API 26+，约 Android 8.0+） | 覆盖主流机型 |

---

## 6. 功能范围

### 6.1 第一版（最小可用版）—— 本轮方案的交付范围

| 模块 | 功能 | 备注 |
|---|---|---|
| 环境安装 | 首次启动自动引导安装 Linux 运行时 + Node.js + dsh | 一体化 App 的核心体验 |
| 服务管理 | 一键启动/停止 dsh Web 服务（127.0.0.1:3080） | 常驻运行，断网重连稳定 |
| 对话界面 | WebView 加载 dsh Web UI，正常聊天 | 复用 dsh，不重写 UI |
| 工具调用 | dsh 自带工具（fs/shell/web/subprocess 等）开箱即用 | 对齐 dsh 能力 seam |
| 内置 Linux | 能执行 Shell 命令、跑脚本、装工具 | 通过 dsh shell 能力 |
| 插件/MCP | 能在 App 内配置并加载 skill 插件 / MCP 服务器 | 复用 dsh 插件注册表 |
| 设置页 | API Key、模型选择、工作目录、启动/停止 | 原生设置页（非 WebView） |
| 安全 | 服务仅绑定 localhost，禁止外部访问 | 与 Operit 云端模式一致的安全性 |

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
| 手机宿主 OS | Android 8.0+（API 26+） | 覆盖主流机型；自用无需商店合规 |
| Linux 运行时 | **Termux**（推荐，第一版） | 轻量、apt 可用、有 Node.js 官方包；App 通过其 API 自动管理 |
| 备选 | **proot-distro / Ubuntu** | 需要更完整发行版时（后续阶段）切换；dsh 无系统绑定 |
| Node.js | `nodejs-lts`（≥ 22.19） | dsh 硬性要求（仓库 AGENTS.md）；Termux 提供 LTS |
| 包管理器 | pnpm（Node 自带 corepack） | 用于安装 dsh 及其 workspace 依赖 |

**安装决策：**
- 第一版用 Termux，因为 App 可以**自动引导安装**（下载 Termux + 运行 `pkg install`），体验接近 Operit 的一体化安装。
- dsh 本体是纯 JS/ESM，无强制原生模块，Termux 上 Node 兼容性风险集中在个别依赖；阶段 0 已验证此风险点。

### 7.2 安装什么工具（Linux 执行层常用工具）

> 这些工具由 dsh 的 shell 能力（`tool-bash` → `dsh-bash-sandbox` → `dsh-subprocess-local`）在 Termux 环境内执行。默认组合已启用 `tool-bash`、`tool-fs`、`tool-web` 等（见 base/cordis.patch.yml）。

| 工具 | 用途 | 安装来源 |
|---|---|---|
| `git` | 版本管理、拉取插件/技能 | `pkg install git` |
| `python` | 跑 Python 脚本（dsh 有 python/ SDK） | `pkg install python` |
| `node` / `npm` / `corepack` | 运行 dsh 本体 + MCP 服务器（stdio） | `pkg install nodejs-lts` |
| `curl` / `wget` | 网络、下载 | `pkg install curl` |
| `openssh` | （可选）远程排障 | `pkg install openssh` |
| `ripgrep`(`rg`) / `fd` | 快速搜索（dsh 的 fs-search 工具可调用） | `pkg install ripgrep` |
| `bash` / `coreutils` / `file` | shell 基础 | Termux 自带 |

**注意：** 不要在第一版安装 `proot`/`chroot` 根发行版——不必要；除非后续要完整 Ubuntu 或跑 GUI 应用。

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

---

## 8. 实施步骤（分阶段任务）

> 以下为"若推进到实现"时的路线图；当前交付物为本方案文档。

### 阶段 0：环境准备
- [ ] 沙箱安装 Android SDK + 构建工具（约 20-30 分钟，Java 22 + Gradle 8.14 已具备）
- [ ] 用 Termux 在真机/模拟器验证：`pkg install nodejs-lts` 后能跑通 dsh 最小例子（作为可行性验证）

### 阶段 1：最小骨架
- [ ] 新建 Android 工程（Kotlin + Compose），实现"WebView 加载 dsh Web UI"的最小壳
- [ ] 实现"首次安装向导"：检查/引导安装 Linux 运行时 + Node.js + dsh 核心（自动化脚本）
- [ ] 实现服务守护：启动 dsh（`dsh web` 或等价入口）、健康检查、停止、日志查看

### 阶段 2：功能落地
- [ ] 设置页（API Key / 模型 / 工作目录）写入 dsh 配置（cordis.yml 覆盖）
- [ ] 验证：对话、工具调用、Shell 命令、skill/MCP 加载在 App 内全部可用
- [ ] 崩溃/重启恢复：服务异常自动拉起，会话日志不丢

### 阶段 3：验收与收尾
- [ ] 在普通手机真机验证全流程
- [ ] 打包自用 APK（自签名即可，无需上商店）
- [ ] 补充使用文档（面向非开发者：安装→填 Key→使用）

---

## 9. 验证方式（如何证明"能在手机上用"）

1. **可行性验证（阶段 0）**：Termux 中能成功启动 dsh 并完成一次对话 → 证明"手机本地跑 dsh"成立。
2. **功能验收（阶段 2）**：App 内完成一个端到端任务——例如"在手机内新建一个文件夹，写一个脚本并运行，输出结果到对话里"。
3. **稳定性**：App 杀掉重开后，会话历史仍在（依赖 dsh session 持久化）。
4. **安全**：端口仅 localhost 监听，`netstat` 验证无外部监听。

---

## 10. 风险与对策

| 风险 | 等级 | 对策 |
|---|---|---|
| 普通手机性能不足以支撑 Node + dsh 长跑 | 中 | 最小可用版只跑轻量任务；本地模型不纳入第一版；建议用闲置机跑 |
| Termux 上 Node.js/原生模块兼容问题 | 中 | 阶段 0 先做可行性验证，卡住则换 proot 发行版 |
| WebView 与 dsh Web UI 交互（剪贴板/文件选择） | 低 | 用 WebView JavaScriptBridge 补齐原生能力桥 |
| 云端 API 依赖网络 | 低 | 明确告知离线不可用；后续可加本地模型路线 |
| 沙箱无 Android SDK | 中 | 已确认可安装；如需实际打包，先执行阶段 0 |

---

## 11. 与参考对象的边界（不做什么）

- **不做**无障碍点击操控其它 App（安全原因，用户已确认"不要"）。
- **不做**手机本地大模型推理（第一版；用户选云端 API，且普通手机跑不动大模型）。
- **不重写**对话界面（复用 dsh Web UI）。
- **不改** dsh 核心循环（遵守仓库约定：插件优先，不轻易动 agent-loop）。

---

## 12. 待办 / 开放问题

- [ ] 用户确认本方案文档后，再决定是否进入"阶段 0 可行性验证"或直接开始搭建 App 工程。
- [ ] 确定 App 名称与包名（自用即可，如 `com.self.dshmobile`）。
- [ ] 若后续要做跨会话记忆，需按 dsh 的"模型可见⟺已记录"原则设计新的 session 事件（届时参考 `.agents/notes` 与 session 文档）。
