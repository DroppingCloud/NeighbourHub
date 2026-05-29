# 前端组件说明

本文档根据当前 `frontend/src/components`、`frontend/src/layout`、`frontend/src/stores`、`frontend/src/composables` 目录整理，说明仍在使用的公共组件、布局组件、状态管理和工具模块。

## 公共组件

| 文件 | 组件 | 当前状态 | 主要用途 | 使用位置 |
|---|---|---|---|---|
| `components/NotificationBell.vue` | `NotificationBell` | 已实现 | 顶部通知铃铛，展示未读数量、通知下拉列表，支持跳转到通知页 | `layout/AppHeader.vue` |
| `components/FontSizeControl.vue` | `FontSizeControl` | 已实现 | 字体大小调节控件，配合关怀模式和全局字体变量使用 | `layout/AppHeader.vue`、`layout/AuthLayout.vue` |

旧版组件 `GuideStepForm.vue`、`MaterialUploadPanel.vue`、`ServiceCard.vue`、`StatisticsChart.vue`、`TaskStatusTag.vue` 已不在当前 `components` 目录中，相关功能已经下沉到页面或改为直接使用 Element Plus/ECharts。

## 布局组件

| 文件 | 当前状态 | 说明 |
|---|---|---|
| `layout/AppHeader.vue` | 已实现 | 主应用顶部栏，包含平台名称、字体控制、通知入口、个人中心和退出登录 |
| `layout/AppSidebar.vue` | 已实现 | 主应用侧边栏，根据居民/家属、工作人员、管理员角色渲染不同菜单 |
| `layout/AuthLayout.vue` | 已实现 | 登录和注册页面布局，包含认证区域、品牌展示和字体控制 |

## 状态管理

| 文件 | 当前状态 | 说明 |
|---|---|---|
| `stores/auth.ts` | 已实现 | 保存 token、当前用户信息、登录、注册、退出、刷新用户信息 |
| `stores/notification.ts` | 已实现 | 调用真实通知接口，维护通知列表、未读数、已读状态 |
| `stores/service.ts` | 保留但不再承载 mock 数据 | 旧版前端业务 mock 数据已清理，后续如无使用可考虑删除 |

已删除或不再使用的旧 store：

- `dictStore.ts`
- `noticeStore.ts`
- `permissionStore.ts`
- `userStore.ts`

当前统一使用 `auth.ts`、`notification.ts` 和后端 API 文件获取真实数据。

## 组合式函数

| 文件 | 当前状态 | 说明 |
|---|---|---|
| `composables/useTheme.ts` | 已实现 | 管理主题、字体大小、关怀模式等显示偏好 |

## 类型定义

| 文件 | 当前状态 | 说明 |
|---|---|---|
| `types/index.ts` | 已实现 | 前端共享业务类型定义 |

## API 模块

| 文件 | 当前状态 | 主要职责 |
|---|---|---|
| `api/auth.ts` | 已实现 | 登录、注册、退出、当前用户、资料更新 |
| `api/application.ts` | 已实现 | 申请提交、申请查询、材料登记、材料预审 |
| `api/booking.ts` | 已实现 | 居民预约、预约列表、取消、工作人员分配和完成 |
| `api/guide.ts` | 已实现 | 导办推荐和对话 |
| `api/notice.ts` | 已实现 | 通知列表、未读数、已读操作 |
| `api/serviceItem.ts` | 已实现 | 事项列表、后台事项管理、材料模板管理 |
| `api/statistics.ts` | 已实现 | 统计总览、事项统计、服务统计 |
| `api/user.ts` | 已实现 | 用户管理、家属绑定相关接口 |
| `api/workOrder.ts` | 已实现 | 工单列表、详情、审核、日志 |

## 样式与响应式约定

当前前端样式主要在 `styles/global.css` 和各页面 scoped style 中维护。

建议继续遵守以下约定：

- 字体大小优先使用 `rem`，组件内间距优先使用 `em`、`rem` 或响应式布局。
- 固定尺寸控件要设置合理的 `min-width`、`max-width`、`min-height`，避免字体放大后内容溢出。
- 列表、卡片、按钮中的文字需要允许换行，必要时使用 `word-break: break-word`。
- 页面布局优先使用 Flex/Grid，避免依赖大量固定像素宽高。
- 与通知、申请、预约、工单、统计相关的数据应通过 `api/` 模块调用后端接口，不再新增本地 mock 数据。

## 后续维护建议

- 新增公共组件时，应同步更新本文档。
- 如果页面内出现重复的表格、状态标签、材料上传控件等逻辑，再考虑抽取为公共组件。
- 如果 `stores/service.ts` 后续确认无引用，可以删除并同步更新文档。
- 若新增 API 文件，需要同步补充“API 模块”表格。
