/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.callback;

import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
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
public class AutoexecJobPhaseStatusUpdateApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "回调更新作业剧本或节点状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "status", type = ApiParamType.STRING, desc = "状态", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数")
    })
    @Output({
    })
    @Description(desc = "回调更新作业剧本或节点状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        String phaseName = jsonObj.getString("phase");
        String status = jsonObj.getString("status");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if(jobVo == null){
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseLockByJobIdAndPhaseName(jobId, phaseName);
        if (jobPhaseVo == null) {
            throw new AutoexecJobPhaseNotFoundException(jobId+":"+phaseName);
        }
        autoexecJobMapper.updateJobPhaseStatus(new AutoexecJobPhaseVo(jobPhaseVo.getId(), status));
        /*if(Objects.equals(status, JobPhaseStatus.FAILED.getValue())){
            result.put("hasFailNode",1);
        }else{
            if(autoexecJobMapper.checkIsHasActivePhaseFailed(jobId)>0){
                result.put("hasFailNode",1);
            }
        }*/
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/status/update";
    }
}
