-- ----------------------------
-- Table structure for autoexec_catalog
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_catalog` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `parent_id` bigint NOT NULL COMMENT '父id',
  `lft` int DEFAULT NULL COMMENT '左编码',
  `rht` int DEFAULT NULL COMMENT '右编码',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_lft_rht` (`lft`,`rht`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工具目录';

-- ----------------------------
-- Table structure for autoexec_combop
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_combop` (
  `id` bigint NOT NULL COMMENT '如果从工具/脚本则直接生成combop则使用对应id，否则id自动生成',
  `uk` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `type_id` bigint NOT NULL COMMENT '类型id',
  `is_active` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态 1：启用 0：禁用',
  `operation_type` enum('script','tool','combop') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具/脚本/流水线',
  `notify_policy_id` bigint DEFAULT NULL COMMENT '通知策略id',
  `owner` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '维护人',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建用户',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改用户',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组合工具信息表';

-- ----------------------------
-- Table structure for autoexec_combop_authority
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_combop_authority` (
  `combop_id` bigint NOT NULL COMMENT '流水线id',
  `type` enum('common','role','user','team') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象类型',
  `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对象uuid',
  `action` enum('edit','execute','view') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权类型',
  PRIMARY KEY (`combop_id`,`uuid`,`action`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组合工具授权表';

-- ----------------------------
-- Table structure for autoexec_combop_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_combop_group` (
  `id` bigint NOT NULL COMMENT 'id',
  `combop_id` bigint NOT NULL COMMENT '组合工具id',
  `sort` int DEFAULT NULL COMMENT '序号',
  `policy` enum('oneShot','grayScale') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'oneShot:并发 grayScale:按批次轮询组内phase',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置信息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组合工具组';

-- ----------------------------
-- Table structure for autoexec_combop_param
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_combop_param` (
  `combop_id` bigint NOT NULL COMMENT '流水线id',
  `key` varchar(70) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中文名',
  `default_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '参数值',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `is_required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否必填 1:必填 0:选填',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文本、密码、文件、日期、节点信息、全局变量、json对象',
  `sort` int NOT NULL COMMENT '排序',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置信息',
  `editable` tinyint(1) DEFAULT NULL COMMENT '是否可编辑',
  PRIMARY KEY (`combop_id`,`key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组合工具参数表';

-- ----------------------------
-- Table structure for autoexec_combop_param_matrix
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_combop_param_matrix` (
  `combop_id` bigint NOT NULL COMMENT '流水线id',
  `key` varchar(70) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名',
  `matrix_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '矩阵uuid',
  PRIMARY KEY (`combop_id`,`key`) USING BTREE,
  KEY `idx_matrix_uuid` (`matrix_uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组合工具参数引用矩阵关系表';

-- ----------------------------
-- Table structure for autoexec_combop_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_combop_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `combop_id` bigint DEFAULT NULL COMMENT '组合工具ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `is_active` tinyint(1) DEFAULT '0' COMMENT '状态',
  `version` int DEFAULT NULL COMMENT '版本号',
  `status` enum('draft','rejected','passed','submitted') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `reviewer` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核人',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置信息',
  `lcd` timestamp NULL DEFAULT NULL COMMENT '最后修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for autoexec_customtemplate
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_customtemplate` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
  `template` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'html模板',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置，json格式',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '首次创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次创建人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最近修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义模板';

-- ----------------------------
-- Table structure for autoexec_global_param
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_global_param` (
  `id` bigint NOT NULL COMMENT '主键id',
  `key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '显示名',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `default_value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数值',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '值类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布全局参数表';

-- ----------------------------
-- Table structure for autoexec_job
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `status` enum('running','pausing','paused','completed','pending','aborting','aborted','succeed','failed','waitInput','ready','revoked','saved','checked') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作业状态',
  `plan_start_time` timestamp(3) NULL DEFAULT NULL COMMENT '计划开始时间',
  `start_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `operation_id` bigint DEFAULT NULL COMMENT '脚本/工具/流水线id',
  `operation_type` enum('script','tool','combop','pipeline') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本/工具/流水线',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作业来源',
  `round_count` smallint DEFAULT NULL COMMENT '分组数',
  `thread_count` smallint DEFAULT NULL COMMENT '执行线程数',
  `param_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '全局参数hash',
  `exec_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行用户/角色/组 uuid',
  `exec_user_type` enum('team','user','role') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行用户类型',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '流水线配置',
  `trigger_type` enum('auto','manual') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发方式(auto:自动触发；manual)',
  `lncd` timestamp(3) NULL DEFAULT NULL COMMENT '最近一次节点变动时间',
  `config_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置hash',
  `parent_id` bigint DEFAULT NULL COMMENT '父id',
  `reviewer` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核人',
  `review_status` enum('passed','failed','waiting') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核状态',
  `review_time` timestamp(3) NULL DEFAULT NULL COMMENT '审核时间',
  `scenario_id` bigint DEFAULT NULL COMMENT '场景id',
  `runner_map_id` bigint DEFAULT NULL COMMENT 'runner执行类型phase的runnerId',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_typeid` (`operation_type`) USING BTREE,
  KEY `idx_source` (`source`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业表';

-- ----------------------------
-- Table structure for autoexec_job_content
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_content` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置hash',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业参数内容表';

-- ----------------------------
-- Table structure for autoexec_job_env
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_env` (
  `job_id` bigint NOT NULL COMMENT '作业id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '环境变量名',
  `value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '环境变量值',
  PRIMARY KEY (`job_id`,`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作业环境变量表';

-- ----------------------------
-- Table structure for autoexec_job_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_group` (
  `id` bigint NOT NULL COMMENT 'id',
  `job_id` bigint NOT NULL COMMENT '作业id',
  `sort` int DEFAULT NULL COMMENT '序号',
  `policy` enum('oneShot','grayScale') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'oneShot:并发 grayScale:按批次轮询组内phase',
  `lncd` timestamp(3) NULL DEFAULT NULL COMMENT '最近一次节点变动时间',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '组配置',
  `round_count` int DEFAULT NULL COMMENT '分批数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业组';

-- ----------------------------
-- Table structure for autoexec_job_invoke
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_invoke` (
  `job_id` bigint NOT NULL COMMENT '作业id',
  `invoke_id` bigint NOT NULL COMMENT '来源id',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源',
  `type` enum('auto','deploy') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源类型',
  PRIMARY KEY (`job_id`,`invoke_id`) USING BTREE,
  KEY `idx_invoke_id` (`invoke_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业来源表';

-- ----------------------------
-- Table structure for autoexec_job_phase
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_phase` (
  `id` bigint NOT NULL COMMENT 'id',
  `job_id` bigint NOT NULL COMMENT '作业id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '剧本名',
  `status` enum('pending','running','pausing','paused','completed','failed','waiting','aborted','aborting','waitInput','ignored') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作业剧本状态',
  `start_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `exec_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行人',
  `exec_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行方式 target runner',
  `round_count` int DEFAULT NULL COMMENT '分批数',
  `sort` int NOT NULL COMMENT '阶段排序',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最近一次更新时间',
  `lncd` timestamp(3) NULL DEFAULT NULL COMMENT '最近一次节点变动时间',
  `node_from` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '节点来源',
  `user_name_from` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '用户来源',
  `protocol_from` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '协议来源',
  `group_id` bigint DEFAULT NULL COMMENT '组id',
  `execute_policy` enum('first','middl','last') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行策略',
  `warn_count` int DEFAULT NULL COMMENT '警告数量',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端uuid',
  `is_pre_output_update_node` tinyint(1) DEFAULT NULL COMMENT '是否通过上游参数更新执行目标',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_jobid_name` (`job_id`,`name`) USING BTREE,
  KEY `idx_jobid_sort` (`job_id`,`sort`) USING BTREE,
  KEY `idx_lcd` (`lcd`) USING BTREE,
  KEY `idx_jobid_groupid` (`job_id`,`group_id`) USING BTREE,
  KEY `idx_node_from` (`node_from`) USING BTREE,
  KEY `idx_user_name_from` (`user_name_from`) USING BTREE,
  KEY `idx_protocol_from` (`protocol_from`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业阶段表';

-- ----------------------------
-- Table structure for autoexec_job_phase_node
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_phase_node` (
  `id` bigint NOT NULL COMMENT 'id',
  `job_id` bigint DEFAULT NULL COMMENT '作业id',
  `job_phase_id` bigint DEFAULT NULL COMMENT '作业阶段id',
  `host` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主机ip',
  `port` int DEFAULT NULL COMMENT '服务端口号',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行目标账号',
  `resource_id` bigint DEFAULT NULL COMMENT '资源id',
  `protocol_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '协议',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '节点名（如：数据库名等）',
  `type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '节点类型',
  `start_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `status` enum('succeed','pending','failed','ignored','running','aborted','aborting','waitInput','pausing','paused') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最近一次更新时间',
  `is_delete` tinyint DEFAULT '0' COMMENT '是否已删除，目前用于重跑刷新节点',
  `warn_count` int DEFAULT NULL COMMENT '警告数量',
  `is_executed` tinyint(1) DEFAULT '0' COMMENT '是否执行过',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uni_id` (`id`) USING BTREE,
  KEY `idx_jobphaseid_status` (`job_phase_id`,`status`) USING BTREE,
  KEY `idx_jobid` (`job_id`) USING BTREE,
  KEY `idx_host_port` (`host`) USING BTREE,
  KEY `idx_lcd` (`lcd`) USING BTREE,
  KEY `idx_resource_id_job_phase_id` (`resource_id`,`job_phase_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业阶段节点表';

-- ----------------------------
-- Table structure for autoexec_job_phase_node_runner
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_phase_node_runner` (
  `job_id` bigint DEFAULT NULL COMMENT '作业id',
  `job_phase_id` bigint DEFAULT NULL COMMENT '作业剧本id',
  `node_id` bigint NOT NULL COMMENT '作业剧本节点id',
  `runner_map_id` bigint NOT NULL COMMENT '作业剧本节点runner 映射id',
  PRIMARY KEY (`node_id`,`runner_map_id`) USING BTREE,
  KEY `idx_phaseId` (`job_phase_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业阶段节点runner表';

-- ----------------------------
-- Table structure for autoexec_job_phase_operation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_phase_operation` (
  `job_id` bigint NOT NULL COMMENT '作业id',
  `job_phase_id` bigint NOT NULL COMMENT '作业阶段id',
  `operation_id` bigint DEFAULT NULL COMMENT '作业/工具id',
  `id` bigint NOT NULL COMMENT '作业脚本/工具id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本/工具名',
  `letter` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本/工具名后缀',
  `type` enum('script','tool') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本/工具',
  `parser` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '解析器',
  `exec_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行方式',
  `fail_policy` enum('stop','goon') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '失败策略：ignore失败忽略   stop失败中止',
  `param_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数hash',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `version_id` bigint DEFAULT NULL COMMENT '脚本版本id',
  `profile_id` bigint DEFAULT NULL COMMENT '预设参数集id',
  `parent_operation_id` bigint DEFAULT NULL COMMENT '父工具id',
  `parent_operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父工具类型,如if-block的if|else',
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端工具uuid',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_jobid_operationId` (`job_id`,`operation_id`) USING BTREE,
  KEY `idx_phaseid_opid` (`job_phase_id`,`operation_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业阶段操作表';

-- ----------------------------
-- Table structure for autoexec_job_phase_runner
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_phase_runner` (
  `job_id` bigint DEFAULT NULL COMMENT '作业id',
  `job_group_id` bigint DEFAULT NULL COMMENT '作业组id',
  `job_phase_id` bigint NOT NULL COMMENT '作业剧本id',
  `runner_map_id` bigint NOT NULL COMMENT '作业剧本runner 映射id',
  `status` enum('pending','completed','failed','paused','aborted','running','aborting','pausing','waitInput','ignored') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'pending' COMMENT '状态',
  `is_fire_next` tinyint(1) DEFAULT '0' COMMENT '是否激活firenext',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最近一次更新时间',
  `warn_count` int DEFAULT NULL COMMENT '警告数量',
  PRIMARY KEY (`job_phase_id`,`runner_map_id`) USING BTREE,
  KEY `idx_lcd` (`lcd`) USING BTREE,
  KEY `idx_job_id` (`job_id`) USING BTREE,
  KEY `idx_runner_map_id` (`runner_map_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业阶段runner表';

-- ----------------------------
-- Table structure for autoexec_job_resource_inspect
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_resource_inspect` (
  `resource_id` bigint NOT NULL COMMENT '资产id',
  `job_id` bigint DEFAULT NULL COMMENT '作业id',
  `phase_id` bigint DEFAULT NULL COMMENT '作业阶段id',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '巡检时间',
  PRIMARY KEY (`resource_id`) USING BTREE,
  KEY `index_jobid_phaseId` (`job_id`,`phase_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化作业资源巡检';

-- ----------------------------
-- Table structure for autoexec_job_sql_detail
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_job_sql_detail` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `status` enum('pending','running','aborting','aborted','succeed','failed','ignored','waitInput') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `sql_file` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'sql文件名称',
  `md5` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'sql文件md5',
  `job_id` bigint DEFAULT NULL COMMENT '作业 id',
  `runner_id` bigint DEFAULT NULL COMMENT 'runner id',
  `resource_id` bigint DEFAULT NULL COMMENT '资产 id',
  `node_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作业节点名',
  `job_phase_id` bigint DEFAULT NULL COMMENT '作业剧本id',
  `job_phase_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作业剧本名称',
  `is_delete` tinyint DEFAULT NULL COMMENT '是否已删除',
  `update_tag` bigint DEFAULT NULL COMMENT '修改时间',
  `start_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `host` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ip',
  `port` int DEFAULT NULL COMMENT '端口',
  `node_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '节点类型',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `sort` int DEFAULT NULL COMMENT '排序',
  `is_modified` int DEFAULT NULL COMMENT '是否改动',
  `warn_count` int DEFAULT NULL COMMENT '告警个数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_resource_id_job_id_job_phase_name_sql_file` (`resource_id`,`job_id`,`job_phase_name`,`sql_file`) USING BTREE,
  KEY `idx_job_id_job_phase_name` (`job_id`,`job_phase_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化sql详情表';

-- ----------------------------
-- Table structure for autoexec_operation_generate_combop
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_operation_generate_combop` (
  `combop_id` bigint NOT NULL COMMENT '组合工具id',
  `operation_type` enum('script','tool') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工具或自定义工具',
  `operation_id` bigint DEFAULT NULL COMMENT '工具id或自定义工具id',
  PRIMARY KEY (`combop_id`) USING BTREE,
  KEY `idx_operation_id` (`operation_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组合工具关联表';

-- ----------------------------
-- Table structure for autoexec_profile
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_profile` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '工具参数',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `system_id` bigint NOT NULL COMMENT '所属系统id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY ` idx_system_id` (`system_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布工具profile表';

-- ----------------------------
-- Table structure for autoexec_profile_cientity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_profile_cientity` (
  `ci_entity_id` bigint NOT NULL COMMENT '配置项id',
  `profile_id` bigint NOT NULL COMMENT ' id',
  PRIMARY KEY (`ci_entity_id`,`profile_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布工具profile配置项关系表';

-- ----------------------------
-- Table structure for autoexec_profile_operation
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_profile_operation` (
  `operation_id` bigint NOT NULL COMMENT '工具库工具id/自定义工具id',
  `profile_id` bigint NOT NULL COMMENT 'profile id',
  `type` enum('script','tool') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工具类型',
  `update_tag` bigint DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`operation_id`,`profile_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发布工具profile和tool、script关系表';

-- ----------------------------
-- Table structure for autoexec_profile_param
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_profile_param` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `profile_id` bigint DEFAULT NULL COMMENT 'profile id',
  `key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'key',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
  `default_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '默认值',
  `operation_id` bigint DEFAULT NULL COMMENT 'key来源的工具id',
  `operation_type` enum('script','tool') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'key来源的工具类型',
  `update_tag` bigint DEFAULT NULL COMMENT '修改时间',
  `mapping_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '值引用类型',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_profile_id_key` (`profile_id`,`key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化profile参数表';

-- ----------------------------
-- Table structure for autoexec_profile_param_value_invoke
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_profile_param_value_invoke` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `profile_param_id` bigint NOT NULL COMMENT 'profile 参数id',
  `value_invoke_id` bigint DEFAULT NULL COMMENT '引用参数id（如：全局参数）',
  `value_invoke_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '值引用类型',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_profile_param_id_value_invoke_id_value_invoke_type` (`profile_param_id`,`value_invoke_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化profile参数值引用表';

-- ----------------------------
-- Table structure for autoexec_risk
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_risk` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '状态是否启用 1：启用 0：禁用',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `sort` int DEFAULT NULL COMMENT '排序',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建用户',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改用户',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化操作级别表';

-- ----------------------------
-- Table structure for autoexec_scenario
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_scenario` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_lcd` (`lcd`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化场景定义表';

-- ----------------------------
-- Table structure for autoexec_scenario_cientity
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_scenario_cientity` (
  `scenario_id` bigint NOT NULL COMMENT '场景 id',
  `ci_entity_id` bigint NOT NULL COMMENT '应用 id',
  PRIMARY KEY (`ci_entity_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化场景应用关系表';

-- ----------------------------
-- Table structure for autoexec_schedule
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_schedule` (
  `id` bigint NOT NULL COMMENT '全局唯一id，跨环境导入用',
  `uuid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '全局唯一uuid',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `autoexec_combop_id` bigint NOT NULL COMMENT '组合工具id',
  `begin_time` timestamp(3) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp(3) NULL DEFAULT NULL COMMENT '结束时间',
  `cron` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'corn表达式',
  `is_active` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0:禁用，1:激活',
  `config` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行配置信息',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建用户',
  `lcd` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修改用户',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时作业信息表';

-- ----------------------------
-- Table structure for autoexec_script
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script` (
  `id` bigint NOT NULL COMMENT 'id',
  `uk` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识(英文名)',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `type_id` bigint DEFAULT NULL COMMENT '分类id',
  `catalog_id` bigint DEFAULT NULL COMMENT '工具目录id',
  `exec_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行方式',
  `risk_id` bigint DEFAULT NULL COMMENT '操作级别id',
  `customtemplate_id` bigint DEFAULT NULL COMMENT '自定义模版id',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `default_profile_id` bigint DEFAULT NULL COMMENT '默认profile id',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `is_lib` int DEFAULT '0' COMMENT '是否库文件',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE,
  KEY `idx_type_id` (`type_id`) USING BTREE,
  KEY `idx_catalog_id` (`catalog_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具表';

-- ----------------------------
-- Table structure for autoexec_script_audit
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_audit` (
  `id` bigint NOT NULL COMMENT '主键',
  `script_id` bigint DEFAULT NULL COMMENT '脚本ID',
  `script_version_id` bigint DEFAULT NULL COMMENT '脚本版本ID',
  `operate` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作',
  `content_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动内容hash',
  `fcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `fcd` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_script_id` (`script_id`) USING BTREE,
  KEY `idx_script_version_id` (`script_version_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具活动表';

-- ----------------------------
-- Table structure for autoexec_script_audit_detail
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_audit_detail` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'hash',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '内容',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具活动内容表';

-- ----------------------------
-- Table structure for autoexec_script_line
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_line` (
  `id` bigint NOT NULL COMMENT 'id',
  `script_id` bigint DEFAULT NULL COMMENT '脚本id',
  `script_version_id` bigint DEFAULT NULL COMMENT '脚本版本id',
  `content_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本内容hash',
  `line_number` int DEFAULT NULL COMMENT '脚本内容行号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_script_id` (`script_id`) USING BTREE,
  KEY `idx_version_id` (`script_version_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具内容行表';

-- ----------------------------
-- Table structure for autoexec_script_line_content
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_line_content` (
  `hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容hash值',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '行脚本内容',
  PRIMARY KEY (`hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具内容表';

-- ----------------------------
-- Table structure for autoexec_script_validate
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_validate` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '危险代码',
  `level` enum('warning','critical') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '等级',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改用户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具危险代码表';

-- ----------------------------
-- Table structure for autoexec_script_validate_type
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_validate_type` (
  `id` bigint NOT NULL COMMENT 'id',
  `validate_id` bigint DEFAULT NULL COMMENT '高危代码id',
  `script_type` enum('xml','python','vbs','shell','perl','powershell','bat') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具危险代码类型表';

-- ----------------------------
-- Table structure for autoexec_script_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_version` (
  `id` bigint NOT NULL COMMENT 'id',
  `script_id` bigint DEFAULT NULL COMMENT '脚本id',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标题',
  `version` int DEFAULT NULL COMMENT '版本号',
  `encoding` enum('UTF-8','GBK') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本编码',
  `parser` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '脚本解析器',
  `status` enum('draft','rejected','passed','submitted') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'draft:编辑中、rejected:已驳回、passed:已通过、submitted:待审批',
  `reviewer` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '脚本配置信息',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_scriptid_version` (`script_id`,`version`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具版本表';

-- ----------------------------
-- Table structure for autoexec_script_version_argument
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_version_argument` (
  `script_version_id` bigint NOT NULL COMMENT '脚本版本id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名',
  `default_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '默认值',
  `argument_count` int DEFAULT NULL COMMENT '数量限制',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `is_required` tinyint(1) DEFAULT NULL COMMENT '是否必填',
  PRIMARY KEY (`script_version_id`,`name`) USING BTREE,
  UNIQUE KEY `idx_script_version_id` (`script_version_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义工具库自由参数';

-- ----------------------------
-- Table structure for autoexec_script_version_lib
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_version_lib` (
  `script_version_id` bigint NOT NULL COMMENT '脚本版本id',
  `lib_script_id` bigint NOT NULL COMMENT '依赖的脚本id',
  PRIMARY KEY (`script_version_id`,`lib_script_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自动化脚本依赖表';

-- ----------------------------
-- Table structure for autoexec_script_version_param
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_script_version_param` (
  `script_version_id` bigint NOT NULL COMMENT '脚本版本id',
  `key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数名',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中文名',
  `default_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '参数默认值',
  `mode` enum('output','input') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数类型：出参、入参',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文本、密码、文件、日期、节点信息、全局变量、json对象、文件路径',
  `mapping_mode` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数映射模式',
  `is_required` tinyint(1) DEFAULT NULL COMMENT '是否必填(1:是；0:否)',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `sort` int DEFAULT NULL COMMENT '排序',
  `config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置信息',
  PRIMARY KEY (`script_version_id`,`key`,`mode`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化自定义工具参数表';

-- ----------------------------
-- Table structure for autoexec_service
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_service` (
  `id` bigint NOT NULL,
  `name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `is_active` tinyint(1) NOT NULL,
  `type` enum('service','catalog') COLLATE utf8mb4_general_ci NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `lft` int DEFAULT NULL,
  `rht` int DEFAULT NULL,
  `description` mediumtext COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_name` (`name`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_lft` (`lft`),
  KEY `idx_rht` (`rht`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for autoexec_service_authority
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_service_authority` (
  `service_id` bigint NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `uuid` char(32) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`service_id`,`type`,`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for autoexec_service_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_service_config` (
  `service_id` bigint NOT NULL,
  `combop_id` bigint NOT NULL,
  `form_uuid` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config` mediumtext COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for autoexec_service_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_service_user` (
  `service_id` bigint NOT NULL,
  `user_uuid` char(32) COLLATE utf8mb4_general_ci NOT NULL,
  `lcd` timestamp NOT NULL,
  PRIMARY KEY (`user_uuid`,`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for autoexec_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_tag` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化标签表';

-- ----------------------------
-- Table structure for autoexec_tool
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_tool` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `is_active` tinyint(1) DEFAULT NULL COMMENT '是否激活',
  `exec_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行方式',
  `type_id` bigint DEFAULT NULL COMMENT '分类id',
  `risk_id` bigint DEFAULT NULL COMMENT '操作级别id',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述说明',
  `parser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '解析器',
  `customtemplate_id` bigint DEFAULT NULL COMMENT '自定义模版id',
  `default_profile_id` bigint DEFAULT NULL COMMENT '默认profile id',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '参数配置',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unique_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化工具表';

-- ----------------------------
-- Table structure for autoexec_type
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_type` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类名',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '描述',
  `lcd` timestamp(3) NULL DEFAULT NULL COMMENT '最后修改时间',
  `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后修改人',
  `type` enum('factory','custom') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'custom' COMMENT '类型',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化类型表';

-- ----------------------------
-- Table structure for autoexec_type_authority
-- ----------------------------
CREATE TABLE IF NOT EXISTS `autoexec_type_authority` (
  `type_id` bigint NOT NULL COMMENT '工具类型id',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型',
  `auth_type` enum('common','user','team','role') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限类型\n',
  `auth_uuid` varbinary(255) NOT NULL COMMENT '权限Uuid',
  PRIMARY KEY (`type_id`,`auth_uuid`,`auth_type`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自动化分类权限表';