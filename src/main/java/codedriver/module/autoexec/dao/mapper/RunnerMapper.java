/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.GroupNetworkVo;
import codedriver.framework.autoexec.dto.RunnerGroupVo;
import codedriver.framework.autoexec.dto.RunnerMapVo;
import codedriver.framework.autoexec.dto.RunnerVo;

import java.util.List;

public interface RunnerMapper {

    List<GroupNetworkVo> getAllNetworkMask();

    RunnerGroupVo getRunnerGroupById(Long groupId);

    List<RunnerMapVo> getAllRunnerMap();

    RunnerVo getRunnerById(Integer runnerId);

    Integer insertRunnerMap(RunnerMapVo runnerMapVo);
}
