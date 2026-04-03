-- 增量脚本：补齐分类管理与用户角色分配相关 RBAC 权限，并绑定管理员
-- 适用库：activity_system（请在目标库中执行）
-- 说明：不会 TRUNCATE/DELETE，仅在不存在时插入，并为管理员角色绑定权限

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT 0, '分类管理', 'category:manage', 1, 6, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'category:manage' AND deleted = 0
);

SET @category_manage_id := (
    SELECT id FROM sys_permission WHERE permission_key = 'category:manage' AND deleted = 0 ORDER BY id DESC LIMIT 1
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT @category_manage_id, '创建分类', 'category:create', 3, 1, 1, 0
WHERE @category_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'category:create' AND deleted = 0
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT @category_manage_id, '更新分类', 'category:update', 3, 2, 1, 0
WHERE @category_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'category:update' AND deleted = 0
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT @category_manage_id, '删除分类', 'category:delete', 3, 3, 1, 0
WHERE @category_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'category:delete' AND deleted = 0
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT 0, '用户管理', 'user:manage', 1, 7, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'user:manage' AND deleted = 0
);

SET @user_manage_id := (
    SELECT id FROM sys_permission WHERE permission_key = 'user:manage' AND deleted = 0 ORDER BY id DESC LIMIT 1
);

INSERT INTO sys_permission (parent_id, name, permission_key, type, sort, status, deleted)
SELECT @user_manage_id, '分配角色', 'user:role:assign', 3, 1, 1, 0
WHERE @user_manage_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_key = 'user:role:assign' AND deleted = 0
);

SET @admin_role_id := (
    SELECT id FROM sys_role WHERE role_key = 'admin' AND deleted = 0 ORDER BY id DESC LIMIT 1
);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role_id, p.id
FROM sys_permission p
WHERE @admin_role_id IS NOT NULL
  AND p.deleted = 0
  AND p.permission_key IN ('category:manage', 'category:create', 'category:update', 'category:delete', 'user:manage', 'user:role:assign');

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
