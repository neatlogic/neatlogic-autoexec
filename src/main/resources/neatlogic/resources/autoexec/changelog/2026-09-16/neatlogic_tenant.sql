CREATE TABLE IF NOT EXISTS `autoexec_job_operation_audit` (
  `id` bigint NOT NULL COMMENT '记录 ID',
  `job_id` bigint DEFAULT NULL COMMENT '作业 ID',
  `operate_time` bigint DEFAULT NULL COMMENT '操作时间',
  `operator_uuid` varchar(255) DEFAULT NULL COMMENT '操作人 ID',
  `action` varchar(255) DEFAULT NULL COMMENT '操作类型',
  `object_type` varchar(255) DEFAULT NULL COMMENT '对象类型',
  `target_name` varchar(1024) DEFAULT NULL COMMENT '目标对象',
  `strategy` varchar(255) DEFAULT NULL COMMENT '执行策略',
  `previous_exec_user` varchar(255) DEFAULT NULL COMMENT '原执行人',
  `current_exec_user` varchar(255) DEFAULT NULL COMMENT '新执行人',
  `interaction_type` varchar(255) DEFAULT NULL COMMENT '交互类型',
  `interaction_value` text DEFAULT NULL COMMENT '交互选择',
  `target_count` int DEFAULT NULL COMMENT '目标数量',
  PRIMARY KEY (`id`),
  KEY `idx_job_time` (`job_id`,`operate_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业业务操作记录';

CREATE TABLE IF NOT EXISTS `autoexec_job_operation_audit_target` (
  `id` bigint NOT NULL COMMENT '记录 ID',
  `audit_id` bigint DEFAULT NULL COMMENT '操作记录 ID',
  `object_id` bigint DEFAULT NULL COMMENT '对象 ID',
  `phase_id` bigint DEFAULT NULL COMMENT '阶段 ID',
  `phase_name` varchar(255) DEFAULT NULL COMMENT '阶段名称',
  `resource_id` bigint DEFAULT NULL COMMENT '资源 ID',
  `node_name` varchar(255) DEFAULT NULL COMMENT '节点名称',
  `host` varchar(255) DEFAULT NULL COMMENT 'IP',
  `sql_file` varchar(1024) DEFAULT NULL COMMENT 'SQL 文件',
  PRIMARY KEY (`id`),
  KEY `idx_audit_id` (`audit_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业业务操作目标快照';
