/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.group.policy;

import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.group.policy.core.AutoexecJobGroupPolicyHandlerBase;
import codedriver.framework.dto.runner.RunnerMapVo;

import java.util.List;

public class AutoexecJobOneShotHandler extends AutoexecJobGroupPolicyHandlerBase {
    @Override
    public String getName() {
        return "";
    }

    @Override
    public List<AutoexecJobPhaseVo> getFirstExecutePhaseList(AutoexecJobVo jobVo) {
        return null;
    }

    @Override
    public List<RunnerMapVo> getExecuteRunnerList(AutoexecJobVo jobVo) {
        return null;
    }
}
