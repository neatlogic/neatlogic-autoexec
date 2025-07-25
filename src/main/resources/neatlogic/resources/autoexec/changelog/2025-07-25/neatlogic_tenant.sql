ALTER TABLE `runnergroup_tag`
MODIFY COLUMN `tag_id` bigint NOT NULL COMMENT '标签id' AFTER `group_id`;