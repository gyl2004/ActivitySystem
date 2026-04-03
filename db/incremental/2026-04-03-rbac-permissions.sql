-- 增量脚本：修复新增接口 403（补齐 RBAC 权限并绑定管理员）
-- 适用库：activity_system（请在目标库中执行）
-- 说明：不会 TRUNCATE/DELETE，仅在不存在时插入，并为管理员角色绑定权限

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- 1) 确保父权限存在（避免子权限 parent_id 为空）
INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT 0, '活动管理', 'activity:manage', 1, 1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'activity:manage' AND deleted = 0
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT 0, '报名管理', 'registration:manage', 1, 2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'registration:manage' AND deleted = 0
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT 0, '签到管理', 'checkin:manage', 1, 5, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'checkin:manage' AND deleted = 0
);

-- 2) 读取父权限 ID
SET @activity_manage_id := (
    SELECT id FROM sys_permission WHERE permission_key = 'activity:manage' AND deleted = 0 ORDER BY id DESC LIMIT 1
);
SET @registration_manage_id := (
    SELECT id FROM sys_permission WHERE permission_key = 'registration:manage' AND deleted = 0 ORDER BY id DESC LIMIT 1
);
SET @checkin_manage_id := (
    SELECT id FROM sys_permission WHERE permission_key = 'checkin:manage' AND deleted = 0 ORDER BY id DESC LIMIT 1
);

-- 3) 插入缺失的按钮/接口权限（仅在不存在时插入）
INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT @activity_manage_id, '导出活动', 'activity:export', 3, 6, 1, 0
WHERE @activity_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'activity:export' AND deleted = 0
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT @registration_manage_id, '导出报名', 'registration:export', 3, 2, 1, 0
WHERE @registration_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'registration:export' AND deleted = 0
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT @checkin_manage_id, '签到审核/二维码', 'checkin:audit', 3, 1, 1, 0
WHERE @checkin_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'checkin:audit' AND deleted = 0
);

-- 4) 绑定到管理员角色（按 role_key=admin 定位；如果你的管理员 role_id 固定为 1，也可直接替换）
SET @admin_role_id := (
    SELECT id FROM sys_role WHERE role_key = 'admin' AND deleted = 0 ORDER BY id DESC LIMIT 1
);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role_id, p.id
FROM sys_permission p
WHERE @admin_role_id IS NOT NULL
  AND p.deleted = 0
  AND p.permission_key IN ('activity:export', 'registration:export', 'checkin:audit', 'checkin:manage');

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

