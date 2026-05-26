# 前端页面说明

## 页面路由一览

| 路由路径 | 页面文件 | 页面标题 | 访问权限 | 主要功能 |
|---|---|---|---|---|
| /login | LoginPage.vue | 登录 | 公开 | 用户名密码登录 |
| /home | HomePage.vue | 首页 | 登录后 | 角色导航入口 |
| /guide | GuidePage.vue | 智能导办 | 居民/家属 | AI 对话咨询 + 事项推荐 |
| /application/submit | ApplicationSubmitPage.vue | 提交申请 | 居民/家属 | 选择事项、填表、上传材料 |
| /application/list | ApplicationListPage.vue | 我的申请 | 居民/家属 | 申请列表+状态筛选 |
| /application/:id | ApplicationDetailPage.vue | 申请详情 | 居民/家属 | 申请详情+审核进度时间轴 |
| /booking | BookingPage.vue | 服务预约 | 居民/家属 | 选择服务类型+时间+提交 |
| /progress | ProgressPage.vue | 进度查询 | 居民/家属 | 快速查询所有进度 |
| /notice | NoticePage.vue | 消息通知 | 登录后 | 通知列表+已读未读管理 |
| /workorder | WorkOrderManagePage.vue | 工单管理 | 工作人员/管理员 | 工单列表+状态筛选 |
| /workorder/:id | WorkOrderDetailPage.vue | 工单详情 | 工作人员/管理员 | 工单详情+审核操作 |
| /admin/dashboard | StatisticsDashboardPage.vue | 统计看板 | 管理员 | ECharts 统计图表 |
| /admin/service-config | AdminServiceConfigPage.vue | 事项配置 | 管理员 | 事项增删改查 |
| /admin/user-manage | AdminUserManagePage.vue | 用户管理 | 管理员 | 用户列表+角色分配 |
| /403 | ForbiddenPage.vue | 无权限 | 公开 | 403 无权限提示 |
| /404 | NotFoundPage.vue | 页面不存在 | 公开 | 404 页面 |

## 路由守卫逻辑

1. 未登录访问需要认证的页面 → 跳转 `/login`
2. 已登录访问 `/login` → 跳转 `/home`
3. 角色不匹配 → 跳转 `/403`
