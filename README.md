# ActivitySystem · 公益志愿活动管理平台

**一个覆盖「活动发布 → 报名审核 → 现场签到 → 时长积分 → 评价互动 → 数据看板」全链路的志愿活动管理系统。**

后端 Spring Boot 3 · 前端 Vue 3 + TypeScript · 前后端分离 · 支持高并发报名与实时数据大屏

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## 简介

ActivitySystem 面向高校社团、公益组织与志愿服务机构，提供志愿活动从创建到结项的一站式管理能力。系统以「组织者发布活动、志愿者报名参与、现场扫码签到、系统结算时长」为主线，内置权限隔离、异步削峰、全文检索与实时统计能力，替代传统的群接龙报名 + 纸质签到表 + 人工统计 Excel 的低效流程。

## 功能特性

| 模块 | 能力 |
|---|---|
| 活动管理 | 活动 CRUD、六态状态机（草稿 / 待发布 / 已发布 / 进行中 / 已结束 / 已取消）、分类与标签、条件分页查询 |
| 报名管理 | 异步报名提交（RabbitMQ 削峰）、管理员审核（通过 / 驳回）、名额控制、报名数据导出 Excel |
| 签到管理 | 6 位限时签到码生成与校验、管理员手动代签、未签到名单拉取、签到数据导出 |
| 用户与权限 | JWT 无状态认证、三层 RBAC（超级管理员 / 管理员 / 志愿者）、志愿者个人中心（时长、积分、参与记录） |
| 电子证书 | 依据累计志愿时长自动生成志愿者电子证书 PDF |
| 评价互动 | 五星评分 + 文字评价、点赞、基于字典库的敏感词过滤、评价情感倾向分析 |
| 搜索推荐 | Elasticsearch 全文检索与相关度排序、基于报名数 / 浏览量的热门活动推荐 |
| 数据看板 | 运营总量统计、近 7 日报名趋势折线图、活动分类占比饼图，签到数据 WebSocket 实时推送 |
| 地图服务 | 附近活动搜索（经纬度 + 半径）、地址地理编码、地理围栏校验 |
| 系统管理 | 用户 / 角色 / 菜单权限、文件上传、系统参数配置、操作日志 |

## 技术栈

**后端**

| 类别 | 选型 |
|---|---|
| 语言 / 运行时 | Java 17 |
| 应用框架 | Spring Boot 3.1.5、Spring MVC、Spring AOP、Bean Validation |
| 安全认证 | Spring Security + JWT（jjwt 0.11.5），无状态鉴权 |
| 持久层 | MyBatis-Plus 3.5.3（逻辑删除、自动填充）、MySQL 8.0 |
| 缓存 | Redis（Token、签到码、热点数据） |
| 消息队列 | RabbitMQ（报名削峰、系统通知），开启 publisher-confirm / return 与消费重试 |
| 搜索引擎 | Elasticsearch 8.x（活动全文检索与推荐排序） |
| 实时通信 | WebSocket（签到事件推送） |
| 文档处理 | Apache POI（Excel 导出）、iText（PDF 证书生成） |
| 接口文档 | SpringDoc OpenAPI 3（Swagger UI） |

**前端**

Vue 3.5 · TypeScript 5.6 · Vite 5 · Element Plus · Pinia · Vue Router · ECharts 5 · Tailwind CSS · Axios

## 项目结构

```
ActivitySystem
├── src/main/java/com/charity
│   ├── common/         统一响应体、全局异常处理、通用工具
│   ├── config/         安全、Redis、RabbitMQ、Elasticsearch、WebSocket、MyBatis-Plus 配置
│   └── modules/        业务按领域垂直分包
│       ├── activity/        活动与分类
│       ├── registration/    报名与审核
│       ├── checkin/         签到码与签到记录
│       ├── review/          评价与互动
│       ├── recommendation/  搜索与推荐
│       ├── statistics/      数据统计与看板
│       ├── map/             附近活动与地理编码
│       ├── ai/              敏感词过滤与情感分析
│       ├── msg/             站内消息
│       └── sys/             用户、角色、权限、文件、系统配置
├── src/main/resources
│   ├── application.yml        公共配置
│   ├── application-dev.yml    开发环境配置（敏感项走环境变量）
│   └── mapper/                MyBatis XML
├── frontend/                  Vue 3 + TypeScript 前端工程
└── db/                        schema.sql 建表 · seed_data.sql 初始化数据
```

## 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| RabbitMQ | 3.9+ |
| Elasticsearch | 8.x |
| Node.js | 18+ |

### 1. 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS activity_system DEFAULT CHARSET utf8mb4;"
mysql -u root -p activity_system < db/schema.sql
mysql -u root -p activity_system < db/seed_data.sql
```

### 2. 配置环境变量

为避免凭据入库，`application-dev.yml` 中的敏感项均通过环境变量注入：

```bash
# macOS / Linux
export DB_PASSWORD="你的数据库密码"
export JWT_SECRET="至少 32 字节的随机密钥"

# Windows PowerShell
$env:DB_PASSWORD="你的数据库密码"
$env:JWT_SECRET="至少 32 字节的随机密钥"
```

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/activity_system` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 / 密码 | `root` / 空 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址 | `localhost` / `6379` |
| `ES_URIS` | Elasticsearch 地址 | `http://localhost:9200` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | RabbitMQ 地址 | `localhost` / `5672` |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 账号 | `guest` / `guest` |
| `JWT_SECRET` | JWT 签名密钥（≥32 字节） | 占位值，**必须覆盖** |

### 3. 启动后端

```bash
mvn spring-boot:run
```

服务端口 `8080`，接口文档：<http://localhost:8080/swagger-ui.html>，OpenAPI JSON：`/api-docs`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 设计要点

1. **三层 RBAC 物理隔离** —— 超级管理员只负责用户与角色，管理员只负责业务运营，权限校验通过自定义注解 + AOP 在接口层完成，避免单一超管账号权限膨胀。
2. **报名链路削峰** —— 报名请求先入 RabbitMQ，消费端做幂等校验后落库，配合 Redis 预扣名额，降低高峰期数据库锁竞争与超卖风险。
3. **签到码缓存化** —— 6 位限时签到码存于 Redis 并设置 TTL，校验与失效由缓存自动完成，避免高频校验打穿数据库。
4. **检索与推荐分离** —— Elasticsearch 承担关键字全文检索与相关度排序，热门推荐走 MySQL 聚合 + 缓存，两条链路互不阻塞。
5. **实时看板** —— WebSocket 推送签到事件，前端 ECharts 增量渲染，替代定时轮询。
6. **文件导出流式化** —— Excel 报表与 PDF 证书在服务端流式生成、前端 Blob 下载，避免大文件占用服务端内存。

## Roadmap

- [ ] 提供 Docker Compose 一键启动（MySQL / Redis / RabbitMQ / ES / 应用）
- [ ] 补充核心链路单元测试与接口集成测试
- [ ] 报名并发场景压测与容量评估报告
- [ ] 志愿者端移动 H5 适配
- [ ] 证书模板可配置化

## 贡献指南

欢迎提交 Issue 与 Pull Request：

1. Fork 本仓库并基于 `main` 创建特性分支：`git checkout -b feature/your-feature`
2. 遵循现有代码风格（后端按 `modules` 垂直分包、统一使用 `Result` 包装响应）
3. 提交信息使用约定式格式：`feat:` / `fix:` / `docs:` / `refactor:` / `test:`
4. 提交 PR 并描述改动背景与验证方式

## License

本项目基于 [MIT License](LICENSE) 开源。
