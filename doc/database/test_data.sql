-- ============================================================
-- 社区服务协同平台 测试数据脚本
-- 用途: 插入覆盖各角色、各状态、各业务流程的测试数据
-- 前置: 先执行 init.sql 完成建表和基础种子数据
-- 执行: docker exec -i community_mysql mysql -uroot -p123456 community_service < test_data.sql
-- 密码: 所有用户密码均为 123456
-- ============================================================

USE community_service;
SET NAMES utf8mb4;

-- ============================================================
-- 1. 用户数据 (user_id 10~30)
--    覆盖: 多社区、各角色、disabled 状态
--    密码哈希对应明文: 123456
-- ============================================================

INSERT INTO `user`
  (`user_id`, `username`, `account`, `password`, `phone`, `email`, `status`, `role`, `community_id`)
VALUES
  -- 社区1 额外工作人员
  (10, 'staff02',    'SQ0010', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000010', 'staff02@test.com',    'active',   'staff',    1),
  (11, 'staff03',    'SQ0011', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000011', 'staff03@test.com',    'active',   'staff',    1),
  -- 社区1 居民
  (12, 'resident02', 'SQ0012', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000012', 'resident02@test.com', 'active',   'resident', 1),
  (13, 'resident03', 'SQ0013', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000013', 'resident03@test.com', 'active',   'resident', 1),
  (14, 'resident04', 'SQ0014', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000014', 'resident04@test.com', 'active',   'resident', 1),
  (15, 'resident05', 'SQ0015', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000015', 'resident05@test.com', 'active',   'resident', 1),
  (16, 'resident06', 'SQ0016', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000016', NULL,                  'active',   'resident', 1),
  -- 社区1 家属
  (17, 'family02',   'SQ0017', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000017', 'family02@test.com',   'active',   'family',   1),
  (18, 'family03',   'SQ0018', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000018', 'family03@test.com',   'active',   'family',   1),
  -- 社区2 管理员 + 工作人员 + 居民
  (20, 'admin02',    'SQ0020', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000020', 'admin02@test.com',    'active',   'admin',    2),
  (21, 'staff_c2_01','SQ0021', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000021', 'staffc201@test.com',  'active',   'staff',    2),
  (22, 'resident_c2_01','SQ0022','$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS','13900000022','resc201@test.com',    'active',   'resident', 2),
  (23, 'resident_c2_02','SQ0023','$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS','13900000023','resc202@test.com',    'active',   'resident', 2),
  (24, 'family_c2_01','SQ0024','$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000024', 'famc201@test.com',   'active',   'family',   2),
  -- 禁用账号（测试 disabled 状态）
  (25, 'disabled01', 'SQ0025', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000025', NULL,                  'disabled', 'resident', 1),
  -- 已逻辑删除用户
  (26, 'deleted01',  'SQ0026', '$2b$10$YKqmZFlX3X/601xg61sUUOrjtdKOjDyXwS1pGN/FurrJ.84A0KLiS', '13900000026', NULL,                  'active',   'resident', 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 逻辑删除 user_id=26
UPDATE `user` SET deleted = 1 WHERE user_id = 26;

-- ============================================================
-- 2. 用户角色关联
-- ============================================================

INSERT IGNORE INTO `user_role` (`user_id`, `role_code`) VALUES
  (10, 'ROLE_STAFF'),
  (11, 'ROLE_STAFF'),
  (12, 'ROLE_RESIDENT'),
  (13, 'ROLE_RESIDENT'),
  (14, 'ROLE_RESIDENT'),
  (15, 'ROLE_RESIDENT'),
  (16, 'ROLE_RESIDENT'),
  (17, 'ROLE_FAMILY'),
  (18, 'ROLE_FAMILY'),
  (20, 'ROLE_ADMIN'),
  (21, 'ROLE_STAFF'),
  (22, 'ROLE_RESIDENT'),
  (23, 'ROLE_RESIDENT'),
  (24, 'ROLE_FAMILY'),
  (25, 'ROLE_RESIDENT'),
  (26, 'ROLE_RESIDENT');

-- ============================================================
-- 3. 居民档案
--    覆盖: 不同年龄段、性别、本地/非本地、有/无用户绑定
-- ============================================================

INSERT INTO `resident_profile`
  (`profile_id`, `user_id`, `community_id`, `real_name`, `id_card`, `gender`, `birthday`, `address`, `age`, `resident_type`)
VALUES
  (10, 12, 1, '张秀英', '110101196505152345', 'female', '1965-05-15', '幸福社区2号楼201室', 61, 'local'),
  (11, 13, 1, '李大明', '110101197208203456', 'male',   '1972-08-20', '幸福社区3号楼302室', 54, 'local'),
  (12, 14, 1, '王小红', '110101198811114567', 'female', '1988-11-11', '幸福社区4号楼103室', 38, 'non_local'),
  (13, 15, 1, '赵国强', '110101194503065678', 'male',   '1945-03-06', '幸福社区5号楼501室', 81, 'local'),
  (14, 16, 1, '陈秋月', '110101195207186789', 'female', '1952-07-18', '幸福社区1号楼402室', 74, 'local'),
  -- 社区2 居民
  (20, 22, 2, '刘建国', '320102198003127890', 'male',   '1980-03-12', '和谐社区1号楼201室', 46, 'local'),
  (21, 23, 2, '孙丽华', '320102195512089012', 'female', '1955-12-08', '和谐社区2号楼103室', 71, 'local'),
  -- 无绑定用户的档案（先建档后注册场景）
  (30, NULL, 1, '周未注册', '110101196801011111', 'male', '1968-01-01', '幸福社区6号楼601室', 58, 'local'),
  (31, NULL, 2, '吴未绑定', '320102197505052222', 'female','1975-05-05', '和谐社区3号楼301室', 51, 'non_local')
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

-- ============================================================
-- 4. 家属代理关系
--    覆盖: active/pending/revoked 状态、不同关系类型
-- ============================================================

INSERT INTO `proxy_relation`
  (`proxy_user_id`, `target_user_id`, `target_profile_id`, `relation`, `authorized_actions`, `status`)
VALUES
  (17, 13, 11, 'spouse',    'apply,booking,query,notice', 'active'),
  (17, 15, 13, 'child',     'apply,booking,query,notice', 'active'),
  (18, 14, 12, 'child',     'apply,booking,query',        'active'),
  (18, 16, 14, 'volunteer', 'booking,query',              'pending'),
  (24, 22, 20, 'child',     'apply,booking,query,notice', 'active'),
  (24, 23, 21, 'spouse',    'apply,booking,query,notice', 'revoked')
ON DUPLICATE KEY UPDATE relation = VALUES(relation), status = VALUES(status);

-- ============================================================
-- 5. 申请单数据
--    覆盖: 各事项 x 各状态流转
-- ============================================================

INSERT INTO `application_form`
  (`application_id`, `user_id`, `profile_id`, `item_id`, `proxy_user_id`, `status`, `form_data`, `remark`, `submit_time`)
VALUES
  -- 居民自主申请
  (101, 12, 10, 1, NULL, 'pending',
   '{"name":"张秀英","idCard":"110101196505152345","phone":"13900000012","address":"幸福社区2号楼201室","applicationCondition":"stable_residence","proofType":"rental_contract"}',
   '居住证办理', '2026-05-20 09:30:00'),
  (102, 12, 10, 3, NULL, 'approved',
   '{"name":"张秀英","idCard":"110101196505152345","phone":"13900000012","address":"幸福社区2号楼201室"}',
   '居住证明开具', '2026-05-18 14:00:00'),
  (103, 13, 11, 2, NULL, 'completed',
   '{"name":"李大明","idCard":"110101197208203456","phone":"13900000013","address":"幸福社区3号楼302室"}',
   '老年补贴申请', '2026-05-10 10:00:00'),
  (104, 14, 12, 1, NULL, 'rejected',
   '{"name":"王小红","idCard":"110101198811114567","phone":"13900000014","address":"幸福社区4号楼103室","applicationCondition":"stable_employment","proofType":"labor_contract"}',
   '居住证办理-材料不合规', '2026-05-15 11:20:00'),
  (105, 15, 13, 2, NULL, 'supplement_required',
   '{"name":"赵国强","idCard":"110101194503065678","phone":"13900000015","address":"幸福社区5号楼501室"}',
   '老年补贴申请-需补充社保卡照片', '2026-05-22 08:45:00'),
  (106, 15, 13, 4, NULL, 'pending',
   '{"name":"赵国强","idCard":"110101194503065678","phone":"13900000015","address":"幸福社区5号楼501室"}',
   '便民证明-无犯罪记录', '2026-06-01 16:00:00'),
  (107, 16, 14, 2, NULL, 'approved',
   '{"name":"陈秋月","idCard":"110101195207186789","phone":"13900000016","address":"幸福社区1号楼402室"}',
   '老年补贴申请', '2026-05-25 09:10:00'),
  -- 家属代办申请
  (108, 17, 13, 2, 17, 'pending',
   '{"name":"赵国强","idCard":"110101194503065678","phone":"13900000017","address":"幸福社区5号楼501室","proxyName":"家属代办"}',
   '家属代办-老年补贴', '2026-06-02 10:30:00'),
  (109, 17, 11, 3, 17, 'approved',
   '{"name":"李大明","idCard":"110101197208203456","phone":"13900000017","address":"幸福社区3号楼302室","proxyName":"家属代办"}',
   '家属代办-居住证明', '2026-05-28 15:00:00'),
  (110, 18, 12, 4, 18, 'supplementing',
   '{"name":"王小红","idCard":"110101198811114567","phone":"13900000018","address":"幸福社区4号楼103室","proxyName":"家属代办"}',
   '家属代办-便民证明-补充材料中', '2026-05-30 11:00:00'),
  -- 社区2 申请
  (111, 22, 20, 1, NULL, 'pending',
   '{"name":"刘建国","idCard":"320102198003127890","phone":"13900000022","address":"和谐社区1号楼201室","applicationCondition":"continuous_study","proofType":"student_card"}',
   '居住证办理', '2026-06-03 09:00:00'),
  (112, 23, 21, 2, NULL, 'approved',
   '{"name":"孙丽华","idCard":"320102195512089012","phone":"13900000023","address":"和谐社区2号楼103室"}',
   '老年补贴申请', '2026-05-26 10:20:00'),
  (113, 24, 21, 3, 24, 'completed',
   '{"name":"孙丽华","idCard":"320102195512089012","phone":"13900000024","address":"和谐社区2号楼103室","proxyName":"家属代办"}',
   '家属代办-居住证明', '2026-05-12 14:30:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- ============================================================
-- 6. 申请材料文件元数据
--    覆盖: 预审通过/失败/待审
-- ============================================================

INSERT INTO `application_material`
  (`material_id`, `application_id`, `template_id`, `material_name`, `file_name`, `file_path`, `file_size`, `file_type`, `ocr_text`, `precheck_status`, `precheck_remark`)
VALUES
  (1001, 101, 100, '居民身份证',   'id_card_zhang.jpg',       'uploads/materials/101/id_card.jpg',       524288,  'jpg', '张秀英 110101196505152345', 'passed', NULL),
  (1002, 101, 101, '本人相片',     'photo_zhang.jpg',         'uploads/materials/101/photo.jpg',         204800,  'jpg', NULL, 'passed', NULL),
  (1003, 101, 102, '房屋租赁合同', 'rental_zhang.pdf',        'uploads/materials/101/rental.pdf',        1048576, 'pdf', '租赁期限：2025年1月至2027年12月', 'pending', NULL),
  (1004, 102, 301, '居民身份证',   'id_card_zhang2.jpg',      'uploads/materials/102/id_card.jpg',       524288,  'jpg', '张秀英 110101196505152345', 'passed', NULL),
  (1005, 102, 302, '居住情况证明', 'residence_proof_zhang.pdf','uploads/materials/102/residence.pdf',     819200,  'pdf', '幸福社区2号楼201室', 'passed', NULL),
  (1006, 103, 201, '身份证',       'id_card_li.jpg',          'uploads/materials/103/id_card.jpg',       524288,  'jpg', '李大明 110101197208203456', 'passed', NULL),
  (1007, 103, 202, '户口簿',       'household_li.jpg',        'uploads/materials/103/household.jpg',     614400,  'jpg', NULL, 'passed', NULL),
  (1008, 103, 203, '近期免冠照片', 'photo_li.jpg',            'uploads/materials/103/photo.jpg',         204800,  'jpg', NULL, 'passed', NULL),
  (1009, 103, 204, '社保卡',       'social_card_li.jpg',      'uploads/materials/103/social_card.jpg',   409600,  'jpg', NULL, 'passed', NULL),
  (1010, 103, 205, '高龄津贴申请表','senior_form_li.pdf',     'uploads/materials/103/senior_form.pdf',   307200,  'pdf', NULL, 'passed', NULL),
  (1011, 104, 100, '居民身份证',   'id_card_wang.jpg',        'uploads/materials/104/id_card.jpg',       524288,  'jpg', '王小红 110101198811114567', 'passed', NULL),
  (1012, 104, 108, '劳动合同',     'labor_wang.pdf',          'uploads/materials/104/labor.pdf',         1572864, 'pdf', '合同已过期', 'failed', '劳动合同已过有效期，请提供当前有效的劳动合同'),
  (1013, 105, 201, '身份证',       'id_card_zhao.jpg',        'uploads/materials/105/id_card.jpg',       524288,  'jpg', '赵国强 110101194503065678', 'passed', NULL),
  (1014, 105, 204, '社保卡',       'social_card_zhao.jpg',    'uploads/materials/105/social_card.jpg',   409600,  'jpg', NULL, 'failed', '照片模糊无法识别卡号，请重新拍摄上传'),
  (1015, 107, 201, '身份证',       'id_card_chen.jpg',        'uploads/materials/107/id_card.jpg',       524288,  'jpg', '陈秋月 110101195207186789', 'passed', NULL),
  (1016, 107, 202, '户口簿',       'household_chen.jpg',      'uploads/materials/107/household.jpg',     614400,  'jpg', NULL, 'passed', NULL),
  (1017, 107, 205, '高龄津贴申请表','senior_form_chen.pdf',   'uploads/materials/107/senior_form.pdf',   307200,  'pdf', NULL, 'passed', NULL),
  (1018, 111, 100, '居民身份证',   'id_card_liu.jpg',         'uploads/materials/111/id_card.jpg',       524288,  'jpg', '刘建国 320102198003127890', 'passed', NULL),
  (1019, 111, 110, '学生证',       'student_card_liu.jpg',    'uploads/materials/111/student_card.jpg',  409600,  'jpg', NULL, 'pending', NULL)
ON DUPLICATE KEY UPDATE precheck_status = VALUES(precheck_status);

-- ============================================================
-- 7. 工单数据
--    覆盖: 各状态、不同工作人员分配、有/无完成时间
-- ============================================================

INSERT INTO `work_order`
  (`order_id`, `application_id`, `staff_user_id`, `community_id`, `status`, `audit_opinion`, `create_time`, `finish_time`)
VALUES
  (201, 101, 2,    1, 'pending',     NULL, '2026-05-20 09:35:00', NULL),
  (202, 102, 2,    1, 'approved',    '材料齐全，居住证明已开具。', '2026-05-18 14:10:00', '2026-05-19 10:00:00'),
  (203, 103, 10,   1, 'completed',   '高龄津贴审核通过，已录入发放系统。', '2026-05-10 10:10:00', '2026-05-12 15:30:00'),
  (204, 104, 10,   1, 'rejected',    '劳动合同已过期，不符合合法稳定就业条件。', '2026-05-15 11:30:00', '2026-05-16 09:00:00'),
  (205, 105, 11,   1, 'supplement_required', '社保卡照片模糊，请重新拍摄清晰照片后补充上传。', '2026-05-22 09:00:00', NULL),
  (206, 106, NULL, 1, 'pending',     NULL, '2026-06-01 16:05:00', NULL),
  (207, 107, 11,   1, 'approved',    '材料完整，高龄津贴审批通过。', '2026-05-25 09:20:00', '2026-05-27 11:00:00'),
  (208, 108, NULL, 1, 'pending',     NULL, '2026-06-02 10:35:00', NULL),
  (209, 109, 2,    1, 'approved',    '居住证明开具完成。', '2026-05-28 15:10:00', '2026-05-29 09:00:00'),
  (210, 110, 10,   1, 'supplement_required', '身份证复印件缺少反面，请补充。', '2026-05-30 11:10:00', NULL),
  (211, 111, 21,   2, 'pending',     NULL, '2026-06-03 09:10:00', NULL),
  (212, 112, 21,   2, 'approved',    '老年补贴审核通过。', '2026-05-26 10:30:00', '2026-05-28 14:00:00'),
  (213, 113, 21,   2, 'completed',   '居住证明已开具并送达。', '2026-05-12 14:40:00', '2026-05-13 10:00:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- ============================================================
-- 8. 工单操作日志
--    覆盖: 多步流转记录
-- ============================================================

INSERT INTO `work_order_log`
  (`order_id`, `operator_id`, `action`, `from_status`, `to_status`, `remark`, `create_time`)
VALUES
  -- 工单202 流转
  (202, 2,  'claim',   'pending',    'processing', '接单处理', '2026-05-18 14:10:00'),
  (202, 2,  'approve', 'processing', 'approved',   '材料齐全，居住证明已开具。', '2026-05-19 10:00:00'),
  -- 工单203 流转
  (203, 10, 'claim',   'pending',    'processing', '接单处理', '2026-05-10 10:10:00'),
  (203, 10, 'approve', 'processing', 'approved',   '审核通过', '2026-05-11 14:00:00'),
  (203, 10, 'complete','approved',   'completed',  '已录入发放系统，办结。', '2026-05-12 15:30:00'),
  -- 工单204 流转
  (204, 10, 'claim',   'pending',    'processing', '接单处理', '2026-05-15 11:30:00'),
  (204, 10, 'reject',  'processing', 'rejected',   '劳动合同已过期，不符合条件。', '2026-05-16 09:00:00'),
  -- 工单205 流转
  (205, 11, 'claim',   'pending',    'processing', '接单处理', '2026-05-22 09:00:00'),
  (205, 11, 'supplement','processing','supplement_required', '社保卡照片模糊，需补充。', '2026-05-22 10:30:00'),
  -- 工单207 流转
  (207, 11, 'claim',   'pending',    'processing', '接单处理', '2026-05-25 09:20:00'),
  (207, 11, 'approve', 'processing', 'approved',   '材料完整，审批通过。', '2026-05-27 11:00:00'),
  -- 工单209 流转
  (209, 2,  'claim',   'pending',    'processing', '接单处理', '2026-05-28 15:10:00'),
  (209, 2,  'approve', 'processing', 'approved',   '居住证明开具完成。', '2026-05-29 09:00:00'),
  -- 工单212 流转（社区2）
  (212, 21, 'claim',   'pending',    'processing', '接单处理', '2026-05-26 10:30:00'),
  (212, 21, 'approve', 'processing', 'approved',   '老年补贴审核通过。', '2026-05-28 14:00:00'),
  -- 工单213 流转（社区2）
  (213, 21, 'claim',   'pending',    'processing', '接单处理', '2026-05-12 14:40:00'),
  (213, 21, 'approve', 'processing', 'approved',   '审核通过', '2026-05-12 16:00:00'),
  (213, 21, 'complete','approved',   'completed',  '已开具并送达。', '2026-05-13 10:00:00');

-- ============================================================
-- 9. 社区服务预约
--    覆盖: 各服务类型、各状态、自约/代约、有无反馈
-- ============================================================

INSERT INTO `service_booking`
  (`booking_id`, `user_id`, `profile_id`, `proxy_user_id`, `service_type`, `expect_time`, `status`, `address`, `remark`, `staff_user_id`, `feedback`, `create_time`, `complete_time`, `community_id`)
VALUES
  -- 助餐服务
  (301, 15, 13, NULL, 'dining', '2026-06-05 11:30:00', 'completed', '幸福社区5号楼501室', '清淡少油少盐', 11, '服务准时，饭菜可口。', '2026-06-03 08:00:00', '2026-06-05 12:00:00', 1),
  (302, 16, 14, NULL, 'dining', '2026-06-06 11:30:00', 'confirmed', '幸福社区1号楼402室', '糖尿病饮食', 11, NULL, '2026-06-04 09:00:00', NULL, 1),
  (303, 13, 11, NULL, 'dining', '2026-06-07 11:30:00', 'pending',   '幸福社区3号楼302室', NULL, NULL, NULL, '2026-06-05 10:00:00', NULL, 1),
  -- 陪诊服务
  (304, 15, 13, NULL, 'accompany', '2026-06-08 08:00:00', 'confirmed', '市第一人民医院', '心内科复查，需轮椅', 10, NULL, '2026-06-04 14:00:00', NULL, 1),
  (305, 12, 10, NULL, 'accompany', '2026-06-10 09:00:00', 'pending', '市中医院', '骨科复查', NULL, NULL, '2026-06-05 16:00:00', NULL, 1),
  -- 上门探访
  (306, 16, 14, NULL, 'home_visit', '2026-06-09 14:00:00', 'in_progress', '幸福社区1号楼402室', '独居老人定期探访', 2, NULL, '2026-06-05 08:00:00', NULL, 1),
  (307, 15, 13, NULL, 'home_visit', '2026-05-28 14:00:00', 'completed', '幸福社区5号楼501室', '术后康复探访', 2, '老人恢复良好，情绪稳定。', '2026-05-26 09:00:00', '2026-05-28 15:30:00', 1),
  -- 家属代约
  (308, 17, 13, 17, 'dining',     '2026-06-06 11:30:00', 'confirmed', '幸福社区5号楼501室', '家属代约-软食', 11, NULL, '2026-06-04 10:00:00', NULL, 1),
  (309, 17, 11, 17, 'accompany',  '2026-06-12 09:00:00', 'pending',   '市第三人民医院', '家属代约-眼科检查', NULL, NULL, '2026-06-05 11:00:00', NULL, 1),
  (310, 18, 12, 18, 'home_visit', '2026-06-11 10:00:00', 'pending',   '幸福社区4号楼103室', '家属代约-定期探望', NULL, NULL, '2026-06-05 14:00:00', NULL, 1),
  -- 已取消
  (311, 14, 12, NULL, 'dining', '2026-06-04 11:30:00', 'cancelled', '幸福社区4号楼103室', '临时有事取消', NULL, NULL, '2026-06-02 09:00:00', NULL, 1),
  -- 社区2 预约
  (312, 22, 20, NULL, 'dining',     '2026-06-06 12:00:00', 'confirmed', '和谐社区1号楼201室', NULL, 21, NULL, '2026-06-04 08:00:00', NULL, 2),
  (313, 23, 21, NULL, 'accompany',  '2026-06-09 08:30:00', 'pending',   '区人民医院', '高血压复查', NULL, NULL, '2026-06-05 07:00:00', NULL, 2),
  (314, 24, 21, 24, 'home_visit',  '2026-06-10 15:00:00', 'confirmed', '和谐社区2号楼103室', '家属代约-探访', 21, NULL, '2026-06-05 09:00:00', NULL, 2)
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- ============================================================
-- 10. 站内通知消息
--     覆盖: 各类型、已读/未读
-- ============================================================

INSERT INTO `notice`
  (`notice_id`, `user_id`, `title`, `content`, `type`, `ref_type`, `ref_id`, `is_read`, `create_time`, `read_time`)
VALUES
  -- 审核结果通知
  (401, 12, '您的居住证明申请已通过', '您提交的居住证明开具申请（编号102）已审核通过，请携带身份证到社区服务中心领取。', 'audit_result', 'application', 102, 1, '2026-05-19 10:05:00', '2026-05-19 12:00:00'),
  (402, 13, '您的老年补贴申请已通过', '您提交的老年补贴申请（编号103）已审核通过，补贴将于下月发放至社保卡账户。', 'audit_result', 'application', 103, 1, '2026-05-12 15:35:00', '2026-05-12 18:00:00'),
  (403, 14, '您的居住证办理申请被退回', '您提交的居住证办理申请（编号104）因材料不合规被退回。原因：劳动合同已过期。请更新材料后重新提交。', 'audit_result', 'application', 104, 1, '2026-05-16 09:05:00', '2026-05-16 10:30:00'),
  (404, 16, '您的老年补贴申请已通过', '您提交的老年补贴申请（编号107）已审核通过。', 'audit_result', 'application', 107, 0, '2026-05-27 11:05:00', NULL),
  -- 补充材料通知
  (405, 15, '请补充材料：老年补贴申请', '您的老年补贴申请（编号105）需要补充材料：社保卡照片模糊，请重新拍摄清晰照片后上传。', 'supplement', 'application', 105, 1, '2026-05-22 10:35:00', '2026-05-22 14:00:00'),
  (406, 18, '请补充材料：便民证明申请', '您代办的便民证明申请（编号110）需要补充身份证反面复印件。', 'supplement', 'application', 110, 0, '2026-05-30 11:15:00', NULL),
  -- 预约类通知
  (407, 15, '助餐服务预约已确认', '您预约的6月5日助餐服务已确认，工作人员将按时送达。', 'booking', 'booking', 301, 1, '2026-06-03 08:30:00', '2026-06-03 09:00:00'),
  (408, 16, '助餐服务预约已确认', '您预约的6月6日助餐服务已确认。', 'booking', 'booking', 302, 0, '2026-06-04 09:30:00', NULL),
  (409, 15, '陪诊服务预约已确认', '您预约的6月8日陪诊服务已确认，工作人员将提前到达接您。', 'booking', 'booking', 304, 1, '2026-06-04 14:30:00', '2026-06-04 15:00:00'),
  (410, 17, '代约助餐服务已确认', '您为赵国强代约的6月6日助餐服务已确认。', 'booking', 'booking', 308, 0, '2026-06-04 10:30:00', NULL),
  -- 系统通知
  (411, 12, '系统维护通知', '系统将于6月8日凌晨2:00-4:00进行例行维护，届时服务暂停。', 'system', 'system', NULL, 0, '2026-06-05 09:00:00', NULL),
  (412, 13, '系统维护通知', '系统将于6月8日凌晨2:00-4:00进行例行维护，届时服务暂停。', 'system', 'system', NULL, 0, '2026-06-05 09:00:00', NULL),
  (413, 14, '系统维护通知', '系统将于6月8日凌晨2:00-4:00进行例行维护，届时服务暂停。', 'system', 'system', NULL, 0, '2026-06-05 09:00:00', NULL),
  (414, 15, '系统维护通知', '系统将于6月8日凌晨2:00-4:00进行例行维护，届时服务暂停。', 'system', 'system', NULL, 0, '2026-06-05 09:00:00', NULL),
  (415, 16, '系统维护通知', '系统将于6月8日凌晨2:00-4:00进行例行维护，届时服务暂停。', 'system', 'system', NULL, 0, '2026-06-05 09:00:00', NULL),
  -- 社区2 通知
  (416, 22, '助餐服务预约已确认', '您预约的6月6日助餐服务已确认。', 'booking', 'booking', 312, 0, '2026-06-04 08:30:00', NULL),
  (417, 23, '您的老年补贴申请已通过', '您提交的老年补贴申请（编号112）已审核通过。', 'audit_result', 'application', 112, 1, '2026-05-28 14:05:00', '2026-05-28 16:00:00'),
  -- 家属收到被代理人的通知
  (418, 17, '赵国强的老年补贴申请已受理', '您代办的赵国强老年补贴申请（编号108）已提交，等待工作人员处理。', 'audit_result', 'application', 108, 0, '2026-06-02 10:35:00', NULL)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- ============================================================
-- 数据统计概览（插入后可验证）:
--   用户:   16 条新增 (含 1 禁用、1 逻辑删除)
--   档案:    9 条新增 (含 2 条无用户绑定)
--   代理:    6 条新增 (active/pending/revoked)
--   申请:   13 条新增 (覆盖全部6种状态)
--   材料:   19 条新增 (passed/failed/pending)
--   工单:   13 条新增 (覆盖全部状态)
--   日志:   18 条新增 (多步流转)
--   预约:   14 条新增 (3种服务类型, 5种状态, 含代约)
--   通知:   18 条新增 (4种类型, 已读/未读)
-- ============================================================
