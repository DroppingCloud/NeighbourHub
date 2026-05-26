# NeighbourHub

## 1. 环境要求

| 工具 | 作用 |
|---|---|
| Docker | 启动 MySQL、Redis |
| Java 17+ | 运行 Spring Boot 后端 |
| Maven | 启动和构建后端 |
| Node.js 24+ | 运行 Vue 前端 |
| npm | 安装前端依赖 |

## 2. 环境配置

### 2.1 后端环境变量

在项目根目录复制环境变量模板：

```bash
cp .env.example .env
```

`.env` 保存本地开发配置：

```bash
MYSQL_ROOT_PASSWORD=123456          # MySQL root 用户密码
MYSQL_DATABASE=community_service    # 业务数据库名
MYSQL_USER=appuser                  # 应用用户名
MYSQL_PASSWORD=app_password_123     # 应用用户密码

MYSQL_HOST=localhost                # MySQL 服务地址
MYSQL_PORT=3306                     # MySQL 服务端口

REDIS_HOST=localhost                # Redis 服务地址    
REDIS_PORT=6379                     # Redis 服务端口
REDIS_PASSWORD=                     # Redis 密码

COMPOSE_PROJECT_NAME=community-service  # 基础服务容器名

JWT_SECRET=change_this_to_a_random_secret_with_at_least_32_chars        # JWT 签名密钥

AI_API_KEY=                         # AI 服务密钥
AI_API_URL=                         # AI 服务接口地址

NGINX_PORT=80                       # Nginx 端口
```

### 2.2 前端环境变量

`frontend/.env.development` 保存 api 转发地址：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 3. 项目启动

### 3.1 启动基础服务

在项目根目录执行：

```bash
docker compose -p community-service up -d
```

查看容器状态：

```bash
docker compose -p community-service ps
```

查看日志：

```bash
docker compose -p community-service logs -f mysql
docker compose -p community-service logs -f redis
```
停止所有服务
```bash
docker compose down
```

停止并删除数据卷
```bash
docker compose down -v
```

### 3.2 启动后端

在项目根目录执行：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

Swagger 文档地址：

```text
http://localhost:8080/swagger-ui.html
```

删除后端进程：

```text
lsof -ti:8080 | xargs kill -9
```

### 3.3 启动前端

打开新的终端，在项目根目录执行：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

删除前端进程：

```text
lsof -ti:5173 | xargs kill -9
```
