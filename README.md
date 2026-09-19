# Campus Trade — 校园二手交易平台

> 求职作品集项目 · 前后端分离 · 全栈云端部署

## 在线演示

| 端点 | 地址 |
|---|---|
| 前端站点 | https://campus-trade-app.netlify.app |
| REST API | https://campus-trade-production-a0ef.up.railway.app/api/products |
| 健康检查 | https://campus-trade-production-a0ef.up.railway.app/actuator/health |

## 功能特性

- 用户体系：注册、登录（JWT，BCrypt 密码哈希）、个人中心、退出登录
- 商品列表：按分类浏览（教材书籍 / 数码电子 / 生活用品 / 运动户外），分类筛选即点即切
- 发布商品：需登录，表单校验（必填/数值/长度）→ `POST /api/products` → 自动关联发布者 → 创建成功跳转详情页
- 商品详情：现价、原价对比、卖家昵称展示
- 站内消息：详情页"联系卖家"弹窗发送消息，消息中心收件箱（未读标记、关联商品）
- 种子数据：数据库表为空时自动写入 6 条示例商品（`DataInitializer`）

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Vite + Vue Router |
| 后端 | Spring Boot 3.2 (Java 17)、Spring Security、Spring Data JPA、Validation、Actuator、JWT (jjwt) |
| 数据库 | PostgreSQL |
| 部署 | Docker 多阶段构建 · Railway（后端 + 数据库）· Netlify（前端，CI/CD 随 push 自动构建） |

## 架构

```mermaid
flowchart LR
    A[浏览器] -->|HTTPS| B[Netlify\nVue 3 SPA]
    B -->|REST /api/products| C[Railway\nSpring Boot 3\nDocker 容器]
    C -->|JDBC| D[(Railway\nPostgreSQL)]
```

## 目录结构

```
├── backend/                # Spring Boot 后端（Railway 部署根目录）
│   ├── Dockerfile          # maven 构建 → JRE 17 运行 多阶段镜像
│   ├── pom.xml
│   └── src/main/java/com/campus/trade/
│       ├── controller/     # ProductController / AuthController / MessageController
│       ├── entity/         # Product / User / Message 实体
│       ├── repository/     # Spring Data JPA（@EntityGraph + JOIN FETCH 解决 LAZY 序列化）
│       ├── security/       # JwtUtil / JwtAuthFilter / SecurityConfig
│       ├── config/         # CORS 配置、种子数据初始化
│       └── CampusTradeApplication.java
└── frontend/               # Vue 3 前端（Netlify 部署根目录）
    ├── netlify.toml        # 构建命令 + SPA 路由回退
    └── src/                # Home / ProductDetail 页面
```

## API 一览

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/api/auth/register` | 公开 | 注册（用户名/密码/昵称/学号） |
| POST | `/api/auth/login` | 公开 | 登录，返回 JWT token |
| GET | `/api/auth/me` | 需登录 | 当前用户信息 |
| GET | `/api/products` | 公开 | 商品列表 |
| GET | `/api/products/{id}` | 公开 | 商品详情（含卖家昵称） |
| POST | `/api/products` | 需登录 | 发布商品（Bean Validation 校验，关联发布者） |
| GET | `/api/messages` | 需登录 | 收件箱（按时间倒序，含未读数） |
| POST | `/api/messages` | 需登录 | 发送站内消息 |
| POST | `/api/messages/{id}/read` | 需登录 | 标记单条消息已读（仅接收者） |
| GET | `/actuator/health` | 公开 | 健康检查（Railway 部署探针） |

## 本地运行

后端（需本地 PostgreSQL 或使用环境变量指向远程库）：

```bash
cd backend
DATABASE_URL=jdbc:postgresql://localhost:5432/campus \
DATABASE_USER=postgres DATABASE_PASSWORD=postgres \
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
```

## 关键配置说明

| 变量 | 作用 | 备注 |
|---|---|---|
| `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` | 后端数据源 | Railway 中引用 Postgres 服务变量 |
| `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` / `PGPASSWORD` | Railway Postgres 注入 | 通过 `${{Postgres.*}}` 引用 |
| `VITE_API_BASE_URL` | 前端构建期注入的 API 地址 | Netlify 环境变量 |
| `FRONTEND_URL` | 后端 CORS 允许来源 | 与前端域名保持一致 |
| `PORT` | 容器监听端口 | Railway 自动注入，Spring 读取 `${PORT:8080}` |
| `JWT_SECRET` | JWT 签名密钥 | 生产环境务必覆盖默认值 |
| `JWT_EXPIRATION_HOURS` | JWT 有效期（小时） | 默认 168（7 天） |

## 部署要点

- **后端（Railway）**：Root Directory 设为 `backend`，Railpack 自动检测 Dockerfile 多阶段构建；服务变量通过 `${{Postgres.PGHOST}}` 等引用数据库实例，实现同项目内网互通。
- **前端（Netlify）**：Root Directory 设为 `frontend`，构建命令 `npm run build`，发布目录 `dist`；`netlify.toml` 配置 SPA 回退，保证 `/product/:id` 直链可访问。
- **CORS**：后端从 `FRONTEND_URL` 读取允许的来源，部署前端后回填即可完成双向打通。
