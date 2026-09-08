-- Existing global name uniqueness already guarantees directory/name uniqueness. No data migration is needed.
ALTER TABLE `autoexec_script` ADD UNIQUE INDEX `uk_catalog_name` (`catalog_id`, `name`);
ALTER TABLE `autoexec_script` DROP INDEX `uk_name`;
