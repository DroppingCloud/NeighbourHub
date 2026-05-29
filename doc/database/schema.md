# 数据库表结构说明

数据库：`community_service`  
字符集：`utf8mb4`  
排序规则：`utf8mb4_unicode_ci`  
存储引擎：`InnoDB`  
主键策略：`BIGINT AUTO_INCREMENT`  
时间字段：统一使用 `DATETIME`，业务表包含 `create_time` / `update_time`。  
逻辑删除：核心业务表使用 `deleted`，`0` 表示正常，`1` 表示已删除。

## 表清单

| 表名 | 说明 | 后端实体 |
|---|---|---|
| `user` | 用户登录账号 | `User` |
| `user_role` | 用户角色关联 | `UserRole` |
| `resident_profile` | 居民业务档案 | `ResidentProfile` |
| `proxy_relation` | 家属/代理授权关系 | `ProxyRelation` |
| `service_item` | 政务事项配置 | `ServiceItem` |
| `service_material_template` | 事项材料模板 | `ServiceMaterialTemplate` |
| `application_form` | 事项申请单 | `ApplicationForm` |
| `application_material` | 申请材料文件元数据 | `ApplicationMaterial` |
| `work_order` | 工作人员处理工单 | `WorkOrder` |
| `work_order_log` | 工单状态流转日志 | `WorkOrderLog` |
| `service_booking` | 社区服务预约 | `ServiceBooking` |
| `notice` | 站内通知消息 | `Notice` |

## user

用户登录账号表，只保存认证和账号状态信息。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `user_id` | `BIGINT` | PK, AUTO_INCREMENT | 用户 ID |
| `username` | `VARCHAR(50)` | NOT NULL, UNIQUE | 登录用户名 |
| `password` | `VARCHAR(100)` | NOT NULL | BCrypt 密码哈希 |
| `phone` | `VARCHAR(20)` | UNIQUE | 手机号 |
| `email` | `VARCHAR(100)` |  | 邮箱 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `active` | `active/disabled` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |

索引：`uk_username(username)`、`uk_phone(phone)`、`idx_status(status)`。

## user_role

用户与角色的多对多关联表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 主键 |
| `user_id` | `BIGINT` | NOT NULL | 用户 ID |
| `role_code` | `VARCHAR(50)` | NOT NULL | `ROLE_RESIDENT/ROLE_FAMILY/ROLE_STAFF/ROLE_ADMIN` |

索引：`uk_user_role(user_id, role_code)`、`idx_user_id(user_id)`。

## resident_profile

居民业务档案表，和登录账号分离。`user_id` 允许为空，用于支持家属先为未注册居民建档，后续再绑定账号。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `profile_id` | `BIGINT` | PK, AUTO_INCREMENT | 档案 ID |
| `user_id` | `BIGINT` | UNIQUE, NULLABLE | 关联登录用户 |
| `real_name` | `VARCHAR(50)` | NOT NULL | 真实姓名 |
| `id_card` | `VARCHAR(18)` | UNIQUE | 身份证号 |
| `address` | `VARCHAR(200)` |  | 居住地址 |
| `age` | `INT` |  | 年龄 |
| `gender` | `VARCHAR(10)` |  | `male/female/private` |
| `birthday` | `DATE` |  | 出生日期 |
| `resident_type` | `VARCHAR(20)` | DEFAULT `local` | `local/non_local` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |

索引：`uk_user_id(user_id)`、`uk_id_card(id_card)`。

## proxy_relation

家属/代理人与居民之间的授权关系。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 主键 |
| `proxy_user_id` | `BIGINT` | NOT NULL | 家属/代理人用户 ID |
| `target_user_id` | `BIGINT` | NULLABLE | 被代理居民用户 ID |
| `target_profile_id` | `BIGINT` | NULLABLE | 被代理居民档案 ID |
| `relation` | `VARCHAR(20)` |  | 关系类型，如子女、配偶、志愿者 |
| `authorized_actions` | `VARCHAR(200)` | DEFAULT `apply,booking,query,notice` | 授权范围 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `active` | `pending/active/revoked/rejected` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |

索引：`uk_proxy_target_user(proxy_user_id, target_user_id)`、`uk_proxy_target_profile(proxy_user_id, target_profile_id)`、`idx_proxy_user_id`、`idx_target_user_id`、`idx_target_profile_id`、`idx_status`。

## service_item

可办理政务事项配置表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `item_id` | `BIGINT` | PK, AUTO_INCREMENT | 事项 ID |
| `item_name` | `VARCHAR(100)` | NOT NULL | 事项名称 |
| `item_code` | `VARCHAR(50)` | NOT NULL, UNIQUE | 事项编码 |
| `category` | `VARCHAR(50)` | NOT NULL | 分类 |
| `description` | `TEXT` |  | 事项说明 |
| `conditions` | `TEXT` |  | 办理条件或规则 JSON |
| `process_steps` | `TEXT` |  | 办理流程 JSON |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `online` | `online/offline` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |

索引：`uk_item_code(item_code)`、`idx_category(category)`、`idx_status(status)`。

## service_material_template

事项所需材料模板表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `template_id` | `BIGINT` | PK, AUTO_INCREMENT | 模板 ID |
| `item_id` | `BIGINT` | NOT NULL | 事项 ID |
| `material_name` | `VARCHAR(100)` | NOT NULL | 材料名称 |
| `material_type` | `VARCHAR(50)` |  | 材料类型编码 |
| `description` | `VARCHAR(200)` |  | 材料说明 |
| `sample_url` | `VARCHAR(500)` |  | 示例或模板下载地址 |
| `is_required` | `TINYINT` | NOT NULL, DEFAULT 1 | 是否必需 |
| `sort_order` | `INT` | NOT NULL, DEFAULT 0 | 展示顺序 |

索引：`idx_item_id(item_id)`、`uk_item_material(item_id, material_type)`。

## application_form

居民或家属提交的事项申请单。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `application_id` | `BIGINT` | PK, AUTO_INCREMENT | 申请 ID |
| `user_id` | `BIGINT` | NOT NULL | 实际办理对象用户 ID |
| `profile_id` | `BIGINT` |  | 实际办理对象居民档案 ID |
| `item_id` | `BIGINT` | NOT NULL | 事项 ID |
| `proxy_user_id` | `BIGINT` |  | 家属代办人用户 ID |
| `status` | `VARCHAR(30)` | NOT NULL, DEFAULT `pending` | 申请状态 |
| `form_data` | `TEXT` |  | 表单 JSON |
| `remark` | `VARCHAR(500)` |  | 用户备注或审核摘要 |
| `submit_time` | `DATETIME` | NOT NULL | 提交时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |

索引：`idx_user_id`、`idx_profile_id`、`idx_item_id`、`idx_proxy_user_id`、`idx_status`、`idx_submit_time`。  
状态：`draft/pending/prechecking/supplement_required/supplementing/approved/rejected/completed/cancelled`。

## application_material

申请材料文件元数据表，真实文件存储在本地文件服务或对象存储中，本表只保存路径和识别结果。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `material_id` | `BIGINT` | PK, AUTO_INCREMENT | 材料 ID |
| `application_id` | `BIGINT` | NOT NULL | 申请 ID |
| `template_id` | `BIGINT` |  | 对应材料模板 ID |
| `material_name` | `VARCHAR(100)` | NOT NULL | 材料名称 |
| `file_name` | `VARCHAR(200)` | NOT NULL | 原始文件名 |
| `file_path` | `VARCHAR(500)` | NOT NULL | 文件路径或 URL |
| `file_size` | `BIGINT` |  | 文件大小 |
| `file_type` | `VARCHAR(20)` |  | 文件类型 |
| `ocr_text` | `TEXT` |  | OCR 文本 |
| `precheck_status` | `VARCHAR(20)` | DEFAULT `pending` | `pending/passed/failed` |
| `precheck_remark` | `VARCHAR(500)` |  | 预审说明 |
| `upload_time` | `DATETIME` | NOT NULL | 上传时间 |

索引：`idx_application_id`、`idx_template_id`、`idx_precheck_status`。

## work_order

工作人员处理事项申请的工单表。申请提交成功后创建工单。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `order_id` | `BIGINT` | PK, AUTO_INCREMENT | 工单 ID |
| `application_id` | `BIGINT` | NOT NULL, UNIQUE | 申请 ID |
| `staff_user_id` | `BIGINT` |  | 处理工作人员 ID |
| `status` | `VARCHAR(30)` | NOT NULL, DEFAULT `pending` | 工单状态 |
| `audit_opinion` | `TEXT` |  | 审核意见 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `finish_time` | `DATETIME` |  | 办结时间 |

索引：`uk_application_id`、`idx_staff_user_id`、`idx_status`、`idx_create_time`、`idx_staff_status(staff_user_id, status)`。  
状态：`pending/processing/approved/supplement_required/rejected/completed`。

## work_order_log

工单状态流转与操作审计表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `log_id` | `BIGINT` | PK, AUTO_INCREMENT | 日志 ID |
| `order_id` | `BIGINT` | NOT NULL | 工单 ID |
| `operator_id` | `BIGINT` | NOT NULL | 操作人用户 ID |
| `action` | `VARCHAR(50)` | NOT NULL | 操作动作 |
| `from_status` | `VARCHAR(30)` |  | 变更前状态 |
| `to_status` | `VARCHAR(30)` | NOT NULL | 变更后状态 |
| `remark` | `TEXT` |  | 操作说明 |
| `create_time` | `DATETIME` | NOT NULL | 操作时间 |

索引：`idx_order_id`、`idx_operator_id`、`idx_create_time`。  
常见动作：`create/assign/start/approve/reject/require_supplement/complete`。

## service_booking

助餐、陪诊、上门等社区服务预约表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `booking_id` | `BIGINT` | PK, AUTO_INCREMENT | 预约 ID |
| `user_id` | `BIGINT` | NOT NULL | 服务对象用户 ID |
| `profile_id` | `BIGINT` |  | 服务对象居民档案 ID |
| `proxy_user_id` | `BIGINT` |  | 家属代约人用户 ID |
| `service_type` | `VARCHAR(50)` | NOT NULL | `dining/accompany/home_visit` |
| `expect_time` | `DATETIME` | NOT NULL | 期望服务时间 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `pending` | 预约状态 |
| `address` | `VARCHAR(200)` |  | 服务地址 |
| `remark` | `VARCHAR(500)` |  | 备注 |
| `staff_user_id` | `BIGINT` |  | 执行工作人员 ID |
| `feedback` | `VARCHAR(500)` |  | 服务反馈 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `complete_time` | `DATETIME` |  | 完成时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |

索引：`idx_user_id`、`idx_profile_id`、`idx_proxy_user_id`、`idx_status`、`idx_expect_time`、`idx_staff_status(staff_user_id, status)`。  
状态：`pending/confirmed/in_progress/completed/cancelled`。

## notice

站内通知和业务提醒表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `notice_id` | `BIGINT` | PK, AUTO_INCREMENT | 通知 ID |
| `user_id` | `BIGINT` | NOT NULL | 接收用户 ID |
| `title` | `VARCHAR(200)` | NOT NULL | 标题 |
| `content` | `TEXT` | NOT NULL | 内容 |
| `type` | `VARCHAR(50)` | NOT NULL | 通知类型 |
| `ref_type` | `VARCHAR(50)` |  | 关联业务类型 |
| `ref_id` | `BIGINT` |  | 关联业务 ID |
| `is_read` | `TINYINT` | NOT NULL, DEFAULT 0 | 是否已读 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `read_time` | `DATETIME` |  | 已读时间 |

索引：`idx_user_id`、`idx_is_read`、`idx_create_time`、`idx_user_read(user_id, is_read)`、`idx_ref(ref_type, ref_id)`。  
类型：`audit_result/supplement/booking/progress/system`。

## 内置测试账号

默认密码均为 `123456`。

| username | role_code | phone | status |
|---|---|---|---|
| `admin` | `ROLE_ADMIN` | `13800000000` | `active` |
| `staff01` | `ROLE_STAFF` | `13811111111` | `active` |
| `resident01` | `ROLE_RESIDENT` | `13822222222` | `active` |
| `family01` | `ROLE_FAMILY` | `13833333333` | `active` |
