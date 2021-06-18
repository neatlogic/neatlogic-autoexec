/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.AutoexecRunnerGroupNetworkVo;
import codedriver.framework.autoexec.dto.AutoexecRunnerGroupVo;
import codedriver.framework.autoexec.dto.AutoexecRunnerMapVo;
import codedriver.framework.autoexec.dto.AutoexecRunnerVo;

import java.util.List;

public interface AutoexecRunnerMapper {

    List<AutoexecRunnerGroupNetworkVo> getAllNetworkMask();

    AutoexecRunnerGroupVo getRunnerGroupById(Long groupId);

    List<AutoexecRunnerMapVo> getAllRunnerMap();

    AutoexecRunnerVo getRunnerById(Integer runnerId);

    Integer insertRunnerMap(AutoexecRunnerMapVo autoexecRunnerMapVo);
}
