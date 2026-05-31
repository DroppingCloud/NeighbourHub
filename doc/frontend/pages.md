# 前端页面说明

本文档根据当前 `frontend/src/router/index.ts` 和 `frontend/src/pages` 目录整理，描述前端页面、路由权限、主要接口依赖和当前完成状态。

## 页面路由总览

| 路由路径 | 页面文件 | 页面标题 | 访问权限 | 当前状态 | 主要功能 |
|---|---|---|---|---|---|
| `/` | - | - | - | 已实现 | 重定向到 `/home` |
| `/login` | `LoginPage.vue` | 登录 | 公开 | 已实现 | 用户名密码登录；其他登录方式显示为暂未开放 |
| `/register` | `RegisterPage.vue` | 注册 | 公开 | 已实现 | 居民/家属账号注册 |
| `/home` | `HomePage.vue` | 首页 | 登录用户 | 已实现 | 角色化首页、快捷入口、最近申请等 |
| `/profile` | `ProfilePage.vue` | 个人中心 | 登录用户 | 已实现 | 查看和编辑个人资料，展示个人数据概览 |
| `/settings` | `SettingsPage.vue` | 系统设置 | 登录用户 | 已实现 | 字体大小、关怀模式等偏好设置 |
| `/guide` | `GuidePage.vue` | 智能导办 | 居民/家属 | 基础实现 | 导办问答、事项推荐入口；当前偏规则化实现 |
| `/application/submit` | `ApplicationSubmitPage.vue` | 提交申请 | 居民/家属 | 已实现 | 选择事项、填写申请、提交事项申请 |
| `/application/list` | `ApplicationListPage.vue` | 我的申请 | 居民/家属 | 已实现 | 查看申请记录、按状态筛选、补件、撤回、已撤回后重新提交 |
| `/material-upload` | `MaterialUploadPage.vue` | 材料上传 | 居民/家属 | 已增强 | 展示材料清单、必需标识、格式要求、正式模板预览/下载、规则型智能预审、补件和已撤回申请重新提交 |
| `/booking` | `BookingPage.vue` | 服务预约 | 居民/家属 | 已实现 | 创建社区服务预约 |
| `/booking/list` | `BookingListPage.vue` | 我的预约 | 居民/家属 | 已实现 | 查看预约列表、取消预约 |
| `/family-binding` | `FamilyBindingPage.vue` | 家属代办 | 居民/家属 | 已实现 | 创建、查看和撤销家属代办授权 |
| `/progress` | `ProgressPage.vue` | 进度查询 | 居民/家属 | 已实现 | 查询申请与服务进度 |
| `/notice` | `NoticePage.vue` | 消息通知 | 登录用户 | 已实现 | 通知列表、未读状态、标记已读、关联跳转 |
| `/workorder` | `WorkOrderManagePage.vue` | 工单管理 | 工作人员/管理员 | 已实现 | 工单列表、筛选、审核、状态流转 |
| `/staff/workbench` | `StaffWorkbenchPage.vue` | 工作人员工作台 | 工作人员/管理员 | 已实现 | 工作台概览、待办入口 |
| `/staff/booking` | `ServiceBookingsStaffPage.vue` | 服务调度 | 工作人员/管理员 | 已实现 | 查看预约、分配工作人员、完成服务 |
| `/admin` | - | 后台管理 | 管理员 | 已实现 | 重定向到 `/admin/dashboard` |
| `/admin/dashboard` | `AdminDashboardPage.vue` | 后台管理 | 管理员 | 已实现 | 管理端概览和功能入口 |
| `/admin/service-config` | `AdminServiceConfigPage.vue` | 事项配置 | 管理员 | 基础实现 | 事项和材料模板增删改查 |
| `/admin/service-resource` | `ServiceResourcePage.vue` | 服务资源 | 管理员 | 暂未开放 | 服务资源、时段和排班尚未建模，页面显示说明和跳转入口 |
| `/admin/user-manage` | `AdminUserManagePage.vue` | 用户管理 | 管理员 | 基础实现 | 用户列表和用户管理入口 |
| `/admin/statistics` | `StatisticsDashboardPage.vue` | 统计分析 | 管理员 | 已实现 | 统计总览、事项统计、预约统计图表 |
| `/service-feedback` | `ServiceFeedbackPage.vue` | 服务评价 | 登录用户 | 基础实现 | 服务反馈入口，完整评价闭环待完善 |
| `/:pathMatch(.*)*` | - | - | - | 已实现 | 未匹配路由重定向到 `/home` |

## 权限规则

前端路由通过 `meta.requiresAuth` 和 `meta.roles` 控制访问：

- 公开页面：`/login`、`/register`。
- 登录后可访问：`/home`、`/profile`、`/settings`、`/notice`、`/service-feedback`。
- 居民/家属可访问：智能导办、事项申请、材料上传、预约、进度查询、家属代办。
- 工作人员/管理员可访问：工单管理、工作人员工作台、服务调度。
- 管理员可访问：后台管理、事项配置、服务资源、用户管理、统计分析。

角色值同时兼容两种形式：

- 简写：`resident`、`family`、`staff`、`admin`
- 后端角色码：`ROLE_RESIDENT`、`ROLE_FAMILY`、`ROLE_STAFF`、`ROLE_ADMIN`

## 页面与接口依赖

| 页面 | 主要接口文件 | 说明 |
|---|---|---|
| `LoginPage.vue`、`RegisterPage.vue`、`ProfilePage.vue` | `api/auth.ts` | 登录、注册、当前用户、资料更新 |
| `GuidePage.vue` | `api/guide.ts` | 导办推荐和对话 |
| `ApplicationSubmitPage.vue`、`ApplicationListPage.vue`、`ProgressPage.vue` | `api/application.ts`、`api/serviceItem.ts` | 事项列表、申请提交、申请查询、撤回申请 |
| `MaterialUploadPage.vue`、`utils/materialTemplateLibrary.ts` | `api/application.ts` | 材料登记、材料查询、正式材料模板预览/下载、规则型预审、预审结果更新、补件重新提交、撤回后重新提交 |
| `BookingPage.vue`、`BookingListPage.vue`、`ServiceBookingsStaffPage.vue` | `api/booking.ts` | 预约创建、查询、取消、分配、完成 |
| `FamilyBindingPage.vue` | `api/user.ts` | 家属绑定、授权列表、撤销授权 |
| `NoticePage.vue`、`NotificationBell.vue` | `api/notice.ts` | 通知列表、未读数、标记已读 |
| `WorkOrderManagePage.vue`、`StaffWorkbenchPage.vue` | `api/workOrder.ts` | 工单列表、详情、审核、日志 |
| `AdminServiceConfigPage.vue` | `api/serviceItem.ts` | 后台事项和材料模板管理 |
| `AdminUserManagePage.vue` | `api/user.ts` | 后台用户管理 |
| `StatisticsDashboardPage.vue` | `api/statistics.ts` | 后台统计数据 |

## 当前注意事项

- `ServiceResourcePage.vue` 是占位说明页，不再使用本地模拟数据；完整服务资源排班需要后端补表和接口后再启用。
- `GuidePage.vue` 当前可作为导办入口演示，但智能程度有限，尚不是完整大模型导办。
- `MaterialUploadPage.vue` 已支持规则型智能预审和正式模板预览/下载；模板内容内置于 `utils/materialTemplateLibrary.ts`，已按 `材料模板示例.md` 对齐 17 份模板：房屋租赁合同、不动产权证书、购房合同、单位住宿证明、工商营业执照、劳动合同、劳动关系证明、学生证、连续就读证明、高龄津贴申请表、亲属关系证明、社区居住证明、无犯罪记录证明、低收入/困难证明、同一人身份证明、申请事由说明、助餐服务申请表。OCR 服务仍需后续补充。
- `StatisticsDashboardPage.vue` 已调用真实统计接口，但图表依赖较大，生产环境可考虑按需拆包。
- 页面文案若在 PowerShell 中显示乱码，优先以 VS Code UTF-8 显示为准；若浏览器页面实际乱码，则需要修复对应 `.vue` 文件源码。
