/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobSource;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecOperationHasNoModifyAuthException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 仅允许phase 和 node 状态都不是running的情况下才能执行重跑动作
 *
 * @author lvzk
 * @since 2021/6/2 15:20
 **/

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecJobReFireApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "重跑作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "refireResetAll,refireAll", desc = "重跑类型：   重置并重跑所有：refireResetAll；重跑所有：refireAll", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "重跑作业")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String type = jsonObj.getString("type");

        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        if(Objects.equals(jobVo.getSource(), JobSource.TEST.getValue())){//测试仅需判断是否有脚本维护权限即可
            if(!AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class)){
               throw new AutoexecOperationHasNoModifyAuthException();
            }
        }else {
            autoexecJobActionService.executeAuthCheck(jobVo);
        }
        jobVo.setAction(JobAction.REFIRE.getValue());
        autoexecJobActionService.refire(jobVo,type);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/refire";
    }
}
