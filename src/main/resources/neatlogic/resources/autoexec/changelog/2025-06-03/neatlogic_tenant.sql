CREATE TABLE `autoexec_job_runner` (
  `job_id` bigint NOT NULL COMMENT '作业id',
  `runner_map_id` bigint NOT NULL COMMENT '执行器id',
  `status` enum('pending','completed','failed','aborted','paused') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'pending' COMMENT '执行器状态',
  `lcd` bigint DEFAULT NULL COMMENT '最后修改时间',
  PRIMARY KEY (`job_id`,`runner_map_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作业runner状态';


CREATE TABLE `autoexec_job_exec` (
  `job_id` bigint NOT NULL COMMENT '作业id',
  `runner_map_id` bigint NOT NULL COMMENT '执行器id',
  `exec_id` bigint NOT NULL COMMENT '执行id',
  `action` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行动作',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (`job_id`,`runner_map_id`,`exec_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作业实时执行记录，用于判断是否有正在进行中的进程';