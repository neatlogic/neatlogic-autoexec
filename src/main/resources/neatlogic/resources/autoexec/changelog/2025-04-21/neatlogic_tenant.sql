ALTER TABLE `autoexec_job_phase`
ADD COLUMN `user_name` varchar(50) NULL COMMENT '执行用户' AFTER `exec_mode`;

ALTER TABLE `autoexec_job_phase`
ADD COLUMN `protocol` varchar(50) NULL COMMENT '执行协议' AFTER `user_name`;

ALTER TABLE `autoexec_job_phase`
ADD COLUMN `round_count_from` varchar(50) NULL COMMENT '分批数来源' AFTER `node_from`;