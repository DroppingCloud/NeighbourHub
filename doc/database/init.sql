-- ============================================================
-- 社区服务协同平台 初始化数据库脚本
-- Version: V1.0.0
-- Date: 2026-05-25
-- ============================================================

CREATE DATABASE IF NOT EXISTS community_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE community_service;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `user_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    VARCHAR(50)   NOT NULL                COMMENT '用户名',
  `password`    VARCHAR(100)  NOT NULL                COMMENT '密码（BCrypt加密）',
  `phone`       VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
  `email`       VARCHAR(100)  DEFAULT NULL            COMMENT '邮箱',
  `status`      VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT '状态: active/disabled',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`   BIGINT      NOT NULL COMMENT '用户ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色码: ROLE_RESIDENT/ROLE_FAMILY/ROLE_STAFF/ROLE_ADMIN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 居民档案表
CREATE TABLE IF NOT EXISTS `resident_profile` (
  `profile_id`    BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT      NOT NULL COMMENT '关联用户ID',
  `real_name`     VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `id_card`       VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `address`       VARCHAR(200) DEFAULT NULL COMMENT '居住地址',
  `age`           INT         DEFAULT NULL COMMENT '年龄',
  `resident_type` VARCHAR(20) DEFAULT 'local' COMMENT '户籍类型: local/non_local',
  `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='居民档案表';

-- 代理关系表
CREATE TABLE IF NOT EXISTS `proxy_relation` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT,
  `proxy_user_id` BIGINT   NOT NULL COMMENT '代理人用户ID（家属）',
  `target_user_id` BIGINT  NOT NULL COMMENT '被代理人用户ID（居民）',
  `status`        VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/revoked',
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proxy_target` (`proxy_user_id`, `target_user_id`),
  KEY `idx_proxy_user_id` (`proxy_user_id`),
  KEY `idx_target_user_id` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理关系表';

-- 政务事项表
CREATE TABLE IF NOT EXISTS `service_item` (
  `item_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `item_name`   VARCHAR(100) NOT NULL COMMENT '事项名称',
  `item_code`   VARCHAR(50)  NOT NULL COMMENT '事项编码',
  `category`    VARCHAR(50)  NOT NULL COMMENT '分类: 证明/补贴/证件/服务',
  `description` TEXT         DEFAULT NULL COMMENT '说明',
  `conditions`  TEXT         DEFAULT NULL COMMENT '办理条件',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'online' COMMENT '状态: online/offline',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_item_code` (`item_code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政务事项表';

-- 事项材料模板表
CREATE TABLE IF NOT EXISTS `service_material_template` (
  `template_id`   BIGINT      NOT NULL AUTO_INCREMENT,
  `item_id`       BIGINT      NOT NULL COMMENT '关联事项ID',
  `material_name` VARCHAR(100) NOT NULL COMMENT '材料名称',
  `description`   VARCHAR(200) DEFAULT NULL COMMENT '说明',
  `is_required`   TINYINT     NOT NULL DEFAULT 1 COMMENT '是否必须: 1必须 0可选',
  PRIMARY KEY (`template_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事项材料模板表';

-- 申请单表
CREATE TABLE IF NOT EXISTS `application_form` (
  `application_id` BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT      NOT NULL COMMENT '申请用户ID',
  `item_id`        BIGINT      NOT NULL COMMENT '事项ID',
  `proxy_user_id`  BIGINT      DEFAULT NULL COMMENT '代理人ID（家属代办时填写）',
  `status`         VARCHAR(30) NOT NULL DEFAULT 'pending'
                   COMMENT '状态: pending/approved/rejected/supplement_required/supplementing/completed',
  `form_data`      TEXT        DEFAULT NULL COMMENT '表单数据（JSON）',
  `remark`         VARCHAR(500) DEFAULT NULL,
  `submit_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`application_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_status` (`status`),
  KEY `idx_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请单表';

-- 申请材料文件表
CREATE TABLE IF NOT EXISTS `application_material` (
  `material_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT       NOT NULL COMMENT '关联申请ID',
  `material_name`  VARCHAR(100) NOT NULL COMMENT '材料名称',
  `file_name`      VARCHAR(200) NOT NULL COMMENT '文件原名',
  `file_path`      VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  `file_size`      BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type`      VARCHAR(20)  DEFAULT NULL COMMENT '文件类型',
  `precheck_status` VARCHAR(20) DEFAULT 'pending' COMMENT '预审状态: pending/passed/failed',
  `precheck_remark` VARCHAR(500) DEFAULT NULL,
  `upload_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`material_id`),
  KEY `idx_application_id` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请材料文件表';

-- 工单表
CREATE TABLE IF NOT EXISTS `work_order` (
  `order_id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT      NOT NULL COMMENT '关联申请ID',
  `staff_user_id`  BIGINT      DEFAULT NULL COMMENT '处理工作人员ID',
  `status`         VARCHAR(30) NOT NULL DEFAULT 'pending'
                   COMMENT '状态: pending/approved/rejected/supplement_required/completed',
  `audit_opinion`  TEXT        DEFAULT NULL COMMENT '审核意见',
  `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_staff_user_id` (`staff_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 工单操作日志表
CREATE TABLE IF NOT EXISTS `work_order_log` (
  `log_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `order_id`   BIGINT       NOT NULL COMMENT '工单ID',
  `operator_id` BIGINT      NOT NULL COMMENT '操作人ID',
  `action`     VARCHAR(50)  NOT NULL COMMENT '操作动作',
  `from_status` VARCHAR(30) DEFAULT NULL COMMENT '变更前状态',
  `to_status`  VARCHAR(30)  NOT NULL COMMENT '变更后状态',
  `remark`     TEXT         DEFAULT NULL COMMENT '操作说明',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单操作日志表';

-- 社区服务预约表
CREATE TABLE IF NOT EXISTS `service_booking` (
  `booking_id`  BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '预约用户ID',
  `service_type` VARCHAR(50) NOT NULL COMMENT '服务类型: dining/accompany/home_visit',
  `expect_time` DATETIME     NOT NULL COMMENT '期望服务时间',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'pending'
                COMMENT '状态: pending/confirmed/in_progress/completed/cancelled',
  `address`     VARCHAR(200) DEFAULT NULL COMMENT '服务地址',
  `remark`      VARCHAR(500) DEFAULT NULL,
  `staff_user_id` BIGINT     DEFAULT NULL COMMENT '执行工作人员ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`booking_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expect_time` (`expect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区服务预约表';

-- 通知消息表
CREATE TABLE IF NOT EXISTS `notice` (
  `notice_id`  BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL COMMENT '接收用户ID',
  `title`      VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content`    TEXT         NOT NULL COMMENT '通知内容',
  `type`       VARCHAR(50)  NOT NULL COMMENT '类型: audit_result/supplement/booking/system',
  `ref_id`     BIGINT       DEFAULT NULL COMMENT '关联业务ID',
  `is_read`    TINYINT      NOT NULL DEFAULT 0 COMMENT '已读状态: 0未读 1已读',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notice_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 初始用户数据
-- 默认密码：123456
INSERT IGNORE INTO `user`
(`user_id`, `username`, `password`, `phone`, `email`, `status`, `create_time`, `update_time`, `deleted`)
VALUES
(1, 'admin',      '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13800000000', NULL, 'active', '2026-05-26 01:17:27', '2026-05-26 03:52:47', 0),
(2, 'staff01',    '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13811111111', NULL, 'active', '2026-05-26 02:16:20', '2026-05-26 03:52:47', 0),
(3, 'resident01', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13822222222', NULL, 'active', '2026-05-26 02:16:20', '2026-05-26 03:52:47', 0),
(4, 'family01',   '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13833333333', NULL, 'active', '2026-05-26 02:16:20', '2026-05-26 03:52:47', 0);

-- 用户角色数据
INSERT IGNORE INTO `user_role` (`user_id`, `role_code`) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_STAFF'),
(3, 'ROLE_RESIDENT'),
(4, 'ROLE_FAMILY');

-- 居民档案数据
INSERT IGNORE INTO `resident_profile`
(`user_id`, `real_name`, `id_card`, `address`, `age`, `resident_type`)
VALUES
(1, '系统管理员', NULL, NULL, NULL, 'local'),
(3, '居民用户', NULL, '幸福社区1号楼101室', 65, 'local');

-- 家属代理关系数据
INSERT IGNORE INTO `proxy_relation`
(`proxy_user_id`, `target_user_id`, `status`)
VALUES
(4, 3, 'active');

-- 示例事项数据
INSERT IGNORE INTO `service_item` (`item_id`, `item_name`, `item_code`, `category`, `description`, `status`) VALUES
(1, '居住证办理', 'ITEM_001', '证件', '为非本地户籍居民办理居住证', 'online'),
(2, '老年补贴申请', 'ITEM_002', '补贴', '60岁以上老年居民可申请社区老年生活补贴', 'online'),
(3, '居住证明开具', 'ITEM_003', '证明', '开具在本辖区居住的证明材料', 'online'),
(4, '便民证明', 'ITEM_004', '证明', '各类社区便民证明材料开具', 'online'),
(5, '助餐服务预约', 'ITEM_005', '服务', '为老年居民提供助餐服务预约', 'online');
