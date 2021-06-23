/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 仅允许phase 和 node 状态都不是running的情况下才能执行重跑动作
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobGoonApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "继续作业";
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
    @Description(desc = "继续作业")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        autoexecJobActionService.executeAuthCheck(jobVo);
        int sort = 0;
        /*寻找中止|暂停的phase
         * 1、优先寻找aborted|paused phase
         * 2、没有满足1条件的，再寻找pending 最小sort phase
         */
        List<AutoexecJobPhaseVo> autoexecJobPhaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseStatus(jobVo.getId(), Arrays.asList(JobPhaseStatus.ABORTED.getValue(),JobPhaseStatus.PAUSED.getValue()));
        if(CollectionUtils.isNotEmpty(autoexecJobPhaseVos)){
            sort = autoexecJobPhaseVos.get(0).getSort();
        }else{
            AutoexecJobPhaseVo phase = autoexecJobMapper.getJobPhaseByJobIdAndPhaseStatus(jobVo.getId(),JobPhaseStatus.PENDING.getValue());
            if(phase != null){
                sort = phase.getSort();
            }
        }
        jobVo.setCurrentPhaseSort(sort);
        autoexecJobService.getAutoexecJobDetail(jobVo,sort);
        autoexecJobActionService.goon(jobVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/goon";
    }
}