ALTER TABLE `autoexec_job_phase`
    CHANGE `execute_policy` `execute_policy` ENUM ('first', 'middle', 'last') CHARSET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '执行策略';