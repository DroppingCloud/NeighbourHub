-- ============================================================
-- 社区服务协同平台 数据库初始化脚本
-- Version: V1.2.0
-- Database: community_service
-- Charset: utf8mb4 / utf8mb4_unicode_ci
-- 执行方式: mysql -u root -p < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS community_service
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE community_service;

-- ------------------------------------------------------------
-- 用户账号表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `user_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username`    VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
  `password`    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码哈希',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号表';

-- ------------------------------------------------------------
-- 用户角色关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_role` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`   BIGINT      NOT NULL COMMENT '用户 ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT 'ROLE_RESIDENT/ROLE_FAMILY/ROLE_STAFF/ROLE_ADMIN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ------------------------------------------------------------
-- 居民档案表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `resident_profile` (
  `profile_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       DEFAULT NULL COMMENT '关联用户 ID，可为空（支持先建档后绑定账号）',
  `real_name`     VARCHAR(50)  NOT NULL     COMMENT '真实姓名',
  `id_card`       VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
  `gender`        VARCHAR(10)  DEFAULT NULL COMMENT 'male/female/private',
  `birthday`      DATE         DEFAULT NULL COMMENT '出生日期',
  `address`       VARCHAR(200) DEFAULT NULL COMMENT '居住地址',
  `age`           INT          DEFAULT NULL COMMENT '年龄',
  `resident_type` VARCHAR(20)  DEFAULT 'local' COMMENT 'local/non_local',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='居民档案表';

-- ------------------------------------------------------------
-- 家属/代理授权关系表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_relation` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `proxy_user_id`      BIGINT       NOT NULL COMMENT '代理人（家属）用户 ID',
  `target_user_id`     BIGINT       DEFAULT NULL COMMENT '被代理居民用户 ID',
  `target_profile_id`  BIGINT       DEFAULT NULL COMMENT '被代理居民档案 ID',
  `relation`           VARCHAR(20)  DEFAULT NULL COMMENT '关系类型，如 child/spouse/volunteer',
  `authorized_actions` VARCHAR(200) DEFAULT 'apply,booking,query,notice' COMMENT '授权操作范围',
  `status`             VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'pending/active/revoked/rejected',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proxy_target_user`    (`proxy_user_id`, `target_user_id`),
  UNIQUE KEY `uk_proxy_target_profile` (`proxy_user_id`, `target_profile_id`),
  KEY `idx_proxy_user_id`    (`proxy_user_id`),
  KEY `idx_target_user_id`   (`target_user_id`),
  KEY `idx_target_profile_id`(`target_profile_id`),
  KEY `idx_status`           (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家属代理授权关系表';

-- ------------------------------------------------------------
-- 政务事项配置表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_item` (
  `item_id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `item_name`     VARCHAR(100) NOT NULL COMMENT '事项名称',
  `item_code`     VARCHAR(50)  NOT NULL COMMENT '事项编码（唯一）',
  `category`      VARCHAR(50)  NOT NULL COMMENT '分类：证件/补贴/证明/服务',
  `description`   TEXT         DEFAULT NULL COMMENT '事项说明',
  `conditions`    TEXT         DEFAULT NULL COMMENT '办理条件',
  `process_steps` TEXT         DEFAULT NULL COMMENT '办理流程 JSON 数组',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'online' COMMENT 'online/offline',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_item_code` (`item_code`),
  KEY `idx_category` (`category`),
  KEY `idx_status`   (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='政务事项配置表';

-- ------------------------------------------------------------
-- 事项材料模板表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_material_template` (
  `template_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `item_id`       BIGINT       NOT NULL COMMENT '关联事项 ID',
  `material_name` VARCHAR(100) NOT NULL COMMENT '材料名称',
  `material_type` VARCHAR(50)  DEFAULT NULL COMMENT '材料类型编码',
  `description`   VARCHAR(200) DEFAULT NULL COMMENT '材料说明',
  `sample_url`    VARCHAR(500) DEFAULT NULL COMMENT '示例或模板下载地址',
  `is_required`   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否必需: 1必需 0可选',
  `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
  PRIMARY KEY (`template_id`),
  KEY `idx_item_id` (`item_id`),
  UNIQUE KEY `uk_item_material` (`item_id`, `material_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事项材料模板表';

-- ------------------------------------------------------------
-- 事项申请单表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `application_form` (
  `application_id` BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT       NOT NULL COMMENT '申请用户 ID',
  `profile_id`     BIGINT       DEFAULT NULL COMMENT '申请对象居民档案 ID',
  `item_id`        BIGINT       NOT NULL COMMENT '事项 ID',
  `proxy_user_id`  BIGINT       DEFAULT NULL COMMENT '家属代办人用户 ID',
  `status`         VARCHAR(30)  NOT NULL DEFAULT 'pending'
                   COMMENT '状态: pending/approved/rejected/supplement_required/supplementing/completed',
  `form_data`      TEXT         DEFAULT NULL COMMENT '表单数据 JSON',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '用户备注或审核摘要',
  `submit_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`application_id`),
  KEY `idx_user_id`       (`user_id`),
  KEY `idx_profile_id`    (`profile_id`),
  KEY `idx_item_id`       (`item_id`),
  KEY `idx_proxy_user_id` (`proxy_user_id`),
  KEY `idx_status`        (`status`),
  KEY `idx_submit_time`   (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事项申请单表';

-- ------------------------------------------------------------
-- 申请材料文件元数据表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `application_material` (
  `material_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `application_id`   BIGINT       NOT NULL COMMENT '关联申请 ID',
  `template_id`      BIGINT       DEFAULT NULL COMMENT '对应材料模板 ID',
  `material_name`    VARCHAR(100) NOT NULL COMMENT '材料名称',
  `file_name`        VARCHAR(200) NOT NULL COMMENT '原始文件名',
  `file_path`        VARCHAR(500) NOT NULL COMMENT '文件存储路径或 URL',
  `file_size`        BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type`        VARCHAR(20)  DEFAULT NULL COMMENT '文件类型/扩展名',
  `ocr_text`         TEXT         DEFAULT NULL COMMENT 'OCR 识别文本',
  `precheck_status`  VARCHAR(20)  DEFAULT 'pending' COMMENT '预审状态: pending/passed/failed',
  `precheck_remark`  VARCHAR(500) DEFAULT NULL COMMENT '预审说明',
  `upload_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`material_id`),
  KEY `idx_application_id`  (`application_id`),
  KEY `idx_template_id`     (`template_id`),
  KEY `idx_precheck_status` (`precheck_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申请材料文件元数据表';

-- ------------------------------------------------------------
-- 工单表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order` (
  `order_id`       BIGINT NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT NOT NULL COMMENT '关联申请 ID',
  `staff_user_id`  BIGINT DEFAULT NULL COMMENT '处理工作人员 ID',
  `status`         VARCHAR(30) NOT NULL DEFAULT 'pending'
                   COMMENT '状态: pending/approved/rejected/supplement_required/completed',
  `audit_opinion`  TEXT        DEFAULT NULL COMMENT '审核意见',
  `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `finish_time`    DATETIME    DEFAULT NULL COMMENT '办结时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_staff_user_id` (`staff_user_id`),
  KEY `idx_status`        (`status`),
  KEY `idx_create_time`   (`create_time`),
  KEY `idx_staff_status`  (`staff_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- ------------------------------------------------------------
-- 工单操作日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_log` (
  `log_id`      BIGINT      NOT NULL AUTO_INCREMENT,
  `order_id`    BIGINT      NOT NULL COMMENT '工单 ID',
  `operator_id` BIGINT      NOT NULL COMMENT '操作人用户 ID',
  `action`      VARCHAR(50) NOT NULL COMMENT '操作动作',
  `from_status` VARCHAR(30) DEFAULT NULL COMMENT '变更前状态',
  `to_status`   VARCHAR(30) NOT NULL    COMMENT '变更后状态',
  `remark`      TEXT        DEFAULT NULL COMMENT '操作说明',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_order_id`    (`order_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单操作日志表';

-- ------------------------------------------------------------
-- 社区服务预约表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_booking` (
  `booking_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT       NOT NULL COMMENT '服务对象用户 ID',
  `profile_id`     BIGINT       DEFAULT NULL COMMENT '服务对象居民档案 ID',
  `proxy_user_id`  BIGINT       DEFAULT NULL COMMENT '家属代约人用户 ID',
  `service_type`   VARCHAR(50)  NOT NULL COMMENT '服务类型: dining/accompany/home_visit',
  `expect_time`    DATETIME     NOT NULL COMMENT '期望服务时间',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'pending'
                   COMMENT '状态: pending/confirmed/in_progress/completed/cancelled',
  `address`        VARCHAR(200) DEFAULT NULL COMMENT '服务地址',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `staff_user_id`  BIGINT       DEFAULT NULL COMMENT '执行工作人员 ID',
  `feedback`       VARCHAR(500) DEFAULT NULL COMMENT '服务反馈',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `complete_time`  DATETIME     DEFAULT NULL COMMENT '完成时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`booking_id`),
  KEY `idx_user_id`      (`user_id`),
  KEY `idx_profile_id`   (`profile_id`),
  KEY `idx_proxy_user_id`(`proxy_user_id`),
  KEY `idx_status`       (`status`),
  KEY `idx_expect_time`  (`expect_time`),
  KEY `idx_staff_status` (`staff_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区服务预约表';

-- ------------------------------------------------------------
-- 站内通知消息表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notice` (
  `notice_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '接收用户 ID',
  `title`       VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content`     TEXT         NOT NULL COMMENT '通知内容',
  `type`        VARCHAR(50)  NOT NULL COMMENT '类型: audit_result/supplement/booking/system',
  `ref_type`    VARCHAR(50)  DEFAULT NULL COMMENT '关联业务类型: application/booking/work_order/system',
  `ref_id`      BIGINT       DEFAULT NULL COMMENT '关联业务 ID',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '已读状态: 0未读 1已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_time`   DATETIME     DEFAULT NULL COMMENT '已读时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_is_read`    (`is_read`),
  KEY `idx_create_time`(`create_time`),
  KEY `idx_user_read`  (`user_id`, `is_read`),
  KEY `idx_ref`        (`ref_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知消息表';

-- ============================================================
-- 种子数据
-- 默认密码均为 123456（BCrypt 哈希）
-- ============================================================

INSERT IGNORE INTO `user`
  (`user_id`, `username`, `password`, `phone`, `status`)
VALUES
  (1, 'admin',      '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13800000000', 'active'),
  (2, 'staff01',    '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13811111111', 'active'),
  (3, 'resident01', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13822222222', 'active'),
  (4, 'family01',   '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13833333333', 'active');

INSERT IGNORE INTO `user_role` (`user_id`, `role_code`) VALUES
  (1, 'ROLE_ADMIN'),
  (2, 'ROLE_STAFF'),
  (3, 'ROLE_RESIDENT'),
  (4, 'ROLE_FAMILY');

INSERT INTO `resident_profile`
  (`user_id`, `real_name`, `id_card`, `address`, `age`, `resident_type`)
VALUES
  (1, '系统管理员', NULL,                '管理中心',          NULL, 'local'),
  (2, '张工作员',   '110101199001011234', NULL,                NULL, 'local'),
  (3, '李居民',     '110101199002021235', '幸福社区1号楼101室', 65,   'local'),
  (4, '王家属',     '110101199003031236', NULL,                NULL, 'local')
ON DUPLICATE KEY UPDATE
  `real_name` = VALUES(`real_name`),
  `address`   = VALUES(`address`);

INSERT INTO `proxy_relation`
  (`proxy_user_id`, `target_user_id`, `target_profile_id`, `relation`, `authorized_actions`, `status`)
VALUES
  (4, 3, (SELECT `profile_id` FROM `resident_profile` WHERE `user_id` = 3), 'child', 'apply,booking,query,notice', 'active')
ON DUPLICATE KEY UPDATE
  `target_profile_id`  = VALUES(`target_profile_id`),
  `relation`           = VALUES(`relation`),
  `authorized_actions` = VALUES(`authorized_actions`),
  `status`             = VALUES(`status`);

INSERT INTO `service_item`
  (`item_id`, `item_name`, `item_code`, `category`, `description`, `conditions`, `process_steps`, `status`)
VALUES
  (1, '居住证办理',   'ITEM_001', '证件', '为非本地户籍居民办理居住证。',           '非本地户籍居民，已在本社区稳定居住并能提供有效居住证明。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
  (2, '老年补贴申请', 'ITEM_002', '补贴', '60岁以上老年居民可申请社区老年生活补贴。', '社区内年满60周岁的居民，可按要求提交身份与年龄证明。',   '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
  (3, '居住证明开具', 'ITEM_003', '证明', '开具在本辖区居住的证明材料。',           '居民身份与居住地址可核验，适用于社区居住证明开具。',       '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
  (4, '便民证明',     'ITEM_004', '证明', '各类社区便民证明材料开具。',             '社区居民可按实际用途申请常见便民证明。',                   '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
  (5, '助餐服务预约', 'ITEM_005', '服务', '为老年居民提供助餐服务预约。',           '社区居民可预约助餐、陪诊、上门探访等便民服务。',           '["填写信息","上传材料","提交申请","等待审核"]', 'online')
ON DUPLICATE KEY UPDATE
  `item_name`     = VALUES(`item_name`),
  `category`      = VALUES(`category`),
  `description`   = VALUES(`description`),
  `conditions`    = VALUES(`conditions`),
  `process_steps` = VALUES(`process_steps`),
  `status`        = VALUES(`status`);

INSERT INTO `service_material_template`
  (`template_id`, `item_id`, `material_name`, `material_type`, `description`, `is_required`, `sort_order`)
VALUES
  (1, 1, '身份证明材料', 'id_card',              '身份证或其他有效身份证明。',         1, 1),
  (2, 1, '居住证明材料', 'address_proof',         '租房合同、房产证明或社区居住证明。', 1, 2),
  (3, 2, '身份证明材料', 'id_card',              '申请人身份证明材料。',               1, 1),
  (4, 2, '年龄证明材料', 'age_proof',             '可证明年龄条件的材料。',             1, 2),
  (5, 3, '身份证明材料', 'id_card',              '居民身份证明材料。',                 1, 1),
  (6, 4, '申请说明材料', 'application_note',      '证明用途或相关说明。',               1, 1),
  (7, 5, '服务预约信息', 'service_booking_info',  '服务时间、地址和联系方式。',         1, 1)
ON DUPLICATE KEY UPDATE
  `material_name` = VALUES(`material_name`),
  `description`   = VALUES(`description`),
  `is_required`   = VALUES(`is_required`),
  `sort_order`    = VALUES(`sort_order`);
