ALTER TABLE `autoexec_catalog`
ADD COLUMN `upward_name_path` varchar(768) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所有父节点name路径' AFTER `rht`,
ADD COLUMN `upward_id_path` varchar(768) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所有父节点id路径' AFTER `upward_name_path`,
ADD INDEX `idx_upwardnamepath`(`upward_name_path`) USING BTREE,
ADD INDEX `idx_rgtlft`(`rht`, `lft`) USING BTREE;