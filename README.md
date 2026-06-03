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

**更新或重建数据库**

日常运行不需要重复导入 `init.sql`。如果数据库表结构更新、旧数据出现乱码，或需要恢复到演示初始数据，可选择以下方式。

保留 Docker 数据卷，只执行最新初始化脚本：

```bash
docker cp doc/database/init.sql community_mysql:/tmp/init.sql
docker exec community_mysql sh -c "mysql --default-character-set=utf8mb4 -uroot -p123456 < /tmp/init.sql"
```

彻底重建数据库（会清空本地 MySQL 数据）：

```bash
docker compose -p community-service down -v
docker compose -p community-service up -d
```

MySQL 容器已挂载 `docker/mysql/conf.d/charset.cnf`，新建容器后服务端和容器内 `mysql` 客户端默认使用 `utf8mb4`。如果是旧容器，需要执行一次 `docker compose -p community-service up -d --force-recreate mysql` 让配置生效。

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

### 阿里百炼 OCR 配置

材料 OCR/AI 预审支持阿里云百炼 DashScope。复制 `.env` 后填写：

```properties
DASHSCOPE_API_KEY=你的百炼API Key
DASHSCOPE_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
DASHSCOPE_OCR_MODEL=qwen-vl-ocr-latest
DASHSCOPE_OCR_ENABLED=true
```

当前会对 JPG/JPEG/PNG 图片材料调用百炼 OCR；PDF、DOC、DOCX 暂不做文件转图片，使用本地规则预审兜底。
