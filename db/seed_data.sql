-- 公益活动管理系统 初始种子数据
-- 数据库版本：MySQL 8.0
-- 创建日期：2026-03-27

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 数据清理 (确保脚本可重复执行)
-- ----------------------------

TRUNCATE TABLE sys_user;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE activity_category;
TRUNCATE TABLE activity;
TRUNCATE TABLE activity_registration;
TRUNCATE TABLE activity_review;
TRUNCATE TABLE sys_config;

-- ----------------------------
-- 2. 角色与权限初始化
-- ----------------------------

-- 插入角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `status`) VALUES 
(1, '超级管理员', 'admin', 1),
(2, '普通志愿者', 'volunteer', 1);

-- 插入权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `permission_key`, `type`, `sort`, `status`) VALUES 
-- 活动管理权限
(1, 0, '活动管理', 'activity:manage', 1, 1, 1),
(2, 1, '创建活动', 'activity:create', 3, 1, 1),
(3, 1, '更新活动', 'activity:update', 3, 2, 1),
(4, 1, '删除活动', 'activity:delete', 3, 3, 1),
(5, 1, '发布活动', 'activity:publish', 3, 4, 1),
(6, 1, '取消活动', 'activity:cancel', 3, 5, 1),
(7, 1, '导出活动', 'activity:export', 3, 6, 1),

-- 分类管理权限
(8, 0, '分类管理', 'category:manage', 1, 6, 1),
(9, 8, '创建分类', 'category:create', 3, 1, 1),
(10, 8, '更新分类', 'category:update', 3, 2, 1),
(11, 8, '删除分类', 'category:delete', 3, 3, 1),

-- 报名与审核权限
(20, 0, '报名管理', 'registration:manage', 1, 2, 1),
(21, 20, '审核报名', 'registration:audit', 3, 1, 1),
(22, 20, '导出报名', 'registration:export', 3, 2, 1),

-- 签到管理权限
(40, 0, '签到管理', 'checkin:manage', 1, 5, 1),
(41, 40, '签到审核/二维码', 'checkin:audit', 3, 1, 1),

-- 评价管理权限
(30, 0, '评价管理', 'review:manage', 1, 3, 1),
(31, 30, '审核评价', 'review:audit', 3, 1, 1),

-- 统计权限
(50, 0, '统计分析', 'statistics:manage', 1, 4, 1),
(51, 50, '查看统计', 'statistics:view', 3, 1, 1),

-- 用户管理权限
(60, 0, '用户管理', 'user:manage', 1, 7, 1),
(61, 60, '分配角色', 'user:role:assign', 3, 1, 1);

-- 角色-权限关联 (管理员拥有所有权限)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES 
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
(1, 8), (1, 9), (1, 10), (1, 11),
(1, 20), (1, 21), (1, 22),
(1, 40), (1, 41),
(1, 30), (1, 31),
(1, 50), (1, 51),
(1, 60), (1, 61);

-- ----------------------------
-- 3. 用户初始化
-- ----------------------------

-- 插入管理员 (密码: admin123)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `avatar`, `email`, `phone`, `status`) VALUES 
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix', 'admin@charity.com', '13800138000', 1);

-- 插入普通用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `avatar`, `email`, `phone`, `status`) VALUES 
(2, 'volunteer_01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '阳光小智', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Lucky', 'user1@charity.com', '13800138001', 1),
(3, 'volunteer_02', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '爱心大使', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Happy', 'user2@charity.com', '13800138002', 1);

-- 关联用户角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES 
(1, 1), -- 管理员
(2, 2), -- 志愿者1
(3, 2); -- 志愿者2

-- ----------------------------
-- 4. 业务数据初始化 (便于展示)
-- ----------------------------

-- 活动分类
INSERT INTO `activity_category` (`id`, `name`, `sort`, `status`) VALUES 
(1, '社区关爱', 1, 1),
(2, '绿色环保', 2, 1),
(3, '助学支教', 3, 1),
(4, '文化传承', 4, 1),
(5, '动物保护', 5, 1);

-- 示例活动
INSERT INTO `activity` (`id`, `category_id`, `title`, `summary`, `content`, `cover_image`, `status`, `start_time`, `end_time`, `registration_start`, `registration_end`, `location_name`, `address`, `longitude`, `latitude`, `max_participants`, `points`, `volunteer_duration`, `registered_count`, `view_count`, `create_user_id`) VALUES 
(1, 1, '社区独居老人关怀行动', '在这个温暖的周末，我们一起走进社区，为那些独居的老人们带去关爱与陪伴。', '<p>活动详情...</p>', 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&q=80&w=800', 2, '2026-04-10 09:00:00', '2026-04-10 17:00:00', '2026-03-27 00:00:00', '2026-04-05 23:59:59', '朝阳区幸福社区服务中心', '北京市朝阳区幸福路100号', 116.48105, 39.996794, 50, 80, 8.0, 12, 450, 1),
(2, 2, '奥林匹克森林公园净滩公益', '保护环境，从我做起。加入我们的净滩小分队，一起清理奥森公园的河道垃圾。', '<p>环境保护活动...</p>', 'https://images.unsplash.com/photo-1518391846015-55a9cc003b25?auto=format&fit=crop&q=80&w=800', 2, '2026-04-15 14:00:00', '2026-04-15 17:00:00', '2026-03-27 00:00:00', '2026-04-12 23:59:59', '奥林匹克森林公园', '北京市朝阳区北五环林萃路', 116.39082, 40.01483, 30, 30, 3.0, 28, 890, 1),
(3, 3, '偏远地区图书捐赠与义教', '书籍是人类进步的阶梯。我们正在为山区的孩子筹集课外书籍，并寻找支教志愿者。', '<p>助学活动内容...</p>', 'https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&q=80&w=800', 2, '2026-05-01 08:00:00', '2026-05-07 18:00:00', '2026-03-27 00:00:00', '2026-04-20 23:59:59', '偏远山区希望小学', '四川省阿坝州某希望小学', 102.221, 31.899, 10, 500, 168.0, 5, 1200, 1),
(4, 1, '“智慧助老”手机使用教学', '手把手教社区老人使用微信、预约挂号等常用功能，跨越数字鸿沟。', '<p>随着智能手机的普及，许多老年人在使用中遇到了困难...</p>', NULL, 2, '2026-04-20 14:00:00', '2026-04-20 16:00:00', '2026-03-28 00:00:00', '2026-04-18 23:59:59', '长青社区活动室', '北京市朝阳区长青路22号', 116.45, 39.92, 20, 40, 2.0, 8, 210, 1),
(5, 2, '“守护蓝天”城市低碳骑行宣传', '倡导绿色出行，减少碳排放。我们将组织志愿者在主要路口发放环保宣传手册。', '<p>低碳生活，绿色出行...</p>', NULL, 2, '2026-04-22 09:00:00', '2026-04-22 12:00:00', '2026-03-28 00:00:00', '2026-04-20 23:59:59', '奥林匹克公园南门', '北京市朝阳区科荟路', 116.39, 40.01, 100, 20, 3.0, 45, 560, 1),
(6, 3, '“点亮梦想”乡村小学线上支教', '通过远程视频为乡村小学的孩子们带去精彩的美术和音乐课。', '<p>利用互联网技术，让优质教育资源触手可及...</p>', NULL, 2, '2026-05-10 19:00:00', '2026-05-10 20:30:00', '2026-03-28 00:00:00', '2026-05-05 23:59:59', '线上会议室', '远程办公', 0, 100, 1.5, 15, 890, 1),
(7, 4, '“非遗剪纸”传统文化进校园', '邀请非遗传承人走进校园，带领学生体验传统剪纸艺术的魅力。', '<p>剪纸是中国最古老的民间艺术之一...</p>', NULL, 2, '2026-05-15 14:00:00', '2026-05-15 16:00:00', '2026-03-28 00:00:00', '2026-05-12 23:59:59', '朝阳区实验小学', '北京市朝阳区工体东路', 50, 60, 2.0, 0, 120, 1),
(8, 5, '“流浪小站”救助基地义工日', '帮助救助站清理环境、给流浪狗洗澡、喂食，用爱心温暖它们。', '<p>流浪动物也需要关爱...</p>', NULL, 2, '2026-04-30 10:00:00', '2026-04-30 16:00:00', '2026-03-28 00:00:00', '2026-04-28 23:59:59', '萌宠救助中心', '北京市昌平区沙河镇', 15, 120, 6.0, 10, 430, 1);

-- 示例报名记录
INSERT INTO `activity_registration` (`activity_id`, `user_id`, `status`, `remark`) VALUES 
(1, 2, 0, '我想为老人们唱歌'),
(1, 3, 1, '我有理发技能，可以帮老人剪头发'),
(2, 2, 1, '自带清理工具'),
(2, 3, 1, '热爱自然，多次参与环保活动');

-- 示例评价
INSERT INTO `activity_review` (`activity_id`, `user_id`, `rating`, `content`, `status`, `sentiment`) VALUES 
(2, 2, 5, '活动组织得非常好，很有意义！', 1, 'positive'),
(2, 3, 4, '环境很美，大家都很热情，下次还来。', 1, 'positive');

-- 系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES 
('app.name', '阳光公益管理平台', '应用名称'),
('activity.auto_publish', 'false', '活动是否自动发布'),
('recommendation.enabled', 'true', '推荐引擎开关');

SET FOREIGN_KEY_CHECKS = 1;
