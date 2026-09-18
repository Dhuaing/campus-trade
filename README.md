# 校园二手交易平台（Campus Trade）

> 面向高校学生的二手物品交易平台 · 作品集项目
> 技术栈：Vue3 + TypeScript + Vite ｜ Spring Boot + PostgreSQL

## ✨ 项目简介

为高校学生提供二手物品发布、浏览、下单与线下交易的一站式平台，聚焦校园场景（学号认证、校内面交），降低信息差与交易成本。

## 🧱 技术架构

| 层 | 技术选型 |
|----|---------|
| 前端 | Vue 3 + TypeScript + Vite + Vue Router |
| 后端 | Spring Boot 3 + Spring Web + Spring Data JPA |
| 数据库 | PostgreSQL |
| 实时通信 | WebSocket（IM 聊天，规划中） |
| 部署 | 前端 Vercel · 后端 Railway · 数据库 Railway PostgreSQL |

## 📂 目录结构

```
campus-trade/
├── frontend/        # Vue3 前端
│   ├── src/
│   ├── vercel.json
│   └── package.json
├── backend/         # Spring Boot 后端
│   ├── src/main/java/com/campus/trade/
│   ├── Dockerfile
│   └── pom.xml
└── README.md
```

## 🚀 本地开发

### 环境要求
- Node.js ≥ 18
- JDK 17+
- Maven 3.8+
- PostgreSQL 14+

### 启动后端
```bash
cd backend
mvn spring-boot:run
# 默认 http://localhost:8080
```

### 启动前端
```bash
cd frontend
npm install
npm run dev
# 默认 http://localhost:5173
```

## 🔌 接口示例

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/products` | 在售商品列表 |
| GET | `/api/products/{id}` | 商品详情 |
| GET | `/actuator/health` | 健康检查 |

## ☁️ 线上部署

- 前端：Vercel（关联 GitHub，push 自动部署）
- 后端 + 数据库：Railway（Dockerfile 构建，端口读 `$PORT`）

## 📌 当前进度

- [x] 项目骨架搭建
- [x] 商品列表 / 详情查询闭环
- [ ] 用户登录 / 学号认证
- [ ] 发布商品
- [ ] 下单流程
- [ ] IM 实时聊天

---

> 本项目为个人作品集，仅用于技术演示，不面向真实市场运营。
