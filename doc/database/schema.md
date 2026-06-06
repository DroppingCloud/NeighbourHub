# 数据库表结构说明

数据库：`community_service`  
字符集：`utf8mb4 / utf8mb4_unicode_ci`  
存储引擎：`InnoDB`  
主键策略：`BIGINT AUTO_INCREMENT`  
时间字段：统一使用 `DATETIME`，业务表包含 `create_time` / `update_time`。  
逻辑删除：`user`、`service_item`、`application_form`、`service_booking` 使用 `deleted` 字段（`0` 正常，`1` 已删除），其余表不使用逻辑删除。

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

---

## user

用户登录账号表，只保存认证和账号状态信息，业务信息存于 `resident_profile`。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `user_id` | `BIGINT` | PK, AUTO_INCREMENT | 用户 ID |
| `username` | `VARCHAR(50)` | NOT NULL, UNIQUE | 登录用户名 |
| `account` | `VARCHAR(50)` | UNIQUE | 系统分配账号，如 `SQxxxx` |
| `password` | `VARCHAR(100)` | NOT NULL | BCrypt 密码哈希 |
| `phone` | `VARCHAR(20)` | UNIQUE | 手机号 |
| `email` | `VARCHAR(100)` | | 邮箱 |
| `avatar` | `VARCHAR(255)` | | 头像文件相对路径 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `active` | `active/disabled` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |
| `role` | `VARCHAR(30)` | | `admin/staff/resident/family`，便于基础角色判断 |
| `staff_type` | `VARCHAR(30)` | | 工作人员业务类型：`application` 处理事项办理工单，`booking` 处理服务预约 |
| `community_id` | `BIGINT` | | 社区 ID，用于工作人员数据隔离 |

索引：`uk_username`、`uk_account`、`uk_phone`、`idx_status`、`idx_staff_type`、`idx_user_community_id`。
相关接口：`POST /api/auth/login`、`POST /api/auth/register`、`GET /api/auth/me`。

---

## user_role

用户与角色的多对多关联表，一个用户当前只分配一个角色。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 主键 |
| `user_id` | `BIGINT` | NOT NULL | 用户 ID |
| `role_code` | `VARCHAR(50)` | NOT NULL | `ROLE_RESIDENT/ROLE_FAMILY/ROLE_STAFF/ROLE_ADMIN` |

索引：`uk_user_role(user_id, role_code)`、`idx_user_id`。

---

## resident_profile

居民业务档案表，与登录账号分离。`user_id` 允许为空，支持家属先为未注册居民建档，后续再绑定账号。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `profile_id` | `BIGINT` | PK, AUTO_INCREMENT | 档案 ID |
| `user_id` | `BIGINT` | UNIQUE, NULLABLE | 关联登录用户 ID |
| `community_id` | `BIGINT` | | 社区 ID，用于居民档案和工单数据隔离 |
| `real_name` | `VARCHAR(50)` | NOT NULL | 真实姓名 |
| `id_card` | `VARCHAR(18)` | UNIQUE | 身份证号 |
| `gender` | `VARCHAR(10)` | | `male/female/private` |
| `birthday` | `DATE` | | 出生日期 |
| `address` | `VARCHAR(200)` | | 居住地址 |
| `age` | `INT` | | 年龄 |
| `resident_type` | `VARCHAR(20)` | DEFAULT `local` | `local/non_local` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |

索引：`uk_user_id`、`uk_id_card`、`idx_profile_community_id`。  
相关接口：`GET /api/auth/me`、`PUT /api/auth/me`。

---

## proxy_relation

家属/代理人与居民之间的授权关系表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 主键 |
| `proxy_user_id` | `BIGINT` | NOT NULL | 代理人（家属）用户 ID |
| `target_user_id` | `BIGINT` | NULLABLE | 被代理居民用户 ID |
| `target_profile_id` | `BIGINT` | NULLABLE | 被代理居民档案 ID |
| `relation` | `VARCHAR(20)` | | 关系类型，如 `child/spouse/volunteer` |
| `authorized_actions` | `VARCHAR(200)` | DEFAULT `apply,booking,query,notice` | 授权操作范围 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `active` | `pending/active/revoked/rejected` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |

索引：`uk_proxy_target_user(proxy_user_id, target_user_id)`、`uk_proxy_target_profile(proxy_user_id, target_profile_id)`、`idx_proxy_user_id`、`idx_target_user_id`、`idx_target_profile_id`、`idx_status`。  
相关接口：`POST /api/proxy/bind`、`GET /api/proxy/list`。

---

## service_item

可办理政务事项配置表，由管理员维护。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `item_id` | `BIGINT` | PK, AUTO_INCREMENT | 事项 ID |
| `item_name` | `VARCHAR(100)` | NOT NULL | 事项名称 |
| `item_code` | `VARCHAR(50)` | NOT NULL, UNIQUE | 事项编码 |
| `category` | `VARCHAR(50)` | NOT NULL | 分类：`证件/补贴/证明/服务` |
| `description` | `TEXT` | | 事项说明 |
| `conditions` | `TEXT` | | 办理条件 |
| `process_steps` | `TEXT` | | 办理流程 JSON 数组 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `online` | `online/offline` |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |

索引：`uk_item_code`、`idx_category`、`idx_status`。  
相关接口：`GET /api/service-item/list`、`GET /api/admin/service-item/list`。

---

## service_material_template

事项所需材料模板表，由管理员配置，居民提交申请时参照此表准备材料。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `template_id` | `BIGINT` | PK, AUTO_INCREMENT | 模板 ID |
| `item_id` | `BIGINT` | NOT NULL | 关联事项 ID |
| `material_name` | `VARCHAR(100)` | NOT NULL | 材料名称 |
| `material_type` | `VARCHAR(50)` | | 材料类型编码 |
| `description` | `VARCHAR(200)` | | 材料说明 |
| `sample_url` | `VARCHAR(500)` | | 示例或模板下载地址 |
| `is_required` | `TINYINT` | NOT NULL, DEFAULT 1 | 是否必需：`1` 必需，`0` 可选 |
| `sort_order` | `INT` | NOT NULL, DEFAULT 0 | 展示顺序 |

索引：`idx_item_id`、`uk_item_material(item_id, material_type)`。  
相关接口：`GET /api/service-item/{id}/materials`、`GET /api/admin/service-item/{itemId}/materials`。

---

## application_form

居民或家属提交的事项申请单，提交后自动创建对应工单。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `application_id` | `BIGINT` | PK, AUTO_INCREMENT | 申请 ID |
| `user_id` | `BIGINT` | NOT NULL | 申请用户 ID |
| `profile_id` | `BIGINT` | | 申请对象居民档案 ID |
| `item_id` | `BIGINT` | NOT NULL | 事项 ID |
| `proxy_user_id` | `BIGINT` | | 家属代办人用户 ID |
| `status` | `VARCHAR(30)` | NOT NULL, DEFAULT `pending` | 申请状态（见下方） |
| `form_data` | `TEXT` | | 表单数据 JSON |
| `remark` | `VARCHAR(500)` | | 用户备注或审核摘要 |
| `submit_time` | `DATETIME` | NOT NULL | 提交时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |

索引：`idx_user_id`、`idx_profile_id`、`idx_item_id`、`idx_proxy_user_id`、`idx_status`、`idx_submit_time`。  
状态：`pending`（待审核）→ `approved`（已通过）/ `rejected`（已驳回）/ `supplement_required`（需补件）→ `supplementing`（补件中）→ `completed`（已办结）。  
相关接口：`POST /api/application/submit`、`GET /api/application/list`、`PUT /api/application/{id}/resubmit`。

---

## application_material

申请材料文件元数据表，真实文件存储在本地文件服务或对象存储，本表只保存路径和识别结果。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `material_id` | `BIGINT` | PK, AUTO_INCREMENT | 材料 ID |
| `application_id` | `BIGINT` | NOT NULL | 关联申请 ID |
| `template_id` | `BIGINT` | | 对应材料模板 ID |
| `material_name` | `VARCHAR(100)` | NOT NULL | 材料名称 |
| `file_name` | `VARCHAR(200)` | NOT NULL | 原始文件名 |
| `file_path` | `VARCHAR(500)` | NOT NULL | 文件存储路径或 URL |
| `file_size` | `BIGINT` | | 文件大小（字节） |
| `file_type` | `VARCHAR(20)` | | 文件类型/扩展名 |
| `ocr_text` | `TEXT` | | OCR 识别文本 |
| `precheck_status` | `VARCHAR(20)` | DEFAULT `pending` | `pending/passed/failed` |
| `precheck_remark` | `VARCHAR(500)` | | 预审说明 |
| `upload_time` | `DATETIME` | NOT NULL | 上传时间 |

索引：`idx_application_id`、`idx_template_id`、`idx_precheck_status`。  
相关接口：`POST /api/application/{id}/materials`、`GET /api/application/{id}/materials`、`PUT /api/application/material/{id}/precheck`。

---

## work_order

工作人员处理事项申请的工单表，申请提交成功后由系统自动创建。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `order_id` | `BIGINT` | PK, AUTO_INCREMENT | 工单 ID |
| `application_id` | `BIGINT` | NOT NULL, UNIQUE | 关联申请 ID（一对一） |
| `staff_user_id` | `BIGINT` | | 处理工作人员 ID |
| `community_id` | `BIGINT` | | 社区 ID，用于工作人员仅查看本社区工单 |
| `status` | `VARCHAR(30)` | NOT NULL, DEFAULT `pending` | 工单状态（见下方） |
| `audit_opinion` | `TEXT` | | 审核意见 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `finish_time` | `DATETIME` | | 办结时间 |

索引：`uk_application_id`、`idx_staff_user_id`、`idx_work_order_community_id`、`idx_status`、`idx_create_time`、`idx_staff_status(staff_user_id, status)`。  
状态：`pending`（待审核）→ `approved`（审核通过）/ `rejected`（已驳回）/ `supplement_required`（退回补件）→ `completed`（已办结）。  
相关接口：`GET /api/workorder/list`、`POST /api/workorder/audit`、`GET /api/workorder/{id}/logs`。

---

## work_order_log

工单状态流转与操作审计日志表，每次状态变更写入一条记录。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `log_id` | `BIGINT` | PK, AUTO_INCREMENT | 日志 ID |
| `order_id` | `BIGINT` | NOT NULL | 工单 ID |
| `operator_id` | `BIGINT` | NOT NULL | 操作人用户 ID |
| `action` | `VARCHAR(50)` | NOT NULL | 操作动作 |
| `from_status` | `VARCHAR(30)` | | 变更前状态 |
| `to_status` | `VARCHAR(30)` | NOT NULL | 变更后状态 |
| `remark` | `TEXT` | | 操作说明 |
| `create_time` | `DATETIME` | NOT NULL | 操作时间 |

索引：`idx_order_id`、`idx_operator_id`、`idx_create_time`。  
常见动作：`approved/rejected/supplement_required/completed/processing`。  
相关接口：`GET /api/workorder/{id}/logs`。

---

## service_booking

助餐、陪诊、上门等社区服务预约表。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `booking_id` | `BIGINT` | PK, AUTO_INCREMENT | 预约 ID |
| `user_id` | `BIGINT` | NOT NULL | 服务对象用户 ID |
| `profile_id` | `BIGINT` | | 服务对象居民档案 ID |
| `proxy_user_id` | `BIGINT` | | 家属代约人用户 ID |
| `service_type` | `VARCHAR(50)` | NOT NULL | `dining/accompany/home_visit` |
| `expect_time` | `DATETIME` | NOT NULL | 期望服务时间 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `pending` | 预约状态（见下方） |
| `address` | `VARCHAR(200)` | | 服务地址 |
| `remark` | `VARCHAR(500)` | | 备注 |
| `staff_user_id` | `BIGINT` | | 执行工作人员 ID |
| `feedback` | `VARCHAR(500)` | | 服务反馈 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `update_time` | `DATETIME` | NOT NULL | 更新时间 |
| `complete_time` | `DATETIME` | | 完成时间 |
| `deleted` | `TINYINT` | NOT NULL, DEFAULT 0 | 逻辑删除 |
| `community_id` | `BIGINT` | | 社区 ID，用于工作人员服务预约数据隔离 |

索引：`idx_user_id`、`idx_profile_id`、`idx_proxy_user_id`、`idx_status`、`idx_expect_time`、`idx_staff_status(staff_user_id, status)`、`idx_booking_community_id`。  
状态：`pending`（待确认）→ `confirmed`（已确认）→ `in_progress`（服务中）→ `completed`（已完成）/ `cancelled`（已取消）。  
相关接口：`POST /api/booking/create`、`GET /api/booking/list`、`PUT /api/booking/{id}/cancel`。

---

## notice

站内通知和业务提醒表，由后端各业务流程自动写入。

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `notice_id` | `BIGINT` | PK, AUTO_INCREMENT | 通知 ID |
| `user_id` | `BIGINT` | NOT NULL | 接收用户 ID |
| `title` | `VARCHAR(200)` | NOT NULL | 通知标题 |
| `content` | `TEXT` | NOT NULL | 通知内容 |
| `type` | `VARCHAR(50)` | NOT NULL | 通知类型 |
| `ref_type` | `VARCHAR(50)` | | 关联业务类型 |
| `ref_id` | `BIGINT` | | 关联业务 ID |
| `is_read` | `TINYINT` | NOT NULL, DEFAULT 0 | 已读状态：`0` 未读，`1` 已读 |
| `create_time` | `DATETIME` | NOT NULL | 创建时间 |
| `read_time` | `DATETIME` | | 已读时间 |

索引：`idx_user_id`、`idx_is_read`、`idx_create_time`、`idx_user_read(user_id, is_read)`、`idx_ref(ref_type, ref_id)`。  
类型（`type`）：`audit_result/supplement/booking/system`。  
关联类型（`ref_type`）：`application/booking/work_order/system`。  
相关接口：`GET /api/notice/list`、`PUT /api/notice/{id}/read`、`PUT /api/notice/read-all`。

---

## 内置测试账号

默认密码均为 `123456`。

| username | role_code | phone | 说明 |
|---|---|---|---|
| `admin` | `ROLE_ADMIN` | - | 代码内置系统管理员，默认账号 `admin` / `123456`，不依赖数据库记录 |
| `staff01` | `ROLE_STAFF` | `13811111111` | 事项办理工作人员，`staff_type=application` |
| `booking01` | `ROLE_STAFF` | `13844444444` | 服务预约工作人员，`staff_type=booking` |
| `resident01` | `ROLE_RESIDENT` | `13822222222` | 居民（已建档，65岁，本地户籍） |
| `family01` | `ROLE_FAMILY` | `13833333333` | 家属（已绑定 resident01 为被代理人） |

## 重点表字段核对记录

本次已按 `init.sql`、Entity、Mapper 和接口返回对象复核以下申请/材料主流程表：

| 表名 | Entity | Mapper | 核对结论 |
|---|---|---|---|
| `resident_profile` | `ResidentProfile` | `ResidentProfileMapper` | 字段一致：`profile_id`、`user_id`、`community_id`、`real_name`、`id_card`、`address`、`age`、`gender`、`birthday`、`resident_type`、`create_time`、`update_time`。 |
| `service_item` | `ServiceItem` | `ServiceItemMapper` | 字段一致：`item_id`、`item_name`、`item_code`、`category`、`description`、`conditions`、`process_steps`、`status`、`create_time`、`update_time`、`deleted`。 |
| `service_material_template` | `ServiceMaterialTemplate` | `ServiceMaterialTemplateMapper` | 字段一致：`template_id`、`item_id`、`material_name`、`material_type`、`description`、`sample_url`、`is_required`、`sort_order`。 |
| `application_form` | `ApplicationForm` | `ApplicationFormMapper` | 字段一致：`application_id`、`user_id`、`profile_id`、`item_id`、`proxy_user_id`、`status`、`form_data`、`remark`、`submit_time`、`update_time`、`deleted`。 |
| `application_material` | `ApplicationMaterial` | `ApplicationMaterialMapper` | 字段一致：`material_id`、`application_id`、`template_id`、`material_name`、`file_name`、`file_path`、`file_size`、`file_type`、`ocr_text`、`precheck_status`、`precheck_remark`、`upload_time`。 |

状态约定：

- `application_form.status`：`pending`、`approved`、`rejected`、`supplement_required`、`supplementing`、`completed`、`cancelled`。
- `application_material.precheck_status`：`pending`、`passed`、`failed`。
- `work_order.status`：`pending`、`processing`、`approved`、`supplement_required`、`rejected`、`completed`、`cancelled`。

## 文件上传与材料访问

- 当前材料文件由后端 `POST /api/application/{id}/materials/file` 接收并保存到本地目录。
- 保存目录默认是 `uploads/materials`，可通过环境变量 `MATERIAL_UPLOAD_DIR` 或配置项 `app.upload.material-dir` 修改。
- `application_material.file_path` 保存相对路径，例如 `1001/uuid.pdf`；真实访问需要走 `GET /api/application/material/{id}/file`，后端会校验申请人、授权代办人、工作人员或管理员权限。
- `application_material.file_type` 保存文件扩展名；当前后端允许 `pdf/jpg/jpeg/png/doc/docx`，大小不超过 20MB。
- `ocr_text` 字段已预留，当前规则型预审不写入真实 OCR 结果。

## 联调样例数据

`init.sql` 目前补充了少量稳定样例，便于前后端联调。样例材料的 `file_path` 使用 `/mock/materials/...` 演示路径；通过真实上传接口提交的新材料会保存为后端本地相对路径。

| 申请ID | 事项 | 申请状态 | 工单状态 | 材料样例 |
|---|---|---|---|---|
| `1001` | 居住证明开具 | `pending` | `pending` | 身份证明材料、社区居住证明 |
| `1002` | 老年补贴申请 | `approved` | `approved` | 高龄津贴申请表、近期免冠两寸照片 |
| `1003` | 居住证办理 | `supplement_required` | `supplement_required` | 房屋租赁合同；租赁期限信息不完整，用于补件演示 |

这些样例不代表生产数据，只用于演示“待审核、已通过、待补件”和材料完整性校验。

