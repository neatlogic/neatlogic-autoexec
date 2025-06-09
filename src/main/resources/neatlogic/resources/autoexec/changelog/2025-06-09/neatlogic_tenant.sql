ALTER TABLE `autoexec_job_phase_runner`
DROP INDEX `idx_job_id`;

ALTER TABLE `autoexec_job_phase_runner`
ADD INDEX `idx_jobid_status`(`job_id`, `status`) USING BTREE;