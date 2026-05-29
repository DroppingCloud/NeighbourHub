# 数据库变更与运行验证记录

## V1.1.0 数据库完整化

日期：2026-05-28

本次变更根据 `database-design.md` 对后端数据库落地进行补齐，目标是让 `init.sql`、`schema.md`、后端实体结构和接口基础保持一致。

## 文档分工

数据库目录保留以下 4 个核心文件：

| 文件 | 作用 |
|---|---|
| `database-design.md` | 数据库总体设计、表关系、状态流转和设计原则 |
| `schema.md` | 逐表字段、索引、关联关系和相关接口说明 |
| `init.sql` | 新环境初始化建库建表和基础种子数据 |
| `migration.md` | 旧库升级说明、变更记录和运行验证结果 |

原 `runtime-verification.md` 的内容已合并到本文档，避免数据库文档分散和重复维护。

## 结构调整

| 表 | 变更 |
|---|---|
| `resident_profile` | `user_id` 调整为可空，新增 `gender`、`birthday`，支持未注册居民先建档 |
| `proxy_relation` | 新增 `target_profile_id`、`relation`、`authorized_actions`、`update_time`，补充授权范围表达 |
| `service_item` | 新增 `process_steps`，用于事项办理流程引导展示 |
| `service_material_template` | 新增 `material_type`、`sample_url`、`sort_order` |
| `application_form` | 新增 `profile_id`，让申请对象可关联居民档案 |
| `application_material` | 新增 `template_id`、`ocr_text` |
| `work_order` | 新增 `finish_time`，支持办结统计 |
| `work_order_log` | 新增 `idx_operator_id`、`idx_create_time` |
| `service_booking` | 新增 `profile_id`、`proxy_user_id`、`feedback`、`complete_time`、`idx_staff_status` |
| `notice` | 新增 `ref_type`、`read_time`、`idx_user_read`、`idx_ref` |

## 后端实体补齐

新增或补齐的实体与 Mapper：

| 实体 | Mapper |
|---|---|
| `ProxyRelation` | `ProxyRelationMapper` |
| `ServiceMaterialTemplate` | `ServiceMaterialTemplateMapper` |
| `ApplicationMaterial` | `ApplicationMaterialMapper` |
| `WorkOrderLog` | `WorkOrderLogMapper` |

同步扩展已有实体：`ResidentProfile`、`ServiceItem`、`ApplicationForm`、`ServiceBooking`、`WorkOrder`、`Notice`。

## 运行验证记录

验证环境：

| 项 | 值 |
|---|---|
| MySQL 容器 | `community_mysql` |
| Redis 容器 | `community_redis` |
| 数据库 | `community_service` |
| 应用数据库用户 | `appuser` |
| MySQL 端口 | `localhost:3306` |
| 后端联调端口 | `18080` |

已确认数据库包含以下核心表：

- `user`
- `user_role`
- `resident_profile`
- `service_item`
- `service_material_template`
- `application_form`
- `application_material`
- `work_order`
- `work_order_log`
- `service_booking`
- `notice`
- `proxy_relation`

已通过真实 HTTP 接口验证的核心流程：

| 流程 | 接口 |
|---|---|
| 注册 | `POST /api/auth/register` |
| 登录 | `POST /api/auth/login` |
| 提交申请 | `POST /api/application/submit` |
| 上传材料 | `POST /api/application/{id}/materials` |
| 创建预约 | `POST /api/booking/create` |
| 审核工单 | `POST /api/workorder/audit` |

运行库曾存在历史中文种子数据和表注释乱码问题，已通过统一 `utf8mb4` 连接参数、修复种子数据和重建干净 `init.sql` 处理。后续新环境应优先使用 `init.sql` 初始化；旧 Docker volume 升级时按本文的升级 SQL 选择性执行。

查看运行库时建议使用：

```bash
docker exec community_mysql mysql --default-character-set=utf8mb4 -uappuser -papp_password_123 community_service
```

## 旧库升级参考 SQL

以下语句用于从旧版演示库升级。若使用新版 `init.sql` 新建数据库，则无需执行。

执行前请先备份数据库；如果某些字段或索引已经存在，应按实际数据库状态跳过对应语句。

```sql
ALTER TABLE resident_profile
  MODIFY COLUMN user_id BIGINT NULL COMMENT '关联用户ID，可为空',
  ADD COLUMN gender VARCHAR(10) NULL COMMENT '性别：male/female/unknown' AFTER id_card,
  ADD COLUMN birthday DATE NULL COMMENT '出生日期' AFTER gender;

ALTER TABLE proxy_relation
  ADD COLUMN target_profile_id BIGINT NULL COMMENT '被代理居民档案ID' AFTER target_user_id,
  ADD COLUMN relation VARCHAR(20) NULL COMMENT '亲属关系' AFTER target_profile_id,
  ADD COLUMN authorized_actions VARCHAR(200) DEFAULT 'apply,booking,query,notice' COMMENT '授权操作范围' AFTER relation,
  ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
  ADD KEY idx_target_profile_id (target_profile_id),
  ADD KEY idx_status (status);

ALTER TABLE service_item
  ADD COLUMN process_steps TEXT NULL COMMENT '办理流程步骤JSON' AFTER conditions;

ALTER TABLE service_material_template
  ADD COLUMN material_type VARCHAR(50) NULL COMMENT '材料类型编码' AFTER material_name,
  ADD COLUMN sample_url VARCHAR(500) NULL COMMENT '样例或模板地址' AFTER description,
  ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '展示顺序' AFTER is_required,
  ADD UNIQUE KEY uk_item_material (item_id, material_type);

ALTER TABLE application_form
  ADD COLUMN profile_id BIGINT NULL COMMENT '申请对象居民档案ID' AFTER user_id,
  ADD KEY idx_profile_id (profile_id),
  ADD KEY idx_proxy_user_id (proxy_user_id);

ALTER TABLE application_material
  ADD COLUMN template_id BIGINT NULL COMMENT '材料模板ID' AFTER application_id,
  ADD COLUMN ocr_text TEXT NULL COMMENT 'OCR识别文本' AFTER file_type,
  ADD KEY idx_template_id (template_id),
  ADD KEY idx_precheck_status (precheck_status);

ALTER TABLE work_order
  ADD COLUMN finish_time DATETIME NULL COMMENT '办结时间' AFTER update_time,
  ADD KEY idx_staff_status (staff_user_id, status);

ALTER TABLE work_order_log
  ADD KEY idx_operator_id (operator_id),
  ADD KEY idx_create_time (create_time);

ALTER TABLE service_booking
  ADD COLUMN profile_id BIGINT NULL COMMENT '服务对象居民档案ID' AFTER user_id,
  ADD COLUMN proxy_user_id BIGINT NULL COMMENT '代约用户ID' AFTER profile_id,
  ADD COLUMN feedback VARCHAR(500) NULL COMMENT '服务反馈' AFTER staff_user_id,
  ADD COLUMN complete_time DATETIME NULL COMMENT '完成时间' AFTER update_time,
  ADD KEY idx_profile_id (profile_id),
  ADD KEY idx_proxy_user_id (proxy_user_id),
  ADD KEY idx_staff_status (staff_user_id, status);

ALTER TABLE notice
  ADD COLUMN ref_type VARCHAR(50) NULL COMMENT '关联业务类型' AFTER type,
  ADD COLUMN read_time DATETIME NULL COMMENT '阅读时间' AFTER create_time,
  ADD KEY idx_user_read (user_id, is_read),
  ADD KEY idx_ref (ref_type, ref_id);
```
