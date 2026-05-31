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

SET NAMES utf8mb4;

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
  `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/approved/supplement_required/rejected/completed/cancelled',
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

/*
Legacy seed block disabled: the material model below is outdated and contains
mojibake text. Keep it commented for history; the clean UTF-8 seed block after
this comment is aligned with 材料模板示例.md.

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

INSERT INTO `service_material_template`
(`template_id`, `item_id`, `material_name`, `material_type`, `description`, `is_required`, `sort_order`)
VALUES
(101, 1, '居住证申请表', 'residence_permit_application', '请下载模板填写个人基本信息、居住信息并签字，可上传 DOC/DOCX/PDF。', 1, 3),
(102, 1, '家属代办授权书', 'family_proxy_authorization', '委托家属代办居住证事项时提交，需授权人和被授权人签字。', 0, 4),
(103, 2, '老年补贴申请表', 'elderly_subsidy_application', '请下载模板填写申请人、紧急联系人和补贴发放账户信息并签字。', 1, 3),
(104, 2, '银行卡信息确认书', 'bank_card_confirmation', '用于确认补贴发放银行账户，需本人签字确认。', 1, 4),
(105, 2, '家属代办授权书', 'family_proxy_authorization', '由家属代理申请老年补贴时提交，需双方签字。', 0, 5),
(106, 3, '社区居住证明', 'residence_proof_template', '社区居住证明标准模板，可下载填写后由社区盖章。', 1, 2),
(107, 4, '亲属关系证明', 'family_relationship_proof', '用于证明亲属关系的社区证明模板。', 0, 2),
(108, 4, '个人收入证明', 'income_proof', '用于证明居民收入情况的社区证明模板。', 0, 3),
(109, 5, '社区服务申请表', 'community_service_application', '用于助餐、陪诊、上门探访等社区服务申请。', 1, 2),
(110, 5, '服务需求说明', 'service_demand_note', '用于填写服务时间、地点、特殊需求和紧急联系人。', 0, 3),
(111, 5, '家属代办授权书', 'family_proxy_authorization', '家属代约社区服务时提交，需双方签字。', 0, 4)
ON DUPLICATE KEY UPDATE
  `material_name` = VALUES(`material_name`),
  `description` = VALUES(`description`),
  `is_required` = VALUES(`is_required`),
  `sort_order` = VALUES(`sort_order`);

-- Stable integration samples for application/material/work-order flows.
-- These rows are intentionally small: pending, approved and supplement-required.
INSERT INTO `application_form`
(`application_id`, `user_id`, `profile_id`, `item_id`, `proxy_user_id`, `status`, `form_data`, `remark`, `submit_time`, `update_time`, `deleted`)
VALUES
(1001, 3, (SELECT `profile_id` FROM `resident_profile` WHERE `user_id` = 3), 3, NULL, 'pending',
 '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
 '居住证明开具申请', NOW(), NOW(), 0),
(1002, 3, (SELECT `profile_id` FROM `resident_profile` WHERE `user_id` = 3), 2, NULL, 'approved',
 '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
 '老年补贴申请', NOW(), NOW(), 0),
(1003, 3, (SELECT `profile_id` FROM `resident_profile` WHERE `user_id` = 3), 1, NULL, 'supplement_required',
 '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
 '居住证办理申请，需补充居住证明材料', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `status` = VALUES(`status`),
  `form_data` = VALUES(`form_data`),
  `remark` = VALUES(`remark`),
  `update_time` = VALUES(`update_time`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `application_material`
(`material_id`, `application_id`, `template_id`, `material_name`, `file_name`, `file_path`, `file_size`, `file_type`, `ocr_text`, `precheck_status`, `precheck_remark`, `upload_time`)
VALUES
(1001, 1001, 5, '身份证明材料', 'sample-id-card.pdf', '/mock/materials/sample-id-card.pdf', 245760, 'pdf', NULL, 'passed', '规则预审通过', NOW()),
(1002, 1001, 106, '社区居住证明', 'sample-residence-proof.doc', '/mock/materials/sample-residence-proof.doc', 56320, 'doc', NULL, 'passed', '规则预审通过', NOW()),
(1003, 1002, 3, '身份证明材料', 'sample-elder-id.pdf', '/mock/materials/sample-elder-id.pdf', 245760, 'pdf', NULL, 'passed', '规则预审通过', NOW()),
(1004, 1002, 4, '年龄证明材料', 'sample-age-proof.pdf', '/mock/materials/sample-age-proof.pdf', 196608, 'pdf', NULL, 'passed', '规则预审通过', NOW()),
(1005, 1002, 103, '老年补贴申请表', 'sample-elderly-subsidy.doc', '/mock/materials/sample-elderly-subsidy.doc', 65536, 'doc', NULL, 'passed', '规则预审通过', NOW()),
(1006, 1002, 104, '银行卡信息确认书', 'sample-bank-card.doc', '/mock/materials/sample-bank-card.doc', 49152, 'doc', NULL, 'passed', '规则预审通过', NOW()),
(1007, 1003, 1, '身份证明材料', 'sample-permit-id.pdf', '/mock/materials/sample-permit-id.pdf', 245760, 'pdf', NULL, 'passed', '规则预审通过', NOW()),
(1008, 1003, 101, '居住证申请表', 'sample-permit-application.doc', '/mock/materials/sample-permit-application.doc', 65536, 'doc', NULL, 'passed', '规则预审通过', NOW())
ON DUPLICATE KEY UPDATE
  `template_id` = VALUES(`template_id`),
  `material_name` = VALUES(`material_name`),
  `file_name` = VALUES(`file_name`),
  `file_path` = VALUES(`file_path`),
  `file_size` = VALUES(`file_size`),
  `file_type` = VALUES(`file_type`),
  `precheck_status` = VALUES(`precheck_status`),
  `precheck_remark` = VALUES(`precheck_remark`);

INSERT INTO `work_order`
(`order_id`, `application_id`, `staff_user_id`, `status`, `audit_opinion`, `create_time`, `update_time`, `finish_time`)
VALUES
(1001, 1001, NULL, 'pending', NULL, NOW(), NOW(), NULL),
(1002, 1002, 2, 'approved', '材料完整，审核通过。', NOW(), NOW(), NULL),
(1003, 1003, 2, 'supplement_required', '缺少居住证明材料，请补充后重新提交。', NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE
  `staff_user_id` = VALUES(`staff_user_id`),
  `status` = VALUES(`status`),
  `audit_opinion` = VALUES(`audit_opinion`),
  `update_time` = VALUES(`update_time`),
  `finish_time` = VALUES(`finish_time`);
*/

/*
Deprecated duplicate seed block disabled. The UTF-8 correction block below now
owns service items, material templates, demo applications and work orders.

INSERT INTO resident_profile
(user_id, real_name, id_card, address, age, gender, birthday, resident_type)
VALUES
(3, '居民用户', '110101195901011234', '幸福社区1号楼101室', 65, 'private', '1959-01-01', 'local')
ON DUPLICATE KEY UPDATE
  real_name = VALUES(real_name),
  id_card = VALUES(id_card),
  address = VALUES(address),
  age = VALUES(age),
  gender = VALUES(gender),
  birthday = VALUES(birthday),
  resident_type = VALUES(resident_type);

INSERT INTO proxy_relation
(proxy_user_id, target_user_id, target_profile_id, relation, authorized_actions, status)
VALUES
(4, 3, (SELECT profile_id FROM resident_profile WHERE user_id = 3), 'child', 'apply,booking,query,notice', 'active')
ON DUPLICATE KEY UPDATE
  target_profile_id = VALUES(target_profile_id),
  relation = VALUES(relation),
  authorized_actions = VALUES(authorized_actions),
  status = VALUES(status);

INSERT INTO service_item
(item_id, item_name, item_code, category, description, conditions, process_steps, status)
VALUES
(1, '居住证办理', 'ITEM_001', '证件', '为非本地户籍居民办理居住证。', '非本地户籍居民，可按居住、就业、就读等情形提交对应证明材料。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(2, '老年补贴申请', 'ITEM_002', '补贴', '为符合条件的老年居民申请高龄津贴。', '社区内符合高龄津贴条件的老年居民，提交高龄津贴申请表和近期免冠两寸照片。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(3, '居住证明开具', 'ITEM_003', '证明', '开具在本辖区居住的证明材料。', '居民可提交居住情况证明、亲属关系证明或社区居住证明材料。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(4, '便民证明', 'ITEM_004', '证明', '开具常见社区便民证明材料。', '居民按实际用途选择无犯罪记录证明、低收入/困难证明、同一人身份证明或申请事由说明。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(5, '助餐服务预约', 'ITEM_005', '服务', '为老年居民提供助餐服务预约。', '老年居民可提交助餐服务申请表预约到店用餐或送餐到家。', '["填写信息","上传材料","提交申请","等待审核"]', 'online')
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  category = VALUES(category),
  description = VALUES(description),
  conditions = VALUES(conditions),
  process_steps = VALUES(process_steps),
  status = VALUES(status);

DELETE FROM service_material_template WHERE item_id IN (1, 2, 3, 4, 5);

INSERT INTO service_material_template
(template_id, item_id, material_name, material_type, description, is_required, sort_order)
VALUES
(100, 1, '居民身份证', 'id_card', '原件及复印件，用于核验申请人身份。', 1, 1),
(101, 1, '本人相片', 'personal_photo', '近期免冠彩色一寸照，部分地区可从人口系统提取。', 1, 2),
(102, 1, '房屋租赁合同', 'rental_contract', '合法稳定住所情形可选，上传租赁合同关键页。', 0, 3),
(103, 1, '房屋产权证明文件', 'real_estate_certificate', '合法稳定住所情形可选，上传房屋产权证明文件或不动产权证书关键信息页。', 0, 4),
(104, 1, '购房合同', 'purchase_contract', '合法稳定住所情形可选，上传购房合同关键信息页。', 0, 5),
(105, 1, '用人单位/就读学校出具的住宿证明', 'unit_accommodation_proof', '合法稳定住所情形可选，由单位或学校出具并盖章。', 0, 6),
(106, 1, '工商营业执照', 'business_license', '合法稳定就业情形可选，个体工商户或企业主提交。', 0, 7),
(107, 1, '劳动合同', 'labor_contract', '合法稳定就业情形可选，提交劳动合同关键页。', 0, 8),
(108, 1, '用人单位出具的劳动关系证明', 'labor_relation_proof', '合法稳定就业情形可选，由用人单位出具并盖章。', 0, 9),
(109, 1, '学生证', 'student_card', '连续就读情形可选，提交学生证关键信息页。', 0, 10),
(110, 1, '就读学校出具的连续就读证明', 'continuous_study_proof', '连续就读情形可选，由学校出具并盖章。', 0, 11),
(201, 2, '高龄津贴申请表', 'senior_allowance_application', '请下载模板填写申请人、紧急联系人和补贴发放账户信息并签字。', 1, 1),
(202, 2, '近期免冠两寸照片', 'recent_two_inch_photo', '近期6个月内免冠彩色两寸照，白色或淡蓝色背景。', 1, 2),
(301, 3, '居住情况证明', 'residence_situation_proof', '可上传房屋租赁合同、不动产权证书或单位住宿证明。', 1, 1),
(302, 3, '亲属关系证明', 'family_relationship_proof', '用于证明申请人与相关人员亲属关系。', 0, 2),
(303, 3, '社区居住证明', 'community_residence_proof', '请下载社区居住证明模板，填写后上传。', 1, 3),
(401, 4, '无犯罪记录证明', 'no_criminal_record_proof', '用于说明居民无犯罪记录情况。', 0, 1),
(402, 4, '低收入/困难证明', 'low_income_hardship_proof', '用于说明居民低收入或困难情况。', 0, 2),
(403, 4, '同一人身份证明', 'same_person_identity_proof', '用于证明曾用名、证件变更前后为同一人。', 0, 3),
(404, 4, '申请事由说明', 'application_reason_note', '用于说明证明用途和申请原因。', 1, 4),
(501, 5, '助餐服务申请表', 'meal_service_application', '请下载模板填写助餐服务需求、用餐频次和紧急联系人。', 1, 1)
ON DUPLICATE KEY UPDATE
  material_name = VALUES(material_name),
  description = VALUES(description),
  is_required = VALUES(is_required),
  sort_order = VALUES(sort_order);

INSERT INTO application_form
(application_id, user_id, profile_id, item_id, proxy_user_id, status, form_data, remark, submit_time, update_time, deleted)
VALUES
(1001, 3, (SELECT profile_id FROM resident_profile WHERE user_id = 3), 3, NULL, 'pending',
 '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
 '居住证明开具申请', NOW(), NOW(), 0),
(1002, 3, (SELECT profile_id FROM resident_profile WHERE user_id = 3), 2, NULL, 'approved',
 '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
 '老年补贴申请', NOW(), NOW(), 0),
(1003, 3, (SELECT profile_id FROM resident_profile WHERE user_id = 3), 1, NULL, 'supplement_required',
 '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
 '居住证办理申请，需补充居住、就业或就读证明材料', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  form_data = VALUES(form_data),
  remark = VALUES(remark),
  update_time = VALUES(update_time),
  deleted = VALUES(deleted);

INSERT INTO application_material
(material_id, application_id, template_id, material_name, file_name, file_path, file_size, file_type, ocr_text, precheck_status, precheck_remark, upload_time)
VALUES
(1001, 1001, 301, '居住情况证明', 'sample-residence-situation.pdf', '/mock/materials/sample-residence-situation.pdf', 245760, 'pdf', NULL, 'passed', '规则预审通过', NOW()),
(1002, 1001, 303, '社区居住证明', 'sample-community-residence-proof.doc', '/mock/materials/sample-community-residence-proof.doc', 56320, 'doc', NULL, 'passed', '规则预审通过', NOW()),
(1003, 1002, 201, '高龄津贴申请表', 'sample-senior-allowance.doc', '/mock/materials/sample-senior-allowance.doc', 65536, 'doc', NULL, 'passed', '规则预审通过', NOW()),
(1004, 1002, 202, '近期免冠两寸照片', 'sample-two-inch-photo.jpg', '/mock/materials/sample-two-inch-photo.jpg', 196608, 'jpg', NULL, 'passed', '规则预审通过', NOW()),
(1005, 1003, 100, '居民身份证', 'sample-permit-id.pdf', '/mock/materials/sample-permit-id.pdf', 245760, 'pdf', NULL, 'passed', '规则预审通过', NOW()),
(1006, 1003, 101, '本人相片', 'sample-permit-photo.jpg', '/mock/materials/sample-permit-photo.jpg', 98304, 'jpg', NULL, 'passed', '规则预审通过', NOW()),
(1007, 1003, 102, '房屋租赁合同', 'sample-rental-contract.pdf', '/mock/materials/sample-rental-contract.pdf', 245760, 'pdf', NULL, 'failed', '租赁期限信息不完整，请补充完整合同页', NOW())
ON DUPLICATE KEY UPDATE
  template_id = VALUES(template_id),
  material_name = VALUES(material_name),
  file_name = VALUES(file_name),
  file_path = VALUES(file_path),
  file_size = VALUES(file_size),
  file_type = VALUES(file_type),
  precheck_status = VALUES(precheck_status),
  precheck_remark = VALUES(precheck_remark);

INSERT INTO work_order
(order_id, application_id, staff_user_id, status, audit_opinion, create_time, update_time, finish_time)
VALUES
(1001, 1001, NULL, 'pending', NULL, NOW(), NOW(), NULL),
(1002, 1002, 2, 'approved', '材料完整，审核通过。', NOW(), NOW(), NULL),
(1003, 1003, 2, 'supplement_required', '房屋租赁合同租赁期限信息不完整，请补充完整合同页后重新提交。', NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE
  staff_user_id = VALUES(staff_user_id),
  status = VALUES(status),
  audit_opinion = VALUES(audit_opinion),
  update_time = VALUES(update_time),
  finish_time = VALUES(finish_time);
*/

-- ============================================================
-- UTF-8 correction block for active demo data and material rules.
-- Keep this block last so it overrides any legacy seed rows above.
-- ============================================================

INSERT INTO resident_profile
(user_id, real_name, id_card, address, age, gender, birthday, resident_type)
VALUES
(3, '居民用户', '110101195901011234', '幸福社区1号楼101室', 65, 'private', '1959-01-01', 'local')
ON DUPLICATE KEY UPDATE
  real_name = VALUES(real_name),
  id_card = VALUES(id_card),
  address = VALUES(address),
  age = VALUES(age),
  gender = VALUES(gender),
  birthday = VALUES(birthday),
  resident_type = VALUES(resident_type);

INSERT INTO proxy_relation
(proxy_user_id, target_user_id, target_profile_id, relation, authorized_actions, status)
VALUES
(4, 3, (SELECT profile_id FROM resident_profile WHERE user_id = 3), 'child', 'apply,booking,query,notice', 'active')
ON DUPLICATE KEY UPDATE
  target_profile_id = VALUES(target_profile_id),
  relation = VALUES(relation),
  authorized_actions = VALUES(authorized_actions),
  status = VALUES(status);

INSERT INTO service_item
(item_id, item_name, item_code, category, description, conditions, process_steps, status)
VALUES
(1, '居住证办理', 'ITEM_001', '证件', '为非本地户籍居民办理居住证。', '非本地户籍居民，可按合法稳定住所、合法稳定就业、连续就读三类情形提交对应证明材料。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(2, '老年补贴申请', 'ITEM_002', '补贴', '为符合条件的老年居民申请高龄津贴。', '社区内符合高龄津贴条件的老年居民，需提交身份证、户口簿、近期免冠两寸照片、社保卡（银行卡）和高龄津贴申请表。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(3, '居住证明开具', 'ITEM_003', '证明', '开具在本辖区居住的证明材料。', '居民身份与居住地址可核验；居住在近亲属家中时需补充亲属关系证明。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(4, '便民证明', 'ITEM_004', '证明', '开具常见社区便民证明材料。', '社区居民可按实际用途申请无犯罪记录证明、低收入/困难证明、同一人身份证明等。', '["填写信息","上传材料","提交申请","等待审核"]', 'online'),
(5, '助餐服务预约', 'ITEM_005', '服务', '为老年居民提供助餐服务预约。', '老年居民可提交助餐服务申请表预约到店用餐或送餐到家；非本地居民按需补充户口簿或居住证。', '["填写信息","上传材料","提交申请","等待审核"]', 'online')
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  category = VALUES(category),
  description = VALUES(description),
  conditions = VALUES(conditions),
  process_steps = VALUES(process_steps),
  status = VALUES(status);

DELETE FROM service_material_template WHERE item_id IN (1, 2, 3, 4, 5);

INSERT INTO service_material_template
(template_id, item_id, material_name, material_type, description, is_required, sort_order)
VALUES
(100, 1, '居民身份证', 'id_card', '原件及复印件，用于核验申请人身份。', 1, 1),
(101, 1, '本人相片', 'personal_photo', '近期免冠彩色照片，部分地区可从人口系统提取。', 1, 2),
(102, 1, '房屋租赁合同', 'rental_contract', '合法稳定住所情形可选，上传租赁合同关键页。', 0, 3),
(103, 1, '房屋产权证明文件', 'real_estate_certificate', '合法稳定住所情形可选，上传房屋产权证明文件或不动产权证书关键信息页。', 0, 4),
(104, 1, '购房合同', 'purchase_contract', '合法稳定住所情形可选，上传购房合同关键信息页。', 0, 5),
(105, 1, '用人单位/就读学校出具的住宿证明', 'unit_accommodation_proof', '合法稳定住所情形可选，由单位或学校出具并盖章。', 0, 6),
(106, 1, '工商营业执照', 'business_license', '合法稳定就业情形可选，个体工商户或企业主提交。', 0, 7),
(107, 1, '劳动合同', 'labor_contract', '合法稳定就业情形可选，提交劳动合同关键页。', 0, 8),
(108, 1, '用人单位出具的劳动关系证明', 'labor_relation_proof', '合法稳定就业情形可选，由用人单位出具并盖章。', 0, 9),
(109, 1, '学生证', 'student_card', '连续就读情形可选，提交学生证关键信息页。', 0, 10),
(110, 1, '就读学校出具的连续就读证明', 'continuous_study_proof', '连续就读情形可选，由学校出具并盖章。', 0, 11),
(201, 2, '身份证', 'id_card', '申请人身份证明材料。', 1, 1),
(202, 2, '户口簿', 'household_register', '申请人户籍信息材料。', 1, 2),
(203, 2, '近期免冠两寸照片', 'recent_two_inch_photo', '近期6个月内免冠彩色两寸照，白色或淡蓝色背景。', 1, 3),
(204, 2, '社保卡（银行卡）', 'social_security_card', '用于补贴发放账户核验。', 1, 4),
(205, 2, '高龄津贴申请表', 'senior_allowance_application', '请下载模板填写申请人、紧急联系人和补贴发放账户信息并签字。', 1, 5),
(301, 3, '居民身份证（或户口簿）', 'identity_or_household_register', '用于核验申请人身份和户籍信息。', 1, 1),
(302, 3, '居住情况证明', 'residence_situation_proof', '可上传房产证、租房合同、单位宿舍证明等。', 1, 2),
(303, 3, '亲属关系证明', 'family_relationship_proof', '居住在近亲属家中时提交。', 0, 3),
(401, 4, '居民身份证', 'id_card', '申请人当前有效身份证或电子身份证。', 1, 1),
(402, 4, '户口簿', 'household_register', '首页和本人页，用于核验户籍及家庭关系。', 1, 2),
(403, 4, '申请事由证明', 'application_reason_proof', '如单位介绍信、录取通知书、签证要求说明或资格审查说明。', 0, 3),
(404, 4, '无犯罪记录证明申请表', 'no_criminal_record_application', '现场填写或下载模板填写后上传。', 0, 4),
(405, 4, '居住证', 'residence_permit_card', '非本地户籍人员部分城市需提供。', 0, 5),
(406, 4, '委托书', 'proxy_authorization', '委托他人代办时提交，并补充双方身份证。', 0, 6),
(407, 4, '收入说明', 'income_statement', '包括工资、养老金、子女赡养费等家庭收入。', 0, 7),
(408, 4, '家庭情况说明', 'family_situation_statement', '说明因病、因残、因灾等致困原因。', 0, 8),
(409, 4, '低收入家庭申请表', 'low_income_family_application', '社区或街道统一制式申请表。', 0, 9),
(410, 4, '病历/诊断证明', 'medical_diagnosis', '医院出具的病历、诊断书、出院小结等。', 0, 10),
(411, 4, '残疾证', 'disability_certificate', '残联颁发的残疾证。', 0, 11),
(412, 4, '失业证明', 'unemployment_certificate', '就业创业证或单位解除劳动关系证明。', 0, 12),
(413, 4, '在校学生证明', 'student_study_proof', '学校出具的在读证明或学生证。', 0, 13),
(414, 4, '婚姻状况证明', 'marital_status_proof', '单亲家庭可补充离婚证、配偶死亡证明等。', 0, 14),
(415, 4, '房产证明', 'housing_asset_proof', '部分地区用于核验住房困难情况。', 0, 15),
(416, 4, '车辆登记证明', 'vehicle_registration_proof', '部分地区用于核对家庭资产。', 0, 16),
(417, 4, '婚姻登记档案证明', 'marriage_archive_proof', '向原登记机关申请调取。', 0, 17),
(418, 4, '结婚证/离婚证', 'marriage_or_divorce_certificate', '补办证件或户口迁移等情形按需提供。', 0, 18),
(419, 4, '配偶死亡证明', 'spouse_death_certificate', '丧偶情形提供医院死亡证明或派出所注销户口证明。', 0, 19),
(420, 4, '法院判决书/调解书', 'court_judgment_or_mediation', '判决离婚情形需提供法院出具材料。', 0, 20),
(421, 4, '单位/社区证明', 'unit_or_community_marriage_proof', '无法获取档案时由单位人事部门或社区出具说明。', 0, 21),
(422, 4, '婚姻状况说明承诺书', 'marital_status_commitment', '无法提供完整档案时补充说明并签字承诺。', 0, 22),
(423, 4, '同一人身份声明书', 'same_person_identity_statement', '本人签字承诺两个身份信息为同一人。', 0, 23),
(424, 4, '原身份证件', 'old_identity_document', '旧身份证复印件或原身份证号证件。', 0, 24),
(425, 4, '户口簿曾用名页', 'household_previous_name_page', '户口簿中记载曾用名的页面。', 0, 25),
(426, 4, '单位/学校证明', 'unit_or_school_proof', '人事档案记录不一致时由单位或学校出具说明。', 0, 26),
(427, 4, '户籍变更证明', 'household_change_proof', '派出所出具的户籍变更证明。', 0, 27),
(428, 4, '佐证材料', 'supporting_identity_evidence', '驾驶证、护照、毕业证等能证明身份的证件。', 0, 28),
(501, 5, '身份证', 'id_card', '申请人身份证明材料。', 1, 1),
(502, 5, '社保卡', 'social_security_card', '用于核验身份及服务资格。', 1, 2),
(503, 5, '助餐服务申请表', 'meal_service_application', '请下载模板填写助餐服务需求、用餐频次和紧急联系人。', 1, 3),
(504, 5, '户口簿', 'household_register', '非本地首次申请时提交。', 0, 4),
(505, 5, '居住证', 'residence_permit_card', '非本地老年居民申请时提交。', 0, 5)
ON DUPLICATE KEY UPDATE
  material_name = VALUES(material_name),
  description = VALUES(description),
  is_required = VALUES(is_required),
  sort_order = VALUES(sort_order);

UPDATE application_form
SET form_data = '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
    remark = '居住证明开具申请'
WHERE application_id = 1001;

UPDATE application_form
SET form_data = '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室"}',
    remark = '老年补贴申请'
WHERE application_id = 1002;

UPDATE application_form
SET form_data = '{"name":"居民用户","idCard":"110101195901011234","phone":"13822222222","address":"幸福社区1号楼101室","applicationCondition":"stable_residence","applicationConditionLabel":"合法稳定住所","proofType":"rental_contract","proofLabel":"房屋租赁合同"}',
    remark = '居住证办理申请，需补充房屋租赁合同完整页'
WHERE application_id = 1003;

UPDATE work_order
SET audit_opinion = '材料完整，审核通过。'
WHERE order_id = 1002;

UPDATE work_order
SET audit_opinion = '房屋租赁合同租赁期限信息不完整，请补充完整合同页后重新提交。'
WHERE order_id = 1003;
