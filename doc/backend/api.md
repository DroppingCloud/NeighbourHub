# 后端接口文档

> 所有接口路径以 `/api` 开头，统一返回 `{ "code": 200, "message": "success", "data": {} }`。除登录、注册外均需要登录；管理后台接口需要 `ROLE_ADMIN`，工单和统计接口主要面向 `ROLE_STAFF` / `ROLE_ADMIN`。

## 认证管理

- `POST /api/auth/login`：用户登录。请求体：`username`、`password`。返回 token、用户 ID、用户名和角色列表。
- `POST /api/auth/register`：用户注册。请求体：`username`、`password`、`phone`、`email`、`realName`、`idCard` 等。
- `POST /api/auth/logout`：退出登录。
- `GET /api/auth/me`：获取当前登录用户信息。

## 智能导办

- `POST /api/guide/recommend`：根据居民类型、年龄、需求类型、描述推荐事项和材料。
- `POST /api/guide/chat`：AI 对话咨询，支持 `sessionId`。

## 事项申请

- `POST /api/application/submit`：提交申请。请求体：`itemId`、`proxyUserId`、`formData`、`remark`。写入 `application_form`，同时创建 `work_order` 并发送通知。
- `GET /api/application/list`：申请列表。参数：`status`、`itemId`、`pageNum`、`pageSize`。
- `GET /api/application/{id}`：申请详情，仅本人或代理人可查看。
- `PUT /api/application/{id}/resubmit`：补件重新提交，仅 `supplement_required` 状态允许操作。

## 申请材料

- `POST /api/application/{id}/materials`：登记申请材料元数据。请求体：`templateId`、`materialName`、`fileName`、`filePath`、`fileSize`、`fileType`。返回 `materialId`。
- `GET /api/application/{id}/materials`：查询申请单材料列表。
- `PUT /api/application/material/{id}/precheck`：更新材料预审结果。请求体：`precheckStatus`、`precheckRemark`、`ocrText`。`precheckStatus` 可为 `pending`、`passed`、`failed`。

## 工单管理

- `GET /api/workorder/list`：工单列表。参数：`status`、`staffUserId`、`pageNum`、`pageSize`。
- `GET /api/workorder/{id}`：工单详情。
- `POST /api/workorder/audit`：审核工单。请求体：`orderId`、`action`、`opinion`。`action` 支持 `approved`、`rejected`、`supplement_required`、`completed`、`processing`。
- `GET /api/workorder/{id}/logs`：查询工单操作日志。

## 服务预约

- `POST /api/booking/create`：发起预约。请求体：`serviceType`、`expectTime`、`address`、`remark`。`serviceType` 支持 `dining`、`accompany`、`home_visit`。
- `GET /api/booking/list`：预约列表。参数：`pageNum`、`pageSize`。
- `GET /api/booking/{id}`：预约详情。
- `PUT /api/booking/{id}/cancel`：取消预约，仅 `pending` / `confirmed` 状态允许。
- `PUT /api/booking/{id}/assign`：分配工作人员。请求体：`staffUserId`，预约状态更新为 `confirmed`。
- `PUT /api/booking/{id}/complete`：完成预约服务。请求体：`feedback`，预约状态更新为 `completed`。
- `POST /api/booking/{id}/feedback`：提交服务反馈，仅本人或代理人可操作已完成预约。

## 通知消息

- `GET /api/notice/list`：通知列表。参数：`pageNum`、`pageSize`。
- `PUT /api/notice/{id}/read`：标记单条通知已读，同时写入 `readTime`。
- `PUT /api/notice/read-all`：全部标记已读。
- `GET /api/notice/unread-count`：获取未读数量。

## 家属绑定

- `POST /api/proxy/bind`：创建家属/代理授权关系。请求体：`targetUserId`、`targetProfileId`、`relation`、`authorizedActions`。至少填写 `targetUserId` 或 `targetProfileId`。
- `GET /api/proxy/list`：查询当前登录用户作为代理人的授权关系列表。
- `PUT /api/proxy/{id}/revoke`：撤销当前登录用户名下的一条授权关系，状态更新为 `revoked`。

## 管理后台

- `GET /api/admin/service-item/list`：事项列表。参数：`category`、`status`、`pageNum`、`pageSize`。
- `POST /api/admin/service-item`：创建事项。
- `PUT /api/admin/service-item/{id}`：更新事项。
- `DELETE /api/admin/service-item/{id}`：删除事项，使用 MyBatis-Plus 逻辑删除。
- `GET /api/admin/service-item/{itemId}/materials`：查询事项材料模板列表。
- `POST /api/admin/material-template`：创建材料模板。字段：`itemId`、`materialName`、`materialType`、`description`、`sampleUrl`、`isRequired`、`sortOrder`。
- `PUT /api/admin/material-template/{id}`：更新材料模板。
- `DELETE /api/admin/material-template/{id}`：删除材料模板。

## 统计分析

- `GET /api/statistics/overview`：统计概览。
- `GET /api/statistics/items`：事项办理统计。
- `GET /api/statistics/services`：社区服务预约统计。

## 主要错误码

- `1001` 用户名或密码错误；`1002` 账号不存在；`1003` 账号禁用；`1006` 账号已存在。
- `2001` 事项不存在；`2002` 事项已下线；`2003` 不满足申请条件。
- `2101` 申请单不存在；`2102` 申请状态不允许操作；`2103` 无权操作申请。
- `2201` 工单不存在；`2202` 无权处理工单；`2203` 工单状态不允许操作。
- `2301` 服务不可用；`2302` 预约时间冲突；`2303` 预约不存在；`2304` 预约状态不允许操作。
