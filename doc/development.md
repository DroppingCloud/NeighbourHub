## 开发规范

## 1. 项目目录结构规范

### 1.1 前端目录结构

```text
frontend/
├── src/
│   ├── api/                 # 接口定义，按业务模块划分
│   ├── assets/              # 图片、图标、字体等静态资源
│   ├── components/          # 通用组件和业务复用组件
│   ├── layout/              # 页面布局和菜单框架
│   ├── pages/               # 页面级组件
│   ├── router/              # 路由配置和权限守卫
│   ├── stores/              # Pinia 全局状态
│   ├── styles/              # 全局样式、变量、重置样式
│   ├── utils/               # 请求封装、格式化、校验等工具函数
│   ├── App.vue
│   └── main.ts
├── .env.development
├── .env.production
└── package.json
```

### 1.2 后端目录结构

```text
backend/
├── src/main/java/com/project/
│   ├── common/              # 统一返回体、异常、常量、工具类
│   ├── config/              # Spring Security、Swagger、跨域、Redis 等配置
│   ├── controller/          # RESTful API 控制层
│   ├── dto/                 # 前端请求 DTO
│   ├── entity/              # 数据库实体 Entity
│   ├── mapper/              # MyBatis-Plus Mapper
│   ├── service/             # 业务接口
│   ├── service/impl/        # 业务实现
│   ├── vo/                  # 返回给前端的 VO
│   └── ProjectApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── mapper/
└── pom.xml
```

### 3.3 文档目录结构

```text
doc/
├── README.md                 # 文档总览，说明各文档用途和维护方式
├── backend/
│   ├── api.md                # 后端接口文档
│   ├── permission.md         # 角色权限与接口访问规则
│   └── error-code.md         # 统一错误码说明
├── database/
│   ├── schema.md             # 数据库表结构说明
│   ├── init.sql              # 初始化建表脚本
│   └── migration.md          # 数据库变更记录
├── frontend/
│   ├── pages.md              # 页面说明与路由说明
│   └── components.md         # 组件说明
│   
└── test/
    ├── test-case.md          # 测试用例
    └── bug-record.md         # 问题记录与修复说明
```

## 2. Git 协作规范

### 2.1 分支规范

```text
main            # 稳定版本
dev             # 集成开发分支
feature/xxx     # 新功能分支
fix/xxx         # 缺陷修复分支
docs/xxx        # 文档修改分支
```

### 2.2 提交前检查

每次提交代码前务必完成以下检查。

#### 前端检查：

```bash
npm install
npm run dev
npm run build
```

检查内容：

1. 页面是否能正常启动。
2. 路由跳转是否正常。
3. 接口请求路径是否正确。
4. 表单校验、加载状态、错误提示是否完整。
5. 控制台是否存在明显报错。

#### 后端检查：

```bash
mvn clean package
mvn spring-boot:run
```

检查内容：

1. 项目是否能正常启动。
2. 接口是否能正常访问。
3. 数据库连接是否正常。
4. Redis、JWT、权限校验是否正常。
5. 新增接口是否已补充文档。

#### 文档检查：

1. 接口变更是否更新 `doc/backend/api.md`。
2. 数据库变更是否更新 `doc/database/schema.md`。
3. 页面或组件变更是否更新 `doc/frontend/`。

## 3. 前端开发规范

### 3.1 接口组织

接口文件统一放在 `src/api/` 下，按业务模块划分，例如：

```text
src/api/auth.ts
src/api/user.ts
src/api/guide.ts
src/api/serviceItem.ts
src/api/application.ts
src/api/workOrder.ts
src/api/booking.ts
src/api/notice.ts
src/api/statistics.ts
```

要求：

1. 不允许在页面中直接使用未封装的 `axios` 请求。
2. 不允许在多个文件中重复定义同一接口。
3. 接口方法命名应体现业务含义，例如 `login`、`getUserInfo`、`recommendGuide`、`submitApplication`、`getWorkOrderList`。
4. 请求和响应类型应使用 TypeScript `interface` 或 `type` 定义。
5. 接口定义应尽量和后端接口文档保持字段一致。

示例：

```ts
import request from '@/utils/request'

export interface GuideRequest {
  residentType: string
  age?: number
  needType: string
}

export function recommendGuide(data: GuideRequest) {
  return request.post('/api/guide/recommend', data)
}
```

### 3.2 页面组织

页面文件放在 `src/pages/`，按业务场景命名：

```text
pages/LoginPage.vue
pages/GuidePage.vue
pages/ApplicationSubmitPage.vue
pages/ProgressPage.vue
pages/WorkOrderManagePage.vue
pages/BookingPage.vue
pages/AdminServiceConfigPage.vue
pages/StatisticsDashboardPage.vue
```

要求：

1. 页面文件采用大驼峰命名，名称应体现业务含义。
2. 页面只负责页面结构组织、状态调度、调用组件和调用接口。
3. 复杂展示逻辑、表单块、图表块应下沉到 `components/`。
4. 路由路径与页面含义保持一致，不使用随意缩写。
5. 新增页面后，应同步维护 `doc/frontend/pages.md`。

### 3.3 组件开发

通用组件和可复用业务组件放在 `src/components/`，例如：

```text
components/GuideStepForm.vue
components/ServiceCard.vue
components/MaterialUploadPanel.vue
components/TaskStatusTag.vue
components/NoticeEditor.vue
components/StatisticsChart.vue
```

要求：

1. 可复用的界面单元必须组件化。
2. 组件通过 props 接收输入，通过 emit 输出事件，不直接修改父组件状态。
3. 组件应提供必要的空状态、加载状态、错误状态。
4. 新增复杂组件时，应在组件说明文档中注明用途、props、emits、基本效果和使用位置。
5. 组件命名应体现业务含义，避免 `Box.vue`、`Item.vue`、`Common.vue` 等过于模糊的名称。
6. 组件内部不应直接写死与后端接口强绑定的逻辑，除非该组件本身就是业务组件。

### 3.4 状态管理

Pinia 用于保存全局状态，建议按业务拆分：

```text
stores/userStore.ts
stores/permissionStore.ts
stores/dictStore.ts
stores/noticeStore.ts
```

适合放入全局状态的数据：

- 当前登录用户信息；
- Token；
- 角色和权限菜单；
- 全局字典数据；
- 未读消息数量；
- 系统主题或适老化显示设置。

页面私有状态不得滥用全局 store，例如当前表单输入、弹窗开关、局部分页参数应保留在页面或组件内部。

### 3.5 路由与权限

1. 登录页、公开导办说明页等可设为公开路由。
2. 事项申请、材料上传、进度查询、后台管理等页面必须设置登录校验。
3. 后台管理、用户管理、事项配置、统计看板等页面必须设置角色权限。
4. 路由守卫应在未登录时跳转登录页，在权限不足时跳转无权限页面或展示提示。
5. 路由元信息中应明确页面标题、是否需要登录、允许访问的角色。
6. 新增路由后，应同步更新 `doc/frontend/pages.md`。

## 4. 后端开发规范

### 4.1 Controller 规范

Controller 层统一提供 RESTful API，路径统一以 `/api` 开头。

示例：

```java
@RestController
@RequestMapping("/api/guide")
public class GuideController {

    private final GuideService guideService;

    @PostMapping("/recommend")
    public Result<GuideResultVO> recommend(@RequestBody GuideRequestDTO dto) {
        return Result.success(guideService.recommend(dto));
    }
}
```

要求：

1. Controller 只负责接收参数、参数校验、调用 Service、返回统一结果。
2. 不允许在 Controller 中编写复杂业务流程、数据库访问或外部服务调用细节。
3. 接口应标明请求方式，避免所有操作都使用 POST。
4. 接口路径使用名词或业务动作组合，保持清晰稳定。
5. 新增接口后，必须同步维护 `doc/backend/api.md`。
6. 接口涉及权限时，必须同步维护 `doc/backend/permission.md`。

### 4.2 Service 规范

1. Service 层负责业务规则、流程编排、权限辅助判断和事务控制。
2. 申请提交、工单审核、材料预审、服务预约等涉及多表写入的操作必须考虑事务。
3. 状态流转必须集中处理，不允许多个地方随意修改工单或预约状态。
4. 外部 AI、OCR、通知服务应通过适配类或独立服务封装，便于替换和降级。
5. Service 方法命名应体现业务含义，避免过度泛化。
6. 复杂业务流程应适当拆分私有方法或独立服务，避免单个方法过长。

### 4.3 Mapper 与数据库访问规范

1. 数据库访问统一通过 Mapper 层完成。
2. 简单 CRUD 可使用 MyBatis-Plus。
3. 复杂查询应保持 SQL 清晰，并注意索引使用。
4. 分页查询必须限制 pageSize，避免一次性返回大量数据。
5. 删除业务数据优先采用逻辑删除或状态字段，除非确认为临时数据。
6. 不允许在 Mapper 层处理复杂业务规则。
7. 涉及复杂统计查询时，应在文档或注释中说明查询逻辑和性能注意事项。

### 4.4 DTO、VO、Entity 规范

为避免层次混乱，统一区分：

- Entity：数据库实体，与表结构对应；
- DTO：前端传入后端的数据对象；
- VO：后端返回前端的视图对象；
- QueryDTO：列表查询、筛选、分页参数对象。

要求：

1. 不直接把 Entity 暴露给前端。
2. DTO 应进行参数校验，例如非空、长度、格式、枚举值。
3. VO 只返回前端需要展示的数据。
4. 密码哈希、密钥、内部配置、权限内部标识等敏感字段不得返回给前端。
5. DTO、VO 字段应与接口文档保持一致。
6. 列表查询接口建议统一使用 QueryDTO 承载分页和筛选条件。

### 4.5 统一返回体规范

所有接口统一返回标准 JSON：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

要求：

1. 不允许不同接口返回风格混乱。
2. 错误信息应可读，不直接暴露异常栈。
3. 统一使用全局异常处理机制。
4. 登录失效、权限不足、参数错误、业务失败、系统异常应使用明确错误码。
5. 错误码应统一维护在 `doc/backend/error-code.md`。

### 4.6 权限规范

系统角色统一命名：

```text
ROLE_RESIDENT   # 居民
ROLE_FAMILY     # 家属/代理人
ROLE_STAFF      # 社区工作人员
ROLE_ADMIN      # 系统管理员
```

权限要求：

1. 新增接口时必须明确是否需要登录、允许哪些角色访问、是否需要数据权限校验。
2. 居民只能访问本人事项、材料、预约和通知。
3. 家属/代理人只能访问授权对象的数据。
4. 社区工作人员只能处理职责范围内的工单和服务任务。
5. 管理员可维护配置和权限，但关键配置变更必须记录审计日志。
6. 权限规则变化必须同步更新 `doc/backend/permission.md`。

---

## 5. 接口文档规范

接口文档统一维护在 `doc/backend/api.md`，每个接口至少包含以下内容：

```markdown
## 接口名称

智能导办推荐接口

## 请求路径

POST /api/guide/recommend

## 功能说明

根据用户提交的基本信息和自然语言需求，返回推荐事项、材料清单和办理步骤。

## 请求参数

无

## 请求体示例

```json
{
  "residentType": "本地户籍",
  "age": 67,
  "needType": "养老服务"
}
```

## 返回体示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [],
    "materials": [],
    "steps": []
  }
}
```

## 权限要求

居民、家属/代理人

## 相关页面

- `GuidePage.vue`
- `ApplicationSubmitPage.vue`

## 相关数据表

- `service_item`
- `service_material`
- `application_form`

## 异常情况

| 错误码 | 含义 | 处理方式 |
| --- | --- | --- |
| 400 | 参数错误 | 返回具体字段错误信息 |
| 401 | 未登录 | 跳转登录页 |
| 403 | 无权限 | 提示无权限访问 |
| 500 | AI 服务异常 | 降级为规则匹配结果 |

## 备注

AI 服务不可用时，应降级为规则匹配结果。
```

接口文档维护要求：

1. 新增接口前，应先补充接口草案。
2. 接口字段变更后，必须同步更新请求体、返回体和字段说明。
3. 接口权限变化后，必须同步更新权限要求。
4. 接口被前端页面调用时，应补充“相关页面”。
5. 接口涉及数据库读写时，应补充“相关数据表”。
6. 接口存在降级、异常、边界情况时，应补充异常说明。
7. 不允许接口实现和文档长期不一致。
8. 接口废弃时，应标注废弃原因、替代接口和计划删除时间。
