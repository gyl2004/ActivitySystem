-- 公益活动管理系统 数据库脚本
-- 数据库版本：MySQL 8.0
-- 创建日期：2026-03-27

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 系统权限模块 (RBAC)
-- ----------------------------

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `email` VARCHAR(100) DEFAULT NULL UNIQUE COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL UNIQUE COMMENT '手机号',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 (0-未知, 1-男, 2-女)',
    `status` TINYINT DEFAULT 1 COMMENT '状态 (0-禁用, 1-启用)',
    `points` INT DEFAULT 0 COMMENT '积分',
    `volunteer_duration` DOUBLE DEFAULT 0.0 COMMENT '志愿时长(小时)',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 (0-未删除, 1-已删除)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_key` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色权限字符串',
    `status` TINYINT DEFAULT 1 COMMENT '状态 (0-禁用, 1-启用)',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表 (菜单/接口)
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
    `name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `permission_key` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `type` TINYINT DEFAULT 1 COMMENT '类型 (1-目录, 2-菜单, 3-按钮/接口)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
    `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 (0-禁用, 1-启用)',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户-角色 关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色-权限 关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ----------------------------
-- 2. 活动核心模块
-- ----------------------------

-- 活动分类表
CREATE TABLE IF NOT EXISTS `activity_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态 (0-禁用, 1-启用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动分类表';

-- 活动表
CREATE TABLE IF NOT EXISTS `activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `title` VARCHAR(100) NOT NULL COMMENT '活动标题',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '活动摘要',
    `content` LONGTEXT DEFAULT NULL COMMENT '活动内容 (富文本)',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '活动封面图',
    `status` TINYINT DEFAULT 0 COMMENT '状态 (0-草稿, 1-待发布, 2-已发布, 3-进行中, 4-已结束, 5-已取消)',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `registration_start` DATETIME DEFAULT NULL COMMENT '报名开始时间',
    `registration_end` DATETIME DEFAULT NULL COMMENT '报名截止时间',
    `location_name` VARCHAR(255) DEFAULT NULL COMMENT '活动地点名称',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
    `longitude` DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
    `max_participants` INT DEFAULT 0 COMMENT '最大参与人数 (0-不限制)',
    `points` INT DEFAULT 0 COMMENT '活动提供积分',
    `volunteer_duration` DOUBLE DEFAULT 0.0 COMMENT '活动提供志愿时长',
    `registered_count` INT DEFAULT 0 COMMENT '已报名人数',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `share_count` INT DEFAULT 0 COMMENT '分享量',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- 报名表
CREATE TABLE IF NOT EXISTS `activity_registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态 (0-待审核, 1-审核通过, 2-审核驳回, 3-已取消)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '报名备注',
    `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    `custom_fields` TEXT DEFAULT NULL COMMENT '自定义表单数据 (JSON)',
    `earned_points` INT DEFAULT 0 COMMENT '实际获得积分',
    `earned_duration` DOUBLE DEFAULT 0.0 COMMENT '实际获得时长',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名表';

-- 签到表
CREATE TABLE IF NOT EXISTS `activity_checkin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `checkin_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
    `longitude` DECIMAL(10, 7) DEFAULT NULL COMMENT '签到经度',
    `latitude` DECIMAL(10, 7) DEFAULT NULL COMMENT '签到纬度',
    `checkin_type` TINYINT DEFAULT 1 COMMENT '签到方式 (1-扫描二维码, 2-管理员手动签到)',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '签到IP',
    PRIMARY KEY (`id`),
    KEY `idx_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到表';

-- ----------------------------
-- 3. 活动评价模块
-- ----------------------------

-- 活动评价表
CREATE TABLE IF NOT EXISTS `activity_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `rating` TINYINT DEFAULT 5 COMMENT '评分 (1-5星)',
    `content` TEXT DEFAULT NULL COMMENT '评价内容',
    `images` JSON DEFAULT NULL COMMENT '评价图片 (JSON数组)',
    `tags` JSON DEFAULT NULL COMMENT '评价标签 (JSON数组)',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数',
    `status` TINYINT DEFAULT 1 COMMENT '状态 (0-待审核, 1-已通过, 2-已拒绝)',
    `sentiment` VARCHAR(20) DEFAULT 'neutral' COMMENT '情感倾向 (positive, negative, neutral)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动评价表';

-- 评价回复表
CREATE TABLE IF NOT EXISTS `activity_review_reply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `review_id` BIGINT NOT NULL COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父回复ID (二级评论)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_review` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价回复表';

-- 评价点赞表
CREATE TABLE IF NOT EXISTS `activity_review_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `review_id` BIGINT NOT NULL COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` TINYINT DEFAULT 1 COMMENT '类型 (1-点赞, 2-点踩)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_user` (`review_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价点赞表';

-- ----------------------------
-- 4. 活动推荐模块
-- ----------------------------

-- 用户行为表 (推荐系统核心)
CREATE TABLE IF NOT EXISTS `user_behavior` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `behavior_type` VARCHAR(20) NOT NULL COMMENT '行为类型 (view, register, checkin, review, share)',
    `weight` DECIMAL(5, 2) DEFAULT 0.00 COMMENT '行为权重',
    `behavior_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_activity` (`user_id`, `activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- 推荐记录表
CREATE TABLE IF NOT EXISTS `activity_recommendation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `algorithm_type` VARCHAR(50) DEFAULT NULL COMMENT '算法类型 (UserCF/ItemCF/Content/Hot)',
    `score` DECIMAL(10, 4) DEFAULT 0.0000 COMMENT '推荐分数',
    `rank` INT DEFAULT 0 COMMENT '推荐排名',
    `is_clicked` TINYINT DEFAULT 0 COMMENT '是否点击 (0-否, 1-是)',
    `is_registered` TINYINT DEFAULT 0 COMMENT '是否报名 (0-否, 1-是)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐记录表';

-- 推荐配置表
CREATE TABLE IF NOT EXISTS `recommendation_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `algorithm_type` VARCHAR(50) NOT NULL UNIQUE COMMENT '算法类型',
    `weight` DECIMAL(5, 2) DEFAULT 1.00 COMMENT '权重',
    `params` JSON DEFAULT NULL COMMENT '算法参数 (JSON)',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用 (0-禁用, 1-启用)',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐配置表';

-- ----------------------------
-- 5. 系统设置与其它
-- ----------------------------

-- 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` TEXT DEFAULT NULL COMMENT '配置值',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

SET FOREIGN_KEY_CHECKS = 1;
