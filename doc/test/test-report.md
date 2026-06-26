# 测试报告

## 1. 测试环境

- 后端：Java 17、Spring Boot 3.2.5、JUnit 5、Mockito、Spring MockMvc。
- 数据库与缓存：MySQL 8、Redis 7。当前自动化测试不依赖真实数据库和 Redis，核心依赖通过 Mock 隔离。
- 前端：Vue 3、TypeScript、Vite、Element Plus。前端页面交互以浏览器联调和人工功能验证为主。

## 2. 测试方法

本项目按课程要求补充三类测试代码，测试代码位于 `backend/src/test/java`。

| 测试类型 | 测试文件 | 测试目标 |
| --- | --- | --- |
| 单元测试 | `WorkOrderStatusTest` | 验证工单状态枚举解析、合法状态流转和非法流转拦截。 |
| 单元测试 | `AuthServiceImplTest` | 验证系统内置管理员登录成功、密码错误失败。 |
| 单元测试 | `ProxyPermissionServiceImplTest` | 验证本人办理、家属代办授权、授权范围不匹配和无授权拦截。 |
| 单元测试 | `MaterialOcrAiPrecheckServiceTest` | 验证材料 OCR/AI 预审在文件缺失时返回失败结果，避免误通过。 |
| 接口测试 | `ServiceItemControllerApiTest` | 使用 MockMvc 验证事项接口路径、参数和统一响应结构。 |
| 接口测试 | `WorkOrderControllerApiTest` | 使用 MockMvc 验证工单详情接口的 REST 返回结构。 |
| 核心功能测试 | `ApplicationServiceCoreFlowTest` | 验证事项申请提交、工单创建、派单调用，以及线下事项禁止在线申请。 |
| 核心功能测试 | `BookingServiceCoreFlowTest` | 验证服务预约类型校验、预约工作人员接单、重复接单拦截、事项工作人员禁止接单。 |
| 核心功能测试 | `WorkOrderServiceCoreFlowTest` | 验证事项工单审核权限、跨社区拦截、材料完整性/预审失败拦截和事项工作人员自动分配逻辑。 |
| 核心功能测试 | `integration-test.md` | 记录真实环境下登录、申请、材料、工单、预约、家属代办和通知的端到端联调方法。 |

## 3. 测试结果

执行命令：

```bash
cd backend
mvn test
```

实际结果：

```text
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试结论：

- 状态机测试通过：合法流转被允许，非法状态跳转会被拒绝。
- 接口测试通过：接口返回统一结构 `{ "code": 200, "message": "success", "data": ... }`。
- 认证与权限测试通过：管理员登录、家属代办授权、无权限拦截均符合预期。
- 事项办理测试通过：在线事项可提交并创建工单，线下事项不能走在线申请流程。
- 服务预约测试通过：预约工作人员可主动接单，重复接单和事项办理工作人员接单会被拦截。
- 工单审核测试通过：跨社区处理、材料不完整或预审未通过时，审核通过/办结会被拦截。

## 4. 已知问题与缺陷

- 当前自动化测试重点覆盖后端核心业务和接口层，前端页面交互仍以手工联调和演示前验证为主。
- OCR/AI 预审依赖外部 DashScope 服务，自动化测试中不真实调用外部接口，避免测试结果受网络、额度和密钥影响。
- MySQL、Redis、文件上传下载等端到端流程需要在完整运行环境中配合测试数据继续做演示前验证。
