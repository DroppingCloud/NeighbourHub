# 用户权限与交互测试报告

## 一、测试统计

| 测试分类 | 用例数 | 通过 | 失败 | BUG |
|----------|--------|------|------|---------|
| 用户认证（AUTH） | 6 | 6 | 0 | 0 |
| 角色权限控制（RBAC） | 8 | 8 | 0 | 0 |
| 数据权限隔离（DATA） | 11 | 9 | 2 | 2 |
| 跨角色交互（CROSS） | 8 | 4 | 4 | 3 |
| **合计** | **33** | **27** | **6** | **5** |

## 二、用户认证测试

| 编号 | 场景 | 结果 | 说明 |
|------|------|------|------|
| AUTH-01 | 未携带Token访问受保护接口 | ✅ | 返回 401 |
| AUTH-02 | 携带无效Token访问 | ✅ | 返回 401 |
| AUTH-03 | 错误密码登录 | ✅ | 返回 1001 |
| AUTH-04 | 不存在用户登录 | ✅ | 返回 1001 |
| AUTH-05 | 重复用户名注册 | ✅ | 返回 1006 |
| AUTH-06 | 正常获取当前用户信息 | ✅ | 返回正确的用户信息和角色 |

## 三、角色权限控制测试

| 编号 | 场景 | 结果 | 说明 |
|------|------|------|------|
| RBAC-01 | 居民访问 /api/admin/* | ✅ | 返回 403 |
| RBAC-02 | 家属访问 /api/admin/* | ✅ | 返回 403 |
| RBAC-03 | 工作人员访问 /api/admin/* | ✅ | 返回 403 |
| RBAC-04 | 管理员正常访问管理后台 | ✅ | 返回 200 |
| RBAC-05 | 居民访问 /api/workorder/* | ✅ | 返回 403 |
| RBAC-06 | 工作人员正常访问工单接口 | ✅ | 返回 200 |
| RBAC-07 | 居民访问 /api/statistics/* | ✅ | 返回 403 |
| RBAC-08 | 管理员访问统计接口 | ✅ | 返回 200 |

## 四、数据权限隔离测试

| 编号 | 场景 | 结果 | 说明 |
|------|------|------|------|
| DATA-01 | 居民查看自己的申请列表 | ✅ | 仅返回本人数据 |
| DATA-02 | 居民查看自己的预约列表 | ✅ | 仅返回本人数据 |
| DATA-03 | 家属查看自己的申请列表 | ✅ | 仅返回本人数据 |
| DATA-04 | 家属尝试查看居民的申请详情（无代理上下文） | ✅ | 返回 2103  |
| DATA-05 | 家属尝试撤回居民的申请（直接传ID） | ✅ | 返回 2103 |
| DATA-06 | 通知仅返回当前用户的消息 | ✅ | 居民可见，家属不可见 |
| DATA-07 | 尝试标记他人通知为已读 | ✅ | 返回 404 |
| DATA-08 | 预约服务：居民无法访问staff接口 | ✅ | 返回 403 |
| DATA-09 | 预约服务：居民无法执行派单 | ✅ | 返回 403 |
| DATA-10 | 代办查询无授权关系时返回数据 | ❌ | BUG-001 |
| DATA-11 | 代办查看授权对象的申请详情 | ❌ | BUG-002 |

## 五、跨角色交互测试

| 编号 | 场景 | 结果 | 说明 |
|------|------|------|------|
| CROSS-01 | 家属代居民提交申请（有授权） | ✅ | 申请成功 |
| CROSS-02 | 居民尝试代办别人（无代办资格） | ❌ | BUG-003 |
| CROSS-03 | 居民尝试代约其他用户（无代办资格） | ❌ | BUG-003 |
| CROSS-04 | 工作人员审核工单（退回补件） | ✅ | 工单状态正确流转 |
| CROSS-05 | 居民尝试审核工单 | ✅ | RBAC 层返回 403 |
| CROSS-06 | 家属通过 /api/proxy/bind 直接绑定 | ❌ | BUG-004 |
| CROSS-07 | 代理关系撤销权限验证 | ✅ | 只有代理人本人可撤销 |
| CROSS-08 | 工单社区隔离（SecurityUtils） | ❌ | BUG-005 |

## 六、BUG

### BUG-001：代办查询接口缺少代理关系校验

**影响范围：** `/api/application/list`、`/api/booking/list`  
**复现步骤：**
1. 以任意已登录用户身份调用 `GET /api/application/list?_proxyFor=1`（目标为admin用户）
2. 系统返回 200 和空数据，而非拒绝无授权的代办查询

**问题原因：**  
`ApplicationServiceImpl.getList()` 和 `BookingServiceImpl.getList()` 在 `proxyFor != null` 时，直接按 `proxyUserId = proxyFor` 查询数据库，**未调用 `ProxyPermissionService.validateAndGetTargetUserId()` 校验当前用户是否有代理授权**。

**预期行为：** 
当 `_proxyFor` 参数不为空时，应先校验 `proxyRelation` 表中是否存在当前用户与目标用户的有效代理关系，无权时应返回 403。

### BUG-002：家属代查申请详情权限校验逻辑不完整

**影响范围：** `/api/application/{id}`（getDetail）  
**复现步骤：**
1. family01（userId=4）与 resident01（userId=3）存在有效代理关系
2. resident01 本人提交的申请（userId=3, proxyUserId=null）
3. family01 调用 `GET /api/application/3` 查看详情
4. 返回 2103 "无权操作该申请"

**问题原因：**  
`ApplicationServiceImpl.getDetail()` 的权限判断为：
```java
if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId()))
```
当居民本人提交的申请 `proxyUserId = null`，家属的 userId（4）既不等于 `application.userId`（3），也不等于 `proxyUserId`（null），所以被拒绝。

**预期行为：** 
应额外判断当前用户是否为目标用户的有效代理人（查询 proxy_relation 表）。

### BUG-003：代办提交接口未校验代理关系

**影响范围：** `/api/application/submit`、`/api/booking/create`  
**复现步骤：**
1. 以 resident01（userId=3）身份调用 `POST /api/application/submit?_proxyFor=4`
2. 居民没有对 family01 的代理关系，但提交成功

**问题原因：**  
`ApplicationController.submit()` 和 `BookingController.create()` 接收 `_proxyFor` 参数后直接传递给 Service 层（`dto.setProxyFor(proxyFor)`），Service 层未调用 `ProxyPermissionService` 验证代理关系就直接使用。

**预期行为：** 
当 `_proxyFor` 不为空时，必须先调用 `ProxyPermissionService.validateAndGetTargetUserId()` 校验授权关系，无授权应返回 403。

### BUG-004：/api/proxy/bind 接口绕过确认流程直接创建active关系

**影响范围：** `/api/proxy/bind`  
**复现步骤：**
1. 以 resident01 身份调用 `POST /api/proxy/bind`
2. body: `{"targetUserId":1,"relation":"parent","authorizedActions":"apply,booking,notice"}`
3. 直接创建了对 admin 的 active 代理关系，无需对方确认

**问题原因：**  
`ProxyRelationServiceImpl.bind()` 方法直接创建 status="active" 的代理关系，没有任何目标用户确认流程。虽然已有 `apply → pending → confirm` 的正规流程（通过 `/api/proxy/apply`），但 `/api/proxy/bind` 接口仍然对外开放。

**预期行为：** 
应废弃或移除 `/api/proxy/bind` 接口，或者限制 bind 接口为管理员专用。

### BUG-005：SecurityUtils.getCurrentUser() 始终为 null，工单社区隔离失效

**影响范围：** `WorkOrderServiceImpl.getList()`、`WorkOrderServiceImpl.getStats()`  
**复现步骤：**
1. 查看代码：`SecurityUtils.setCurrentUser()` 在整个项目中没有任何调用点
2. `WorkOrderServiceImpl.getList()` 中 `SecurityUtils.getCurrentUser()` 返回 null
3. 社区隔离条件 `if (currentUser != null && ...)` 永远为 false
4. 工作人员可看到所有社区的工单

**问题原因：**  
`JwtAuthenticationFilter` 只将 userId 和 roles 放入 SecurityContext 的 `Authentication` 对象中，但没有设置 `SecurityUtils.CURRENT_USER` ThreadLocal。`WorkOrderServiceImpl` 依赖 `SecurityUtils.getCurrentUser()` 获取完整 User 对象进行社区隔离判断，但该方法始终返回 null。

**预期行为：** 
在 `JwtAuthenticationFilter` 中，认证成功后应从数据库查询完整 User 对象并调用 `SecurityUtils.setCurrentUser(user)`；或者修改 WorkOrderServiceImpl 从 Authentication 中获取 userId 后自行查询 User 信息。

## 七、修复验证

| BUG | 修复文件 | 修复方式 |
|-----|----------|----------|
| BUG-001 | `ApplicationServiceImpl.java`、`BookingServiceImpl.java` | 在 getList 中增加 `proxyPermissionService.validateProxyPermission()` 调用 |
| BUG-002 | `ApplicationServiceImpl.java` | 在 getDetail 中增加 `proxyPermissionService.hasProxyPermission()` 判断 |
| BUG-003 | `ApplicationServiceImpl.java`、`BookingServiceImpl.java` | 在 submit/create 中增加 `proxyPermissionService.validateProxyPermission()` 调用 |
| BUG-004 | `ProxyRelationController.java` | 在 bind 方法上添加 `@PreAuthorize("hasRole('ADMIN')")` |
| BUG-005 | `SecurityUtils.java`、`WorkOrderServiceImpl.java` | 新增 `getCurrentUserId()` 从 SecurityContext 获取，替换原来依赖 ThreadLocal 的方式 |

### 修复验证测试结果

| 验证场景 | 预期 | 实际 | 结果 |
|----------|------|------|------|
| 居民尝试代办别人（无代理关系） | 403 | 403 | ✅ |
| 居民尝试代约无关用户 | 403 | 403 | ✅ |
| 无代理关系查询 _proxyFor | 403 | 403 | ✅ |
| 家属代居民提交申请（有授权） | 200 | 200 | ✅ |
| 家属代查居民申请详情（有授权） | 200 | 200 | ✅ |
| 家属代查预约列表（有授权） | 200 | 200 | ✅ |
| 居民尝试 proxy/bind | 403 | 403 | ✅ |
