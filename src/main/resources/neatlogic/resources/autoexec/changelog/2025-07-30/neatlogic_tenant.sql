ALTER TABLE `autoexec_job_group`
ADD COLUMN `parallel_from` varchar(50) NULL COMMENT '并发策略来自' AFTER `parallel_policy`;