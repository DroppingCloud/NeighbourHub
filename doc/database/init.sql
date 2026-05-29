-- ============================================================
-- Community service platform database initialization script
-- Version: V1.1.0
-- Database: community_service
-- Charset: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS community_service
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE community_service;

CREATE TABLE IF NOT EXISTS `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User ID',
  `username` VARCHAR(50) NOT NULL COMMENT 'Login username',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt password hash',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone number',
  `email` VARCHAR(100) DEFAULT NULL COMMENT 'Email',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal, 1 deleted',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User account';

CREATE TABLE IF NOT EXISTS `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'User ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT 'ROLE_RESIDENT/ROLE_FAMILY/ROLE_STAFF/ROLE_ADMIN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-role relation';

CREATE TABLE IF NOT EXISTS `resident_profile` (
  `profile_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL COMMENT 'Linked user ID, nullable before account binding',
  `real_name` VARCHAR(50) NOT NULL COMMENT 'Real name',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT 'Identity card number',
  `address` VARCHAR(200) DEFAULT NULL COMMENT 'Residential address',
  `age` INT DEFAULT NULL COMMENT 'Age',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT 'male/female/private',
  `birthday` DATE DEFAULT NULL COMMENT 'Birthday',
  `resident_type` VARCHAR(20) DEFAULT 'local' COMMENT 'local/non_local',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Resident profile';

CREATE TABLE IF NOT EXISTS `proxy_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `proxy_user_id` BIGINT NOT NULL COMMENT 'Proxy/family user ID',
  `target_user_id` BIGINT DEFAULT NULL COMMENT 'Target resident user ID',
  `target_profile_id` BIGINT DEFAULT NULL COMMENT 'Target resident profile ID',
  `relation` VARCHAR(20) DEFAULT NULL COMMENT 'Relation type, such as child/spouse/volunteer',
  `authorized_actions` VARCHAR(200) DEFAULT 'apply,booking,query,notice' COMMENT 'Authorized action codes',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'pending/active/revoked/rejected',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proxy_target_user` (`proxy_user_id`, `target_user_id`),
  UNIQUE KEY `uk_proxy_target_profile` (`proxy_user_id`, `target_profile_id`),
  KEY `idx_proxy_user_id` (`proxy_user_id`),
  KEY `idx_target_user_id` (`target_user_id`),
  KEY `idx_target_profile_id` (`target_profile_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Family proxy authorization';

CREATE TABLE IF NOT EXISTS `service_item` (
  `item_id` BIGINT NOT NULL AUTO_INCREMENT,
  `item_name` VARCHAR(100) NOT NULL COMMENT 'Service item name',
  `item_code` VARCHAR(50) NOT NULL COMMENT 'Unique service item code',
  `category` VARCHAR(50) NOT NULL COMMENT 'certificate/subsidy/license/service',
  `description` TEXT DEFAULT NULL COMMENT 'Description',
  `conditions` TEXT DEFAULT NULL COMMENT 'Handling conditions or rule JSON',
  `process_steps` TEXT DEFAULT NULL COMMENT 'Process steps JSON',
  `status` VARCHAR(20) NOT NULL DEFAULT 'online' COMMENT 'online/offline',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal, 1 deleted',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_item_code` (`item_code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Government service item';

CREATE TABLE IF NOT EXISTS `service_material_template` (
  `template_id` BIGINT NOT NULL AUTO_INCREMENT,
  `item_id` BIGINT NOT NULL COMMENT 'Service item ID',
  `material_name` VARCHAR(100) NOT NULL COMMENT 'Material name',
  `material_type` VARCHAR(50) DEFAULT NULL COMMENT 'Material type code',
  `description` VARCHAR(200) DEFAULT NULL COMMENT 'Material description',
  `sample_url` VARCHAR(500) DEFAULT NULL COMMENT 'Sample or template URL',
  `is_required` TINYINT NOT NULL DEFAULT 1 COMMENT '1 required, 0 optional',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Display order',
  PRIMARY KEY (`template_id`),
  KEY `idx_item_id` (`item_id`),
  UNIQUE KEY `uk_item_material` (`item_id`, `material_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Service material template';

CREATE TABLE IF NOT EXISTS `application_form` (
  `application_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'Target user ID',
  `profile_id` BIGINT DEFAULT NULL COMMENT 'Target resident profile ID',
  `item_id` BIGINT NOT NULL COMMENT 'Service item ID',
  `proxy_user_id` BIGINT DEFAULT NULL COMMENT 'Proxy user ID when submitted by family',
  `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT 'draft/pending/prechecking/supplement_required/supplementing/approved/rejected/completed/cancelled',
  `form_data` TEXT DEFAULT NULL COMMENT 'Form JSON',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT 'User remark or audit summary',
  `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal, 1 deleted',
  PRIMARY KEY (`application_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_profile_id` (`profile_id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_proxy_user_id` (`proxy_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Application form';

CREATE TABLE IF NOT EXISTS `application_material` (
  `material_id` BIGINT NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT NOT NULL COMMENT 'Application ID',
  `template_id` BIGINT DEFAULT NULL COMMENT 'Material template ID',
  `material_name` VARCHAR(100) NOT NULL COMMENT 'Material name',
  `file_name` VARCHAR(200) NOT NULL COMMENT 'Original file name',
  `file_path` VARCHAR(500) NOT NULL COMMENT 'File path or URL',
  `file_size` BIGINT DEFAULT NULL COMMENT 'File size in bytes',
  `file_type` VARCHAR(20) DEFAULT NULL COMMENT 'File extension or MIME type',
  `ocr_text` TEXT DEFAULT NULL COMMENT 'OCR recognized text',
  `precheck_status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/passed/failed',
  `precheck_remark` VARCHAR(500) DEFAULT NULL COMMENT 'Precheck remark',
  `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`material_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_precheck_status` (`precheck_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Application material metadata';

CREATE TABLE IF NOT EXISTS `work_order` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT NOT NULL COMMENT 'Application ID',
  `staff_user_id` BIGINT DEFAULT NULL COMMENT 'Assigned staff user ID',
  `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/approved/supplement_required/rejected/completed',
  `audit_opinion` TEXT DEFAULT NULL COMMENT 'Audit opinion',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `finish_time` DATETIME DEFAULT NULL COMMENT 'Completion time',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_staff_user_id` (`staff_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_staff_status` (`staff_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Work order';

CREATE TABLE IF NOT EXISTS `work_order_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT 'Work order ID',
  `operator_id` BIGINT NOT NULL COMMENT 'Operator user ID',
  `action` VARCHAR(50) NOT NULL COMMENT 'create/assign/start/approve/reject/require_supplement/complete',
  `from_status` VARCHAR(30) DEFAULT NULL COMMENT 'Previous status',
  `to_status` VARCHAR(30) NOT NULL COMMENT 'Next status',
  `remark` TEXT DEFAULT NULL COMMENT 'Operation remark',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Work order operation log';

CREATE TABLE IF NOT EXISTS `service_booking` (
  `booking_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'Target user ID',
  `profile_id` BIGINT DEFAULT NULL COMMENT 'Target resident profile ID',
  `proxy_user_id` BIGINT DEFAULT NULL COMMENT 'Proxy user ID when booked by family',
  `service_type` VARCHAR(50) NOT NULL COMMENT 'dining/accompany/home_visit',
  `expect_time` DATETIME NOT NULL COMMENT 'Expected service time',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/confirmed/in_progress/completed/cancelled',
  `address` VARCHAR(200) DEFAULT NULL COMMENT 'Service address',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  `staff_user_id` BIGINT DEFAULT NULL COMMENT 'Assigned staff user ID',
  `feedback` VARCHAR(500) DEFAULT NULL COMMENT 'Service feedback',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `complete_time` DATETIME DEFAULT NULL COMMENT 'Completion time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal, 1 deleted',
  PRIMARY KEY (`booking_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_profile_id` (`profile_id`),
  KEY `idx_proxy_user_id` (`proxy_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expect_time` (`expect_time`),
  KEY `idx_staff_status` (`staff_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Community service booking';

CREATE TABLE IF NOT EXISTS `notice` (
  `notice_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'Receiver user ID',
  `title` VARCHAR(200) NOT NULL COMMENT 'Notice title',
  `content` TEXT NOT NULL COMMENT 'Notice content',
  `type` VARCHAR(50) NOT NULL COMMENT 'audit_result/supplement/booking/progress/system',
  `ref_type` VARCHAR(50) DEFAULT NULL COMMENT 'application/booking/work_order/system',
  `ref_id` BIGINT DEFAULT NULL COMMENT 'Related business ID',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0 unread, 1 read',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_time` DATETIME DEFAULT NULL COMMENT 'Read time',
  PRIMARY KEY (`notice_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_ref` (`ref_type`, `ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notice message';

-- ============================================================
-- Seed data
-- Default password for seed users: 123456
-- ============================================================

INSERT IGNORE INTO `user`
(`user_id`, `username`, `password`, `phone`, `email`, `status`, `create_time`, `update_time`, `deleted`)
VALUES
(1, 'admin',      '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13800000000', NULL, 'active', NOW(), NOW(), 0),
(2, 'staff01',    '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13811111111', NULL, 'active', NOW(), NOW(), 0),
(3, 'resident01', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13822222222', NULL, 'active', NOW(), NOW(), 0),
(4, 'family01',   '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13833333333', NULL, 'active', NOW(), NOW(), 0);

INSERT IGNORE INTO `user_role` (`user_id`, `role_code`) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_STAFF'),
(3, 'ROLE_RESIDENT'),
(4, 'ROLE_FAMILY');

INSERT INTO `resident_profile`
(`user_id`, `real_name`, `id_card`, `address`, `age`, `gender`, `birthday`, `resident_type`)
VALUES
(3, '居民用户', NULL, '幸福社区1号楼101室', 65, 'private', NULL, 'local')
ON DUPLICATE KEY UPDATE
  `real_name` = VALUES(`real_name`),
  `address` = VALUES(`address`),
  `age` = VALUES(`age`),
  `gender` = VALUES(`gender`),
  `resident_type` = VALUES(`resident_type`);

INSERT INTO `proxy_relation`
(`proxy_user_id`, `target_user_id`, `target_profile_id`, `relation`, `authorized_actions`, `status`)
VALUES
(4, 3, (SELECT `profile_id` FROM `resident_profile` WHERE `user_id` = 3), 'child', 'apply,booking,query,notice', 'active')
ON DUPLICATE KEY UPDATE
  `target_profile_id` = VALUES(`target_profile_id`),
  `relation` = VALUES(`relation`),
  `authorized_actions` = VALUES(`authorized_actions`),
  `status` = VALUES(`status`);

INSERT INTO `service_item`
(`item_id`, `item_name`, `item_code`, `category`, `description`, `conditions`, `process_steps`, `status`)
VALUES
(1, '居住证办理', 'ITEM_001', '证件', '为非本地户籍居民办理居住证。', '非本地户籍居民，已在本社区稳定居住并能提供有效居住证明。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(2, '老年补贴申请', 'ITEM_002', '补贴', '60岁以上老年居民可申请社区老年生活补贴。', '社区内年满60周岁的居民，可按要求提交身份与年龄证明。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(3, '居住证明开具', 'ITEM_003', '证明', '开具在本辖区居住的证明材料。', '居民身份与居住地址可核验，适用于社区居住证明开具。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(4, '便民证明', 'ITEM_004', '证明', '各类社区便民证明材料开具。', '社区居民可按实际用途申请常见便民证明。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(5, '助餐服务预约', 'ITEM_005', '服务', '为老年居民提供助餐服务预约。', '社区居民可预约助餐、陪诊、上门探访等便民服务。', '["填写信息","上传材料","提交申请","等待审核"]', 'online')
ON DUPLICATE KEY UPDATE
  `item_name` = VALUES(`item_name`),
  `category` = VALUES(`category`),
  `description` = VALUES(`description`),
  `conditions` = VALUES(`conditions`),
  `process_steps` = VALUES(`process_steps`),
  `status` = VALUES(`status`);

INSERT INTO `service_material_template`
(`template_id`, `item_id`, `material_name`, `material_type`, `description`, `is_required`, `sort_order`)
VALUES
(1, 1, '身份证明材料', 'id_card', '身份证或其他有效身份证明。', 1, 1),
(2, 1, '居住证明材料', 'address_proof', '租房合同、房产证明或社区居住证明。', 1, 2),
(3, 2, '身份证明材料', 'id_card', '申请人身份证明材料。', 1, 1),
(4, 2, '年龄证明材料', 'age_proof', '可证明年龄条件的材料。', 1, 2),
(5, 3, '身份证明材料', 'id_card', '居民身份证明材料。', 1, 1),
(6, 4, '申请说明材料', 'application_note', '证明用途或相关说明。', 1, 1),
(7, 5, '服务预约信息', 'service_booking_info', '服务时间、地址和联系方式。', 1, 1)
ON DUPLICATE KEY UPDATE
  `material_name` = VALUES(`material_name`),
  `description` = VALUES(`description`),
  `is_required` = VALUES(`is_required`),
  `sort_order` = VALUES(`sort_order`);
