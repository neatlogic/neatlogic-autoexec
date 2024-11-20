ALTER TABLE `autoexec_job_phase_node_runner` DROP PRIMARY KEY,ADD PRIMARY KEY (`job_phase_id`, `node_id`) USING BTREE;

ALTER TABLE `autoexec_job_phase_node_runner` DROP INDEX `idx_phaseId`,ADD INDEX `idx_nodeId`(`node_id`) USING BTREE;

ALTER TABLE `autoexec_job_phase_node` DROP INDEX `uni_id`;

ALTER TABLE `autoexec_job_phase_node` DROP INDEX `idx_host_port`;

ALTER TABLE `autoexec_job_phase_node` DROP INDEX `idx_lcd`;

ALTER TABLE `autoexec_job_phase_node` DROP INDEX `idx_resource_id_job_phase_id`;

ALTER TABLE `autoexec_job_phase_node` ADD INDEX `idx_phaseid_updatetag`(`job_phase_id`, `update_tag`) USING BTREE;

ALTER TABLE `autoexec_job_phase_node` ADD COLUMN `runner_map_id` bigint NULL COMMENT '执行器id' AFTER `is_executed`;

ALTER TABLE `autoexec_job_phase_node` ADD COLUMN `update_tag` bigint NULL COMMENT '最近一次更新标记' AFTER `runner_map_id`;

ALTER TABLE `autoexec_job_phase_node` ADD UNIQUE INDEX `idx_phaseid_resourceid`(`job_phase_id`, `resource_id`) USING BTREE;

ALTER TABLE `autoexec_job_phase_node` ADD COLUMN `error_type` int NULL COMMENT '异常信息' AFTER `update_tag`;

ALTER TABLE `autoexec_job_phase_node` MODIFY COLUMN `status` enum('succeed','pending','failed','ignored','running','aborted','aborting','waitInput','pausing','paused','error') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态' AFTER `end_time`;