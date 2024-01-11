ALTER TABLE `autoexec_combop`
ADD COLUMN `op_type` varchar(50) NULL COMMENT '操作类型' AFTER `type_id`;