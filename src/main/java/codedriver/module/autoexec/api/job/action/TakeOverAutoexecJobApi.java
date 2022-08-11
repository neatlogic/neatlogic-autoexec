/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobSource;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobTakeOverPermissionDeniedException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author laiwt
 * @since 2022/3/24 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class TakeOverAutoexecJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return "接管作业";
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
    @Description(desc = "接管作业")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByJobId(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (Objects.equals(jobVo.getSource(), JobSource.TEST.getValue()) && !AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class)) {
            throw new AutoexecJobTakeOverPermissionDeniedException(jobId);
        } else if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
            if (combopVo == null) {
                throw new AutoexecCombopNotFoundException(jobVo.getOperationId());
            }
            if (!autoexecCombopService.checkOperableButton(combopVo, CombopAuthorityAction.EXECUTE)) {
                throw new AutoexecJobTakeOverPermissionDeniedException(jobId);
            }
        }
        jobVo.setExecUser(UserContext.get().getUserUuid());
        autoexecJobMapper.updateJobExecUser(jobVo);
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/takeover";
    }
}
