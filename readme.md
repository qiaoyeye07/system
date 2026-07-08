# 二手交易平台（Flea Market）

C2C 个人闲置物品交易平台，支持现金交易、以物易物、站内聊天、评价和举报。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Vue Router + Axios + STOMP |
| 后端 | Spring Boot 3.2 + Spring Security + JWT + JPA |
| 数据库 | MySQL 8.0 |
| 实时通信 | WebSocket (STOMP) |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+

### 首次启动

```bash
# 1. 创建配置文件
setup.bat          # Windows 双击
./setup.sh         # Unix / Git Bash

# 2. 初始化数据库
powershell -ExecutionPolicy Bypass -File setup.ps1   # Windows
mysql -u root -p < database/init.sql                 # Unix 手动

# 3. 启动
start.bat          # Windows 双击
./start.sh         # Unix / Git Bash
```

### 日常启动

双击 `start.bat`（或 `./start.sh`），浏览器访问 `http://localhost:5173`。

## 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 12345678 |
| 普通用户 | zhangsan | 12345678 |
| 普通用户 | lisi | 12345678 |
| 普通用户 | wangwu | 12345678 |
| 被禁用 | sunqi | — |

## 脚本说明

| 文件 | 用途 |
|------|------|
| `start.bat` / `start.sh` | 一键启动前后端 |
| `setup.bat` / `setup.sh` | 创建 `.env` 配置文件 |
| `setup.ps1` | 初始化数据库（含测试数据） |
| `.env.template` | 配置模板，复制为 `.env` 修改 |

## 项目文档

详见 `memory/` 目录：

- [产品需求文档](memory/01-产品需求文档-PRD.md)
- [软件需求规格说明书](memory/02-软件需求规格说明书-SRS.md)
- [页面与交互说明](memory/03-页面与交互说明.md)
- [数据模型设计](memory/05-数据模型设计.md)
- [接口设计文档](memory/06-接口设计文档.md)
- [技术架构设计](memory/07-技术架构设计说明书.md)
- [测试与验收方案](memory/08-测试与验收方案.md)
- [MVP 实施计划](memory/09-MVP实施计划.md)
