-- ============================================================
-- 社区服务协同平台 数据库初始化脚本
-- Version: V1.3.0
-- Database: community_service
-- Charset: utf8mb4 / utf8mb4_unicode_ci
-- 执行方式: mysql -u root -p < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS community_service
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE community_service;

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 用户账号表
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `user` (
  `user_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username`    VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
  `account`     VARCHAR(50)  DEFAULT NULL            COMMENT '账号（如 SQxxxx）',
  `password`    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码哈希',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `avatar`      VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0正常 1已删除',
  `role`        VARCHAR(30)  DEFAULT NULL            COMMENT 'admin/staff/resident/family',
  `community_id` BIGINT      DEFAULT NULL            COMMENT '社区 ID',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_user_community_id` (`community_id`)
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
  `community_id`  BIGINT       DEFAULT NULL COMMENT '社区 ID',
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
  UNIQUE KEY `uk_id_card` (`id_card`),
  KEY `idx_profile_community_id` (`community_id`)
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
  `order_id` BIGINT NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT NOT NULL COMMENT 'Application ID',
  `staff_user_id` BIGINT DEFAULT NULL COMMENT 'Assigned staff user ID',
  `community_id` BIGINT DEFAULT NULL COMMENT 'Community ID for staff isolation',
  `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/approved/supplement_required/rejected/completed/cancelled',
  `audit_opinion` TEXT DEFAULT NULL COMMENT 'Audit opinion',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `finish_time` DATETIME DEFAULT NULL COMMENT 'Completion time',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_application_id` (`application_id`),
  KEY `idx_staff_user_id` (`staff_user_id`),
  KEY `idx_work_order_community_id` (`community_id`),
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
  `community_id`   BIGINT       DEFAULT NULL COMMENT '社区 ID',
  PRIMARY KEY (`booking_id`),
  KEY `idx_user_id`      (`user_id`),
  KEY `idx_profile_id`   (`profile_id`),
  KEY `idx_proxy_user_id`(`proxy_user_id`),
  KEY `idx_status`       (`status`),
  KEY `idx_expect_time`  (`expect_time`),
  KEY `idx_staff_status` (`staff_user_id`, `status`),
  KEY `idx_booking_community_id` (`community_id`)
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
  (`user_id`, `username`, `account`, `password`, `phone`, `status`, `role`, `community_id`)
VALUES
  (1, 'admin',      'SQ0001', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13800000000', 'active', 'admin', 1),
  (2, 'staff01',    'SQ0002', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13811111111', 'active', 'staff', 1),
  (3, 'resident01', 'SQ0003', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13822222222', 'active', 'resident', 1),
  (4, 'family01',   'SQ0004', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13833333333', 'active', 'family', 1);

INSERT IGNORE INTO `user_role` (`user_id`, `role_code`) VALUES
  (1, 'ROLE_ADMIN'),
  (2, 'ROLE_STAFF'),
  (3, 'ROLE_RESIDENT'),
  (4, 'ROLE_FAMILY');

UPDATE `user` u
LEFT JOIN `user_role` r ON u.user_id = r.user_id
SET
  u.role = CASE r.role_code
    WHEN 'ROLE_ADMIN' THEN 'admin'
    WHEN 'ROLE_STAFF' THEN 'staff'
    WHEN 'ROLE_FAMILY' THEN 'family'
    ELSE 'resident'
  END,
  u.community_id = COALESCE(u.community_id, 1)
WHERE u.user_id IN (1, 2, 3, 4);

-- ============================================================
-- UTF-8 correction block for active demo data and material rules.
-- Keep this block last so it overrides any legacy seed rows above.
-- ============================================================

INSERT INTO resident_profile
(user_id, community_id, real_name, id_card, address, age, gender, birthday, resident_type)
VALUES
(3, 1, '居民用户', '110101195901011234', '幸福社区1号楼101室', 65, 'private', '1959-01-01', 'local')
ON DUPLICATE KEY UPDATE
  community_id = VALUES(community_id),
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
(4, '便民证明', 'ITEM_004', '证明', '开具常见社区便民证明材料。', '社区居民可按实际用途申请无犯罪记录证明、低收入/困难证明、同一人身份证明等。', '["填写信息","上传材料","提交申请","等待审核"]', 'online')
ON DUPLICATE KEY UPDATE
  item_name = VALUES(item_name),
  category = VALUES(category),
  description = VALUES(description),
  conditions = VALUES(conditions),
  process_steps = VALUES(process_steps),
  status = VALUES(status);

DELETE FROM service_material_template WHERE item_id IN (1, 2, 3, 4);

INSERT INTO service_material_template
(template_id, item_id, material_name, material_type, description, is_required, sort_order)
VALUES
(100, 1, '居民身份证', 'id_card', '原件及复印件，用于核验申请人身份。', 1, 1),
(101, 1, '本人相片', 'personal_photo', '近期免冠彩色照片，部分地区可从人口系统提取。', 1, 2),
(102, 1, '房屋租赁合同', 'rental_contract', '合法稳定住所情形可选，上传租赁合同关键页。', 0, 3),
(103, 1, '房屋产权证明文件', 'real_estate_certificate', '合法稳定住所情形可选，上传房屋产权证明文件或不动产权证书关键信息页。', 0, 4),
(104, 1, '购房合同', 'purchase_contract', '合法稳定住所情形可选，上传购房合同关键信息页。', 0, 5),
(105, 1, '用人单位出具的住宿证明', 'unit_accommodation_proof', '单位宿舍居住情形可选，由用人单位出具并盖章。', 0, 6),
(106, 1, '就读学校出具的住宿证明', 'school_accommodation_proof', '学校宿舍居住情形可选，由就读学校出具并盖章。', 0, 7),
(107, 1, '工商营业执照', 'business_license', '合法稳定就业情形可选，个体工商户或企业主提交。', 0, 8),
(108, 1, '劳动合同', 'labor_contract', '合法稳定就业情形可选，提交劳动合同关键页。', 0, 9),
(109, 1, '用人单位出具的劳动关系证明', 'labor_relation_proof', '合法稳定就业情形可选，由用人单位出具并盖章。', 0, 10),
(110, 1, '学生证', 'student_card', '连续就读情形可选，提交学生证关键信息页。', 0, 11),
(111, 1, '就读学校出具的连续就读证明', 'continuous_study_proof', '连续就读情形可选，由学校出具并盖章。', 0, 12),
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
(428, 4, '佐证材料', 'supporting_identity_evidence', '驾驶证、护照、毕业证等能证明身份的证件。', 0, 28)
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
