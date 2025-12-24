INSERT INTO `autoexec_combop_authority` (`combop_id`, `type`, `uuid`, `action`)
SELECT `id`, 'common', 'alluser', 'view' FROM `autoexec_combop`
WHERE `id` NOT IN (SELECT `combop_id` FROM `autoexec_combop_authority`);
