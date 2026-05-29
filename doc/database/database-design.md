# 数据库设计方案

## 1. 设计目标

数据库服务于“多事项智能导办与社区服务协同平台”，支撑从咨询导办、事项申请、材料上传、工单审核、社区服务预约、通知反馈到后台统计的闭环管理。

核心目标：

- 统一管理居民、家属、工作人员、管理员账号和角色权限。
- 支撑政务事项配置、材料模板、申请表单、材料预审和工单流转。
- 支撑家属代办授权、代办申请、代约服务和进度查询。
- 支撑助餐、陪诊、上门服务等社区服务预约、派单、完成和反馈。
- 支撑站内通知、工单日志和后台统计分析。

## 2. 设计原则

1. 以当前工程命名为准：采用 `service_item`、`application_form`、`proxy_relation`、`notice` 等已落地表名。
2. 账号与居民档案分离：`user` 表示可登录账号，`resident_profile` 表示居民业务身份资料。
3. 角色与账号解耦：通过 `user_role` 支持一个用户拥有多个角色。
4. 事项配置可扩展：事项规则放在 `service_item`，材料要求放在 `service_material_template`。
5. 申请与工单分离：`application_form` 记录居民提交的申请，`work_order` 记录工作人员处理任务。
6. 文件不直接入库：`application_material` 只保存文件元数据、路径和预审结果。
7. 状态值统一使用英文枚举，前端展示时再映射成中文。
8. 关键操作可追溯：工单流转写入 `work_order_log`，通知写入 `notice`。
9. 业务数据优先逻辑删除，避免误删历史记录。

## 3. 数据库概览

- 数据库名：`community_service`
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`
- 存储引擎：`InnoDB`
- 主键类型：`BIGINT AUTO_INCREMENT`
- 时间字段：统一使用 `DATETIME`
- 逻辑删除字段：`deleted TINYINT NOT NULL DEFAULT 0`

## 4. 核心实体关系

| 实体 | 表名 | 说明 |
|---|---|---|
| 用户账号 | `user` | 所有可登录账号，包括居民、家属、工作人员、管理员 |
| 用户角色 | `user_role` | 用户与角色的多对多关系 |
| 居民档案 | `resident_profile` | 居民身份、住址、年龄、性别、生日等业务资料 |
| 代理关系 | `proxy_relation` | 家属/代理人与被代理居民之间的授权关系 |
| 政务事项 | `service_item` | 居住证、老年补贴、证明开具等事项配置 |
| 材料模板 | `service_material_template` | 每个事项要求提交的材料清单 |
| 申请单 | `application_form` | 居民或家属提交的事项申请 |
| 申请材料 | `application_material` | 申请单关联的材料文件元数据 |
| 工单 | `work_order` | 工作人员审核处理申请的任务 |
| 工单日志 | `work_order_log` | 工单状态流转和审核操作记录 |
| 服务预约 | `service_booking` | 助餐、陪诊、上门服务等预约记录 |
| 通知消息 | `notice` | 补件、审核结果、预约提醒、系统消息 |

主要关系：

- `user` 1 - N `user_role`
- `user` 1 - 0..1 `resident_profile`
- `user` 1 - N `proxy_relation.proxy_user_id`
- `service_item` 1 - N `service_material_template`
- `service_item` 1 - N `application_form`
- `application_form` 1 - N `application_material`
- `application_form` 1 - 0..1 `work_order`
- `work_order` 1 - N `work_order_log`
- `user` 1 - N `service_booking`
- `user` 1 - N `notice`

## 5. 状态枚举

### 用户状态

| 状态值 | 含义 |
|---|---|
| `active` | 正常 |
| `disabled` | 禁用 |

### 角色编码

| 角色编码 | 含义 |
|---|---|
| `ROLE_RESIDENT` | 居民 |
| `ROLE_FAMILY` | 家属/代理人 |
| `ROLE_STAFF` | 社区工作人员 |
| `ROLE_ADMIN` | 系统管理员 |

### 代理关系状态

| 状态值 | 含义 |
|---|---|
| `pending` | 待确认或待审核 |
| `active` | 已授权 |
| `revoked` | 已撤销 |
| `rejected` | 已拒绝 |

### 事项状态

| 状态值 | 含义 |
|---|---|
| `online` | 上线可办理 |
| `offline` | 下线不可办理 |

### 申请状态

| 状态值 | 含义 |
|---|---|
| `draft` | 草稿 |
| `pending` | 待审核 |
| `prechecking` | 材料预审中 |
| `supplement_required` | 待补件 |
| `supplementing` | 补件中 |
| `approved` | 审核通过 |
| `rejected` | 已驳回 |
| `completed` | 已办结 |
| `cancelled` | 已取消 |

推荐流转：

```text
draft -> pending -> prechecking -> pending -> approved -> completed
                         |
                         -> supplement_required -> supplementing -> pending
pending -> rejected
pending -> cancelled
```

### 材料预审状态

| 状态值 | 含义 |
|---|---|
| `pending` | 待预审 |
| `passed` | 预审通过 |
| `failed` | 预审失败 |

### 工单状态

| 状态值 | 含义 |
|---|---|
| `pending` | 待处理 |
| `processing` | 处理中 |
| `approved` | 审核通过 |
| `supplement_required` | 退回补件 |
| `rejected` | 已驳回 |
| `completed` | 已办结 |

### 预约状态

| 状态值 | 含义 |
|---|---|
| `pending` | 待调度 |
| `confirmed` | 已派单/已确认 |
| `in_progress` | 服务中 |
| `completed` | 已完成 |
| `cancelled` | 已取消 |

### 通知类型

| 类型 | 含义 |
|---|---|
| `audit_result` | 审核结果 |
| `supplement` | 补件提醒 |
| `booking` | 预约提醒 |
| `progress` | 进度更新 |
| `system` | 系统消息 |

## 6. 表结构说明

详细字段说明见 [schema.md](schema.md)。完整建库和种子数据脚本见 [init.sql](init.sql)。旧库升级说明见 [migration.md](migration.md)。

## 7. 已落地字段调整

| 表 | 调整 | 原因 |
|---|---|---|
| `resident_profile` | `user_id` 可为空，新增 `gender`、`birthday` | 支持独立居民档案和个人中心资料维护 |
| `proxy_relation` | 新增 `target_profile_id`、`relation`、`authorized_actions`、`update_time` | 完整表达代办授权 |
| `service_item` | 新增 `process_steps` | 支持流程引导展示 |
| `service_material_template` | 新增 `material_type`、`sample_url`、`sort_order` | 支持材料上传、模板下载和排序 |
| `application_form` | 新增 `profile_id` | 申请对象不只依赖账号 |
| `application_material` | 新增 `template_id`、`ocr_text` | 支持材料模板匹配和 OCR 预审 |
| `work_order` | 新增 `finish_time` | 支持办结统计 |
| `service_booking` | 新增 `profile_id`、`proxy_user_id`、`feedback`、`complete_time` | 支持代约、反馈和统计 |
| `notice` | 新增 `ref_type`、`read_time` | 支持多业务跳转和已读审计 |

## 8. 关键业务数据流

### 事项申请与工单审核

1. 居民或家属选择 `service_item`。
2. 系统读取 `service_material_template` 生成材料清单。
3. 用户提交 `application_form`。
4. 用户上传 `application_material`，系统记录预审结果。
5. 系统创建 `work_order`，状态为 `pending`。
6. 工作人员审核工单。
7. 工单状态写入 `work_order`，操作轨迹写入 `work_order_log`。
8. 系统同步更新 `application_form.status`。
9. 系统创建 `notice` 通知居民或家属。

### 家属代办

1. 家属账号具备 `ROLE_FAMILY`。
2. 家属与居民通过 `proxy_relation` 建立授权关系。
3. 提交申请或预约时，后端校验 `proxy_relation.status = active`。
4. 代办申请写入 `application_form.proxy_user_id`。
5. 代约服务写入 `service_booking.proxy_user_id`。
6. 系统向被代理居民发送 `notice`，保证透明可追溯。

### 社区服务预约

1. 居民或家属创建 `service_booking`。
2. 工作人员或系统调度写入 `staff_user_id`。
3. 状态从 `pending` 流转到 `confirmed/in_progress/completed/cancelled`。
4. 关键节点写入 `notice`。
5. 完成后记录 `feedback` 和 `complete_time`，用于统计分析。

## 9. 索引设计建议

| 表 | 索引 | 用途 |
|---|---|---|
| `user` | `uk_username`、`uk_phone` | 登录、注册唯一性校验 |
| `user_role` | `uk_user_role` | 权限加载 |
| `resident_profile` | `uk_user_id`、`uk_id_card` | 档案查询、实名校验 |
| `proxy_relation` | `uk_proxy_target_user`、`uk_proxy_target_profile`、`idx_proxy_user_id` | 家属授权校验 |
| `service_item` | `uk_item_code`、`idx_category`、`idx_status` | 事项检索、后台筛选 |
| `service_material_template` | `idx_item_id` | 按事项加载材料清单 |
| `application_form` | `idx_user_id`、`idx_item_id`、`idx_status`、`idx_submit_time` | 我的申请、后台筛选、统计 |
| `application_material` | `idx_application_id` | 查询申请材料 |
| `work_order` | `uk_application_id`、`idx_staff_user_id`、`idx_status`、`idx_staff_status` | 工单列表、工作人员任务 |
| `work_order_log` | `idx_order_id`、`idx_create_time` | 状态轨迹 |
| `service_booking` | `idx_user_id`、`idx_status`、`idx_expect_time`、`idx_staff_status` | 预约列表、调度、统计 |
| `notice` | `idx_user_read`、`idx_create_time`、`idx_ref` | 通知列表、未读数量、业务跳转 |

## 10. 安全与合规

1. `user.password` 必须保存 BCrypt 等不可逆哈希，不能保存明文密码。
2. `id_card`、`phone`、`address` 属于敏感字段，生产环境建议加密或脱敏。
3. 文件上传只保存路径，真实文件应限制访问权限，不能直接公开访问。
4. 涉及本人数据的查询必须按当前登录用户做数据权限过滤。
5. 家属代办操作必须校验授权关系，并记录代理人 ID。
6. 管理员删除事项、禁用用户、变更权限等关键操作建议扩展审计日志。
7. 重要业务表不建议物理删除，优先使用 `deleted` 或状态字段。

## 11. 与需求/UML 命名映射

| 需求/UML/PDF 命名 | 当前工程命名 | 说明 |
|---|---|---|
| `GovService` / `service_config` / `gov_service` | `service_item` | 政务事项配置 |
| `Application` / `application` | `application_form` | 事项申请单 |
| `Material` / `material` | `application_material` | 申请材料文件 |
| `FamilyBinding` / `family_binding` | `proxy_relation` | 家属代理关系 |
| `Notification` / `notification_message` | `notice` | 通知消息 |
| `ServiceBooking` | `service_booking` | 社区服务预约 |
| `WorkOrder` | `work_order` | 工单 |

后续代码、接口和数据库脚本应继续统一采用当前工程命名。

## 12. 后续建议

1. 继续补全家属授权校验、材料预审、工单日志写入和通知跳转的 Service 层细节。
2. 若扩展管理员配置变更，应新增审计日志表。
3. 若文件上传进入真实环境，应补充文件表访问权限、文件大小限制和文件类型白名单。
4. 每次数据库变更应同步维护 `init.sql`、`schema.md`、`migration.md`、接口文档和实体类。
