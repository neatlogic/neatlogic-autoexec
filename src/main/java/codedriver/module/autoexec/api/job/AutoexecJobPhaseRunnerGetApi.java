/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/6/2 10:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseRunnerGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取作业runner执行方式剧本的runner信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "作业剧本id", isRequired = true)
    })
    @Output({
            @Param(explode = RunnerVo.class,desc = "runner信息")
    })
    @Description(desc = "获取作业runner执行方式剧本的runner信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long jobPhaseId = jsonObj.getLong("jobPhaseId");
        AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseRunnerNodeByJobIdAndPhaseId(jobId,jobPhaseId);
        if(nodeVo == null){
            throw  new AutoexecJobRunnerNotFoundException(StringUtils.EMPTY);
        }
        nodeVo.setRunnerVo(autoexecJobMapper.getJobRunnerById(nodeVo.getRunnerId()));
        return nodeVo;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/runner/get";
    }
}
