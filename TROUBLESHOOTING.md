# 故障排查清单

校园二手交易平台运维速查。按"先定位层级、再动手"的顺序排查。

## 0. 快速分诊（30 秒）

| 现象 | 可能层级 | 优先看 |
|---|---|---|
| 前端白屏 / 页面打不开 | Netlify 前端 | Netlify Deploy log |
| 页面打开但接口全报错（CORS/502/超时） | Railway 后端或网络 | Railway Deploy log + 健康检查 |
| 个别接口 500 | 后端代码或 DB | Railway 运行日志 + 该接口 |
| 接口 401/403 | 认证/JWT | token 是否过期 |
| 数据查不到/写入失败 | PostgreSQL | Railway PG 控制台 |

健康检查：`https://campus-trade-production-a0ef.up.railway.app/actuator/health`
- `UP` → 后端进程存活，问题在业务/DB/前端
- 不通 → 后端挂了或部署中

---

## 1. 后端部署失败（Railway）

**入口**：Railway 控制台 → backend 服务 → Deployments 标签 → 最新部署 → View Logs

**常见原因 & 处理**：

| 错误特征 | 原因 | 处理 |
|---|---|---|
| `Could not resolve placeholder 'DATABASE_URL'` | 环境变量丢失 | Railway → Variables → 确认 `DATABASE_URL` / `PG*` 存在，缺失则重填并 Redeploy |
| `Communications link failure` / `Connection refused` | PG 实例挂了或网络隔离 | PG 服务页看状态；如 Down 等待自动恢复或重启 |
| `BeanCreationException` / `LazyInitializationException` | 代码问题 | 看堆栈定位到具体类，修代码重提 |
| 构建成功但容器反复重启 | 健康检查不过 / OOM | 看启动日志末尾的异常；OOM 则升级内存或优化 |
| `Port already in use` | 端口配置错 | 确认应用监听 `$PORT` 环境变量（非硬编码 8080） |

**恢复手段**：
- 单个提交有问题 → 用回滚流程（推一个 tree 指向上一个稳定提交的 commit）
- 环境变量问题 → 修改变量后点 Redeploy

---

## 2. PostgreSQL 数据库故障

**入口**：Railway → postgres 服务 → Data 标签（SQL 控制台）

| 现象 | 排查 SQL |
|---|---|
| 连接超时 | `SELECT 1;` 看控制台是否响应 |
| 表不存在 | `\dt` 或 `SELECT tablename FROM pg_tables WHERE schemaname='public';` |
| 某表数据异常 | `SELECT * FROM products LIMIT 5;` 直接查 |
| 连接池耗尽 | 后端日志出现 `HikariPool-1 - Connection is not available` |

**处理**：
- 实例状态异常 → Railway PG 页 → Restart
- 数据误删 → 如有备份则恢复；无备份则用 SQL 控制台手动 INSERT 修复
- 中文乱码 → 确认列编码为 UTF8（Railway PG 默认 UTF8，乱码通常是客户端写入时编码不对）

---

## 3. 前端构建失败（Netlify）

**入口**：Netlify → campus-trade-app → Deploys → 最新部署 → Deploy log

| 错误特征 | 原因 | 处理 |
|---|---|---|
| `Could not resolve import` / `Cannot find name` | TypeScript 类型/导入错误 | 看报错文件行号，修代码重提 |
| `vite build` 退出码非 0 | 构建配置或依赖问题 | 本地 `npm run build` 复现（如可） |
| 部署成功但页面白屏 | 资源路径/路由 | 浏览器 F12 看 Console 和 Network 404 |
| 接口全 404 | `VITE_API_BASE_URL` 未配置 | Netlify → Site settings → Environment → 确认 `VITE_API_BASE_URL` = Railway 后端地址 |

**回滚**：Netlify Deploys 列表 → 选上一个成功部署 → `Publish deploy`

---

## 4. 接口 500 / 业务错误

**定位步骤**：
1. 用 PowerShell 直接调接口拿错误体：
   ```powershell
   try {
       Invoke-RestMethod -Uri "https://campus-trade-production-a0ef.up.railway.app/api/xxx" -TimeoutSec 15
   } catch {
       $r = $_.Exception.Response
       $body = (New-Object System.IO.StreamReader($r.GetResponseStream())).ReadToEnd()
       Write-Host $body
   }
   ```
2. 错误体只有 `{"status":500,"error":"Internal Server Error"}` → 去 Railway 运行日志找堆栈
3. 常见根因：
   - `LazyInitializationException` → JPA 懒加载关联未预取，加 `@EntityGraph` 或 `fetch`
   - `NullPointerException` → 空值未判空
   - `DataIntegrityViolationException` → 数据库约束（唯一键、非空）
   - `AccessDeniedException` → 权限/认证

---

## 5. CORS / 跨域问题

**现象**：前端 Console 报 `CORS policy: No 'Access-Control-Allow-Origin'`

**排查**：
1. 确认后端 `FRONTEND_URL` 环境变量 = 前端域名（`https://campus-trade-app.netlify.app`）
2. 后端 `WebConfig` 的 `CorsConfiguration` allowedOrigins 取的是 `FRONTEND_URL`
3. 改完变量后 Railway 自动 Redeploy

---

## 6. 认证 / 401 问题

| 现象 | 原因 | 处理 |
|---|---|---|
| 登录后所有接口 401 | token 未存或过期 | localStorage 看 `token`；重新登录 |
| token 有但仍 401 | JWT 密钥变了 | Railway `JWT_SECRET` 是否被改过 |
| 接口 403 | 非本人操作（如标记别人消息已读） | 业务逻辑，正常拦截 |

---

## 7. 回滚流程（已验证）

**回滚单个提交**：
1. 找到要回滚到的目标 commit 的 tree sha：
   ```
   gh api repos/Dhuaing/campus-trade/git/commits/<target_sha> --jq .tree.sha
   ```
2. 构造回滚 commit（parent=当前 HEAD，tree=目标 tree）：
   ```powershell
   $req = @{ message="revert: ..."; tree="<target_tree>"; parents=@("<current_head>") } | ConvertTo-Json
   # POST 到 /repos/Dhuaing/campus-trade/git/commits
   ```
3. 把新 commit sha PATCH 到 `refs/heads/main`
4. 等 Railway 自动部署（约 90s），验证健康检查

**注意**：回滚后功能处于"修复被撤销"状态，仅作为应急手段；确认根因后应重新修复并重新推送。

---

## 8. 关键链接

| 资源 | 地址 |
|---|---|
| 前端 | https://campus-trade-app.netlify.app |
| 后端健康检查 | https://campus-trade-production-a0ef.up.railway.app/actuator/health |
| 代码仓库 | https://github.com/Dhuaing/campus-trade |
| Railway 控制台 | https://railway.app |
| Netlify 控制台 | https://app.netlify.com |
