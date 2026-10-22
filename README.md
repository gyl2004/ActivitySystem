# 公益志愿活动管理系统 ActivitySystem

> 本科毕业设计 · 前后端分离的公益志愿活动全流程管理平台
> 后端 Spring Boot 3 · 前端 Vue 3 + TypeScript

## 项目简介

面向高校与公益组织的志愿活动管理平台，覆盖「活动发布 → 报名审核 → 现场签到 → 时长积分 → 评价互动 → 数据看板」完整业务闭环，解决传统志愿活动中报名靠群接龙、签到靠纸质表、时长统计靠人工 Excel 的问题。

## 核心功能

| 模块 | 功能 |
|---|---|
| 活动管理 | 活动 CRUD、六态状态机（草稿/待发布/已发布/进行中/已结束/已取消）、分类管理、分页查询 |
| 报名管理 | 报名提交（RabbitMQ 异步削峰）、管理员审核（通过/驳回）、报名数据导出 Excel |
| 签到管理 | 6 位限时签到码生成与校验、管理员手动代签、未签到名单拉取、签到数据导出 |
| 用户与权限 | JWT 无状态登录、三层 RBAC（超级管理员/管理员/志愿者）、志愿者个人中心（时长、积分、参与记录） |
| 电子证书 | 依据志愿时长自动生成志愿者电子证书 PDF（iText） |
| 评价互动 | 五星评分 + 文字评价、点赞、基于字典库的敏感词过滤、情感倾向分析 |
| 搜索推荐 | Elasticsearch 全文检索与相关度排序、热门活动（报名数/浏览量）推荐 |
| 数据看板 | 总量统计、近 7 日报名趋势折线图、活动分类占比饼图（ECharts），签到数据 WebSocket 实时推送 |
| 地图服务 | 附近活动搜索（经纬度 + 半径）、地址地理编码、地理围栏校验 |

## 技术栈

**后端**：Java 17 · Spring Boot 3.1.5 · Spring Security + JWT · MyBatis-Plus 3.5.3 · MySQL 8 · Redis · RabbitMQ · Elasticsearch 8 · WebSocket · AOP · SpringDoc OpenAPI
**前端**：Vue 3 · TypeScript · Vite · Element Plus · Pinia · Vue Router · ECharts · Tailwind CSS · Axios
**文档处理**：Apache POI（Excel 导出）· iText（PDF 证书）

## 项目结构

```
src/main/java/com/charity
├── common/          统一返回体 Result、全局异常处理
├── config/          安全、Redis、RabbitMQ、ES、WebSocket、MyBatis-Plus 配置
└── modules/         按业务垂直分包
    ├── activity/        活动与分类
    ├── registration/    报名
    ├── checkin/         签到
    ├── review/          评价互动
    ├── recommendation/  搜索与推荐
    ├── statistics/      数据统计
    ├── map/             附近活动与地理编码
    ├── ai/              敏感词过滤与情感分析
    ├── msg/             系统消息
    └── sys/             用户、角色、权限、文件、配置
frontend/             Vue 3 + TypeScript 前端工程
db/                   schema.sql（建表）· seed_data.sql（初始化数据）
```

## 设计要点

1. **三层 RBAC 物理隔离**：超级管理员仅管用户与角色，管理员仅管业务，通过自定义注解 + AOP 在接口层校验权限，避免"一个 admin 走天下"。
2. **报名削峰**：报名请求先入 RabbitMQ 队列，消费端幂等校验后落库，配合 Redis 预扣名额，降低高并发下的超卖与锁竞争风险。
3. **签到码设计**：6 位限时签到码存于 Redis（带 TTL），校验与失效由缓存自动完成，避免数据库写放大。
4. **检索与推荐分离**：ES 负责关键字全文检索与相关度排序，热门推荐走 MySQL 聚合 + 缓存，两者互不阻塞。
5. **实时看板**：WebSocket 推送签到事件，前端 ECharts 增量刷新，替代轮询。
6. **文件导出**：Excel 报表与 PDF 证书在服务端流式生成，前端以 Blob 下载，避免大文件占用内存。

## 快速开始

### 环境要求

JDK 17 · Maven 3.8+ · MySQL 8.0 · Redis 6+ · RabbitMQ 3.9+ · Elasticsearch 8.x · Node.js 18+

### 后端

```bash
# 1. 初始化数据库（库名 activity_system）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS activity_system DEFAULT CHARSET utf8mb4;"
mysql -u root -p activity_system < db/schema.sql
mysql -u root -p activity_system < db/seed_data.sql

# 2. 配置敏感项：通过环境变量注入（不要提交明文密码）
#    Windows PowerShell
#      $env:DB_PASSWORD="你的数据库密码"
#      $env:JWT_SECRET="至少32字节的随机密钥"
#    macOS / Linux
#      export DB_PASSWORD="你的数据库密码"
#      export JWT_SECRET="至少32字节的随机密钥"
#    也可以在 IDEA 的 Run Configuration → Environment variables 中填写

# 3. 启动
mvn spring-boot:run
```

服务默认端口 `8080`，接口文档：`http://localhost:8080/swagger-ui.html`

支持的环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`ES_URIS`、`RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`、`JWT_SECRET`。

### 前端

```bash
cd frontend
npm install
npm run dev
```

## 说明

本项目为个人本科毕业设计，代码用于学习与技术交流。所有连接信息与密钥均通过环境变量注入，请勿将真实凭据提交到仓库。
