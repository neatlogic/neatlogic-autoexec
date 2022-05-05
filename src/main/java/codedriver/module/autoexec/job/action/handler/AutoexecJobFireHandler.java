/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobFireHandler extends AutoexecJobActionHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return JobAction.FIRE.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        //List<AutoexecJobPhaseVo> executePhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndGroupSort(jobVo.getId(),jobVo.getExecuteJobGroupVo().getSort());
        //return Objects.equals(JobStatus.PENDING.getValue(), jobVo.getStatus()) || executePhaseVoList.stream().allMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.PENDING.getValue()));
        return true;
    }

    @Override
    public boolean isNeedExecuteAuthCheck(){
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        autoexecJobMapper.getJobLockByJobId(jobVo.getId());
        executeGroup(jobVo);
        return new JSONObject(){{
            put("jobId",jobVo.getId());
        }};
    }
}
