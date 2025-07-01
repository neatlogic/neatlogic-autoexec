ALTER TABLE `autoexec_job_phase`
ADD COLUMN `runner_group_from` varchar(50) NULL COMMENT '执行器组来源' AFTER `protocol_from`;