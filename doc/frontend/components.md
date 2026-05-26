# 前端组件说明

## 通用组件

| 组件文件 | 组件名 | 用途 | Props | Emits | 使用页面 |
|---|---|---|---|---|---|
| TaskStatusTag.vue | TaskStatusTag | 状态标签展示 | `status: string`, `type?: string` | - | ApplicationListPage, WorkOrderManagePage, BookingPage |
| ServiceCard.vue | ServiceCard | 事项卡片 | `item: {itemId, itemName, category, description}` | `apply(itemId)` | GuidePage, ApplicationSubmitPage |
| MaterialUploadPanel.vue | MaterialUploadPanel | 材料上传 | - | `uploaded(files)` | ApplicationSubmitPage |
| GuideStepForm.vue | GuideStepForm | 步骤向导表单 | `steps: Step[]` | `submit(data)` | 可复用 |
| StatisticsChart.vue | StatisticsChart | ECharts 图表容器 | `options: EChartsOption` | - | StatisticsDashboardPage |

## 布局组件

| 组件文件 | 说明 |
|---|---|
| layout/DefaultLayout.vue | 主布局框架（侧边栏+顶部导航+内容区），包含菜单渲染、未读消息数、退出登录 |
