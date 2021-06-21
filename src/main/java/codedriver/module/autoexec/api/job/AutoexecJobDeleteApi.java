/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_JOB_MODIFY;
import codedriver.framework.autoexec.dto.job.AutoexecJobParamContentVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lvzk
 * @since 2021/4/21 11:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_JOB_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecJobDeleteApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Override
    public String getName() {
        return "删除作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
    })
    @Output({
    })
    @Description(desc = "删除作业")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        autoexecJobActionService.executeAuthCheck(jobVo);
        //删除jobParamContent
        Set<String> hashSet = new HashSet<>();
        hashSet.add(jobVo.getParamHash());
        List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobId(jobId);
        for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
            hashSet.add(operationVo.getParamHash());
        }
        for (String hash : hashSet) {
            AutoexecJobParamContentVo paramContentVo = autoexecJobMapper.getJobParamContentLock(hash);
            if(paramContentVo != null) {
                int jobParamReferenceCount = autoexecJobMapper.checkIsJobParamReference(jobId, hash);
                int jobPhaseOperationParamReferenceCount = autoexecJobMapper.checkIsJobPhaseOperationParamReference(jobId, hash);
                if (jobParamReferenceCount == 0 && jobPhaseOperationParamReferenceCount == 0) {
                    autoexecJobMapper.deleteJobParamContentByHash(hash);
                }
            }
        }
        //else
        autoexecJobMapper.deleteJobPhaseRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseNodeRunnerByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseOperationByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseNodeByJobId(jobId);
        autoexecJobMapper.deleteJobPhaseByJobId(jobId);
        autoexecJobMapper.deleteJobByJobId(jobId);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/delete";
    }
}
