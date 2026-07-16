CREATE TABLE IF NOT EXISTS `autoexec_script_execrtool_authority` (
  `operation_type` varchar(20) NOT NULL COMMENT '执行对象类型：script/tool',
  `operation_id` bigint NOT NULL COMMENT '执行对象id',
  `type` varchar(20) NOT NULL COMMENT '授权对象类型: common/user/team/role',
  `uuid` varchar(64) NOT NULL COMMENT '授权对象uuid，所有人为alluser',
  PRIMARY KEY (`operation_type`,`operation_id`,`type`,`uuid`),
  KEY `idx_type_uuid` (`type`,`uuid`),
  KEY `idx_operation` (`operation_type`,`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脚本execrtool执行授权';
