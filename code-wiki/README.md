# DeepSeek Harness 代码知识库（Code Wiki）

本目录是 **DeepSeek Harness**（`dsh`）项目的代码知识库，帮助开发者快速理解项目的整体架构、模块划分、核心实现、依赖关系与运行方式。

> 说明：DeepSeek Harness 是 DeepSeek AI 开发的一个**开源 Agent 运行框架（agent harness）**。它采用“一切皆插件”的架构，底层基于 [Cordis](https://github.com/cordiverse/cordis) 框架构建。本 Wiki 基于源码与官方文档整理，用于导航和快速上手。

## 文档目录

| 文档 | 内容 |
|---|---|
| [01-架构总览.md](01-架构总览.md) | 项目定位、核心架构思想（插件化、Cordis、Profile/Bundle）、运行时数据流 |
| [02-模块职责.md](02-模块职责.md) | 仓库布局、包分组（core / llm / fs / session 等）及每个包的职责 |
| [03-关键类与函数.md](03-关键类与函数.md) | 核心包（core）的关键类、接口、函数与事件说明 |
| [04-依赖关系.md](04-依赖关系.md) | 包间依赖结构、分层原则、依赖方向约束 |
| [05-运行方式.md](05-运行方式.md) | 安装、本地运行、构建、测试、发布等命令 |
| [06-Android手机应用实现方案.md](06-Android手机应用实现方案.md) | 「手机即主机」Android 客户端：架构决策、工程结构、实施状态 |

## 快速速览

- **一句话**：`dsh` 是一个可配置、可扩展、可持续化记录的 AI Agent 运行框架，从模型适配、工具执行、会话日志到前端 UI 全部由插件组成。
- **核心概念**：
  - **一切皆插件（Everything is a plugin）**：包括模型适配器、工具注册表、会话日志、Agent 主循环在内，每个部分都是可替换的插件。
  - **Cordis**：底层插件框架，提供上下文（`Context`）、服务（`Service`）、类型化事件（`Events`）与可逆副作用（`Effect`）。
  - **Profile 与 Bundle**：一个运行的 `dsh` 是在启动时按层级组合的插件树；`profile` 是命名的组合，`bundle` 是插件配置行的分发格式。
  - **会话日志（Session log）**：持久化真相源，模型看到的上下文全部可从此日志重建。
- **核心包**：`core/agent`、`core/agent-loop`、`core/session`、`core/system-prompt`、`core/tools`、`core/scope`。

## 相关官方文档（更深入）

- [README.md](../README.md) —— 项目首页
- [docs/architecture.md](../docs/architecture.md) —— 架构文档
- [docs/module-graph.md](../docs/module-graph.md) —— 模块依赖图（自动生成）
- [docs/subsystems/](../docs/subsystems/) —— 各子系统详细文档