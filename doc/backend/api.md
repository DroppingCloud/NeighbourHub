# 后端接口文档

> 所有接口路径以 `/api` 开头，统一返回格式：`{ "code": 200, "message": "success", "data": {} }`

---

## 认证管理

### 用户登录

- **请求路径：** `POST /api/auth/login`
- **权限：** 公开
- **请求体：**
```json
{ "username": "admin", "password": "admin123" }
```
- **返回示例：**
```json
{ "code": 200, "message": "success", "data": { "token": "eyJ...", "userId": 1, "username": "admin", "roles": ["ROLE_ADMIN"] } }
```
- **异常：** 1001 密码错误 | 1002 账号不存在 | 1003 账号禁用

---

### 用户注册

- **请求路径：** `POST /api/auth/register`
- **权限：** 公开
- **请求体：**
```json
{ "username": "user001", "password": "pass123", "phone": "13800000001", "realName": "张三", "idCard": "330102199001011234" }
```
- **异常：** 1006 账号已存在

---

### 退出登录

- **请求路径：** `POST /api/auth/logout`
- **权限：** 需登录

---

### 获取当前用户信息

- **请求路径：** `GET /api/auth/me`
- **权限：** 需登录
- **返回示例：**
```json
{ "code": 200, "data": { "userId": 1, "username": "zhangsan", "roles": ["ROLE_RESIDENT"], "realName": "张三", "age": 67 } }
```

---

## 智能导办

### 事项推荐

- **请求路径：** `POST /api/guide/recommend`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **请求体：**
```json
{ "residentType": "local", "age": 67, "needType": "补贴", "description": "我想申请老年补贴" }
```
- **返回示例：**
```json
{ "code": 200, "data": { "items": [{ "itemId": 2, "itemName": "老年补贴申请", "category": "补贴" }], "materials": ["身份证", "户口本"], "steps": ["提交申请", "等待审核", "领取补贴"], "tips": "60岁以上本地户籍居民可申请", "isFallback": false } }
```
- **备注：** AI 服务不可用时降级为规则匹配，`isFallback=true`
- **相关页面：** GuidePage.vue

---

### AI 对话咨询

- **请求路径：** `POST /api/guide/chat`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **请求体：**
```json
{ "message": "我怎么办居住证？", "sessionId": "session-xxx" }
```

---

## 事项申请

### 提交申请

- **请求路径：** `POST /api/application/submit`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **请求体：**
```json
{ "itemId": 2, "proxyUserId": null, "formData": { "contactPhone": "13800000001" }, "remark": "" }
```
- **返回：** `data` 为 applicationId
- **相关数据表：** application_form, work_order
- **异常：** 2001 事项不存在 | 2002 事项已下线

---

### 申请列表

- **请求路径：** `GET /api/application/list`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **请求参数：** `status`, `itemId`, `pageNum`, `pageSize`
- **数据权限：** 居民只能看本人，家属只能看授权对象

---

### 申请详情

- **请求路径：** `GET /api/application/{id}`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY, ROLE_STAFF, ROLE_ADMIN
- **异常：** 2101 申请不存在 | 2103 无权限

---

### 补件重新提交

- **请求路径：** `PUT /api/application/{id}/resubmit`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **前置条件：** 申请状态必须为 `supplement_required`
- **异常：** 2102 状态不允许操作

---

## 工单管理

### 工单列表

- **请求路径：** `GET /api/workorder/list`
- **权限：** ROLE_STAFF, ROLE_ADMIN
- **请求参数：** `status`, `staffUserId`, `pageNum`, `pageSize`

---

### 工单详情

- **请求路径：** `GET /api/workorder/{id}`
- **权限：** ROLE_STAFF, ROLE_ADMIN

---

### 审核工单

- **请求路径：** `POST /api/workorder/audit`
- **权限：** ROLE_STAFF, ROLE_ADMIN
- **请求体：**
```json
{ "orderId": 1, "action": "approved", "opinion": "材料齐全，审核通过" }
```
- **action 可选值：** `approved` / `rejected` / `supplement_required` / `completed`
- **副作用：** 同步更新 application_form.status，记录 work_order_log，发送 notice

---

## 服务预约

### 发起预约

- **请求路径：** `POST /api/booking/create`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **请求体：**
```json
{ "serviceType": "dining", "expectTime": "2026-06-01 12:00:00", "address": "XX小区1号楼", "remark": "" }
```

---

### 预约列表

- **请求路径：** `GET /api/booking/list`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **请求参数：** `pageNum`, `pageSize`

---

### 取消预约

- **请求路径：** `PUT /api/booking/{id}/cancel`
- **权限：** ROLE_RESIDENT, ROLE_FAMILY
- **前置条件：** 状态为 `pending` 或 `confirmed`

---

## 消息通知

### 通知列表

- **请求路径：** `GET /api/notice/list`
- **权限：** 需登录
- **请求参数：** `pageNum`, `pageSize`

### 标记已读

- **请求路径：** `PUT /api/notice/{id}/read`
- **权限：** 需登录（仅本人通知）

### 全部已读

- **请求路径：** `PUT /api/notice/read-all`
- **权限：** 需登录

### 未读数量

- **请求路径：** `GET /api/notice/unread-count`
- **权限：** 需登录
- **返回：** `data` 为 Long 类型未读数量

---

## 管理后台（需 ROLE_ADMIN）

### 事项列表

- **请求路径：** `GET /api/admin/service-item/list`
- **请求参数：** `category`, `status`, `pageNum`, `pageSize`

### 创建事项

- **请求路径：** `POST /api/admin/service-item`
- **请求体：** `{ "itemName", "itemCode", "category", "description", "conditions", "status" }`

### 更新事项

- **请求路径：** `PUT /api/admin/service-item/{id}`

### 删除事项

- **请求路径：** `DELETE /api/admin/service-item/{id}`
- **说明：** 逻辑删除

### 用户列表

- **请求路径：** `GET /api/admin/user/list`
- **请求参数：** `role`, `pageNum`, `pageSize`

---

## 统计分析（需 ROLE_ADMIN 或 ROLE_STAFF）

### 统计概览

- **请求路径：** `GET /api/statistics/overview`
- **返回：** 总申请量、各状态计数、预约量、近7天趋势、事项排行

### 事项统计

- **请求路径：** `GET /api/statistics/items`

### 服务统计

- **请求路径：** `GET /api/statistics/services`
