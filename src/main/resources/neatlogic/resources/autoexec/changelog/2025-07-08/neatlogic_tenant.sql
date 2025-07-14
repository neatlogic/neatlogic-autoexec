ALTER TABLE `autoexec_job_group`
ADD COLUMN `parallel_count` int NULL COMMENT '并发数' AFTER `round_count`;

ALTER TABLE `autoexec_job_group`
ADD COLUMN `parallel_policy` varchar(50) NULL COMMENT '并发策略' AFTER `parallel_count`;