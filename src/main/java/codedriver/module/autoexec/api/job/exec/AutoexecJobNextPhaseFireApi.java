/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.exception.user.UserNotFoundException;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/14 14:15
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobNextPhaseFireApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    UserMapper userMapper;

    @Override
    public String getName() {
        return "激活作业下一阶段剧本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "lastPhase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数", isRequired = true),
            @Param(name = "time", type = ApiParamType.DOUBLE, desc = "回调时间")
    })
    @Output({
    })
    @Description(desc = "激活作业下一阶段剧本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String lastPhase = jsonObj.getString("lastPhase");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        JSONObject passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
        Long runnerId = null;
        if (MapUtils.isEmpty(passThroughEnv)) {
            throw new ParamIrregularException("passThroughEnv");
        }
        if (!passThroughEnv.containsKey("runnerId")) {
            throw new AutoexecJobRunnerNotFoundException("runnerId");
        } else {
            runnerId = passThroughEnv.getLong("runnerId");
        }

        //初始化执行用户上下文
        UserVo execUser = userMapper.getUserBaseInfoByUuid(jobVo.getExecUser());
        if (execUser == null) {
            throw new UserNotFoundException(jobVo.getExecUser());
        }
        UserContext.init(execUser, "+8:00");
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(execUser).getCc());
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseLockByJobIdAndPhaseName(jobId, lastPhase);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId + ":" + lastPhase);
        }
        autoexecJobMapper.updateJobPhaseRunnerFireNextByPhaseId(jobPhaseVo.getId(), 1, runnerId);
        /*
         *判断是否满足激活下个phase条件
         * 1、当前sort的所有phase都completed
         * 2、当前sort的所有phase的runner 都是completed，所有runner都触发了fireNext
         */
        if (autoexecJobService.checkIsAllActivePhaseIsCompleted(jobId, jobPhaseVo.getSort())) {
            Integer sort = autoexecJobMapper.getNextJobPhaseSortByJobId(jobId, jobPhaseVo.getSort());
            if (sort != null) {
                autoexecJobService.getAutoexecJobDetail(jobVo, sort);
                jobVo.setCurrentGroupSort(sort);
                IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
                fireAction.doService(jobVo);
            }
            /*
            暂时注释，因为如果是所有phase都完成也会update phase status 为completed，防止update两次，导致callback两次
            else {//说明没有可以执行的phase，更新job 状态为 completed
            autoexecJobMapper.updateJobStatus(new AutoexecJobVo(jobId, JobStatus.COMPLETED.getValue()));
            }
            */
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/next/phase/fire";
    }
}
