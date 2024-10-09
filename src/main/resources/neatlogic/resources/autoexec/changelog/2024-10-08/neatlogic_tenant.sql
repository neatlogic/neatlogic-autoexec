CREATE TABLE `runnergroup_tag`  (
  `group_id` bigint NOT NULL COMMENT '执行器组id',
  `tag_id` varchar(255) NOT NULL COMMENT '标签id',
  PRIMARY KEY (`group_id`, `tag_id`)
) COMMENT = '执行器组标签';

ALTER TABLE `tag`
ADD COLUMN `type` varchar(50) NULL COMMENT '类型' AFTER `name`,
ADD UNIQUE INDEX `uniq_nametype`(`type`, `name`) USING BTREE;