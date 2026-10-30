# 🎯 xxl-job-demo

[![Java](https://img.shields.io/badge/java-21%2B-orange)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)](https://spring.io/projects/spring-boot)
[![XXL-JOB](https://img.shields.io/badge/XXL--JOB-3.5.0-blue)](https://github.com/xuxueli/xxl-job)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://www.docker.com/)

> XXL-JOB 示例项目，基于 **Spring Boot 4.1.1** + **Java 21** + **XXL-JOB 3.5.0** 构建。

演示 XXL-JOB 分布式任务调度的基本集成与使用，提供完整的本地测试环境（Docker Compose 一键启动）。

## ✨ 特性

- 🚀 基于 Spring Boot 4.1.1 + Java 21
- 📦 XXL-JOB 3.5.0 最新版本
- 🐳 Docker Compose 一键启动本地测试环境
- 🎯 最简单的任务示例（Hello World）
- 📊 XXL-JOB Admin 可视化管理界面

## 🚀 快速开始

### 🐳 方式一：一键启动全部服务（推荐）

一条命令拉起 **MySQL + XXL-JOB Admin + 执行器应用** 三个容器：

```bash
docker-compose up -d

# 查看状态
docker-compose ps

# 查看执行器日志
docker-compose logs -f xxl-job-demo
```

| 服务 | 访问地址 | 说明 |
| --- | --- | --- |
| XXL-JOB Admin | http://localhost:8090/ | 账号 `admin` / `123456` |
| 执行器应用 | http://localhost:8080/ | 启动后自动注册到 Admin |
| MySQL | localhost:3306 | 库 `xxl_job`，账号 `xxl_job` / `xxl_job123` |

容器启动顺序由 `depends_on` + `healthcheck` 保证：MySQL 就绪 → Admin 健康检查通过 → 执行器应用启动。

> 执行器镜像 `javawiki/xxl-job-demo:latest` 由 GitHub Actions 打 tag 时自动推送到 Docker Hub。本地改了代码需重新 `docker build` 后镜像才会更新。

### 💻 方式二：宿主机调试执行器

只启动 MySQL 和 Admin，执行器应用在宿主机（IDEA）中运行：

```bash
docker-compose up -d xxl-job-mysql xxl-job-admin
```

等待约 30 秒后访问控制台：
- **地址**：http://localhost:8090/
- **账号**：`admin` / `123456`

### 🔨 构建项目

```bash
mvn clean package -DskipTests
```

### ▶️ 运行应用

```bash
java -jar target/xxl-job-demo.jar
```

或使用 Maven：

```bash
mvn spring-boot:run
```

> 在宿主机运行时，Admin 容器无法回调宿主机网卡 IP，需按下方「Windows Docker + IDEA 联调问题」覆盖 `xxl.job.executor.ip`。
> 在容器内运行时无需设置，应用会自动探测容器 IP，Admin 同网络可直接回调。

### ⚙️ 配置任务

1. **执行器已预创建**
   - SQL 初始化脚本已预创建执行器分组 `xxl-job-demo`
   - 应用启动后自动注册到该分组，无需手动创建

2. **创建任务**
   - 进入「任务管理」→「新增」
   - 执行器：选择 `xxl-job-demo`
   - JobHandler：`helloworld`
   - Cron：`0/10 * * * * ?`（每 10 秒执行一次）

3. **执行测试**
   - 点击「执行一次」查看执行结果
   - 查看执行日志

## 🛑 停止环境

```bash
docker-compose down
```

> ️ **注意**：不要删除 `.env/mysql` 目录，否则数据库数据会丢失！

## 🐛 Windows Docker + IDEA 联调问题

### 场景

- **Admin**：Docker 容器内运行（`docker-compose up -d`）
- **执行器**：宿主机 IDEA 中调试运行

### 问题现象

Admin 触发任务时报错：
```
Http Request Error (Connect timed out). for url : http://172.19.144.1:9999/trigger
```

### 原因分析

执行器启动时自动检测本机 IP（如 `172.19.144.1`）并注册到 Admin。Admin 容器回调该地址时，**流量无法到达宿主机网卡 IP**，即使已开放 Windows 防火墙入站规则。

**根本原因**：Docker Desktop 的网络路由机制——容器访问宿主机时，流量通过 `com.docker.backend.exe` 进程转发，只能到达 Docker 宿主机网关地址，无法直接访问宿主机网卡 IP。

### 解决方案

**配置执行器 IP 为 Docker 宿主机网关地址**

获取宿主机网关地址：
```bash
# 在 Admin 容器内执行（宿主机 nslookup 结果不可用，以容器内为准）
docker exec xxl-job-admin sh -c "getent hosts host.docker.internal"
```
输出示例：`192.168.65.254  host.docker.internal`

按 Spring Boot 的标准环境变量覆盖规则设置 `xxl.job.executor.ip` 即可（松散绑定后即 `XXL_JOB_EXECUTOR_IP`），**配置文件里不需要声明任何占位符**：

```powershell
# PowerShell
$env:XXL_JOB_EXECUTOR_IP="192.168.65.254"; java -jar target/xxl-job-demo.jar
```

```bash
# Bash
export XXL_JOB_EXECUTOR_IP=192.168.65.254
java -jar target/xxl-job-demo.jar
```

或在 IDEA 运行配置的 **Environment variables** 中填入 `XXL_JOB_EXECUTOR_IP=192.168.65.254`。

> 同理，`xxl.job.admin.addresses` 可用 `XXL_JOB_ADMIN_ADDRESSES` 覆盖（compose 里就是这么把 Admin 地址指向 `xxl-job-admin:8080` 的），`xxl.job.accessToken` 对应 `XXL_JOB_ACCESSTOKEN`。

> 用 `docker-compose up -d` 在容器内运行执行器时无需配置该变量，应用会自动探测容器 IP，Admin 在同一容器网络内可直接回调，不会遇到上述问题。

## 📚 参考文档

- [XXL-JOB 官方文档](https://www.xuxueli.com/xxl-job/)
- [XXL-JOB GitHub](https://github.com/xuxueli/xxl-job)
- [阿里云 MSE XXL-JOB](https://help.aliyun.com/zh/mse/use-cases/use-cases-xxljob/)
