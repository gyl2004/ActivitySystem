-- 角色权限变更：根据需求重新划分超级管理员与管理员职责
-- 修正：超级管理员（super_admin）具备所有管理权限，管理员具备受限权限
-- 本脚本支持重复执行（幂等性）
-- 日期：2026-04-03

-- 1. 更新超级管理员角色标识 (保持 id=1)
UPDATE `sys_role` SET `role_key` = 'super_admin', `role_name` = '超级管理员', `status` = 1 WHERE `id` = 1;

-- 2. 处理管理员角色 (id=3)
-- 如果已存在则更新，不存在则插入
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `status`) 
VALUES (3, '管理员', 'admin', 1)
ON DUPLICATE KEY UPDATE `role_name` = '管理员', `role_key` = 'admin', `status` = 1;

-- 3. 重置并重新分配超级管理员 (id=1) 权限：赋予所有现有权限
DELETE FROM `sys_role_permission` WHERE `role_id` = 1;
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission`;

-- 4. 重置并重新分配管理员 (id=3) 权限：活动管理、报名审核、签到管理
DELETE FROM `sys_role_permission` WHERE `role_id` = 3;
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES 
(3, 1),  -- 活动管理 (父级)
(3, 2),  -- 创建活动
(3, 3),  -- 更新活动
(3, 4),  -- 删除活动
(3, 5),  -- 发布活动
(3, 6),  -- 取消活动
(3, 7),  -- 导出活动
(3, 20), -- 报名管理 (父级)
(3, 21), -- 审核报名
(3, 22), -- 导出报名
(3, 40), -- 签到管理 (父级)
(3, 41); -- 签到审核/二维码
