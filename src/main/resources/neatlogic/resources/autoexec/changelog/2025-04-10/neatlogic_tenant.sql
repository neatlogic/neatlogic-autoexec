ALTER TABLE `autoexec_job`
MODIFY COLUMN `status` enum('running','pausing','paused','completed','pending','aborting','aborted','succeed','failed','waitInput','ready','revoked','saved','checked','waiting') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '作业状态' AFTER `name`;

ALTER TABLE `autoexec_job_phase_node`
MODIFY COLUMN `status` enum('succeed','pending','failed','ignored','running','aborted','aborting','waitInput','pausing','paused','invalid','waiting') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态' AFTER `end_time`;

ALTER TABLE `autoexec_job_phase_runner`
MODIFY COLUMN `status` enum('pending','completed','failed','paused','aborted','running','aborting','pausing','waitInput','ignored','waiting') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'pending' COMMENT '状态' AFTER `runner_map_id`;