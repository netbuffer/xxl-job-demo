# 项目 AI Agent 协作指南 (AGENTS.md)

本文档为 AI Coding Agent 提供项目核心上下文与代码规范。

## 1. 技术栈与服务拓扑

- **技术栈**: Java 21, Spring Boot 4.1.1, XXL-JOB 3.5.0, MySQL 8.0, Docker Compose
- **服务端口**:
  - MySQL: `3306`（库: `xxl_job`，账号: `xxl_job` / `xxl_job123`）
  - XXL-JOB Admin: `8090`（容器内 8080，控制台账号: `admin` / `123456`）
  - Demo 执行器: `8080`（Web 端口），`9999`（Netty RPC 回调端口）

## 2. 核心规范与约束

1. **日志规范**:
   - 类级别统一使用 Lombok `@Slf4j`。
   - `@XxlJob` 远程控制台日志必须使用 `XxlJobHelper.log("...", args)`，仅支持 SLF4J `{}` 占位符。
2. **长任务优雅终止**: 批处理与长耗时任务必须在循环中检测 `Thread.currentThread().isInterrupted()`，确保能响应 Admin「终止任务」动作。
