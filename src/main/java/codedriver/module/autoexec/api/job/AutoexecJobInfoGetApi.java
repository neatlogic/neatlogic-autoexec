/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobSource;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.job.source.AutoexecJobSourceHandlerFactory;
import codedriver.framework.autoexec.job.source.IAutoexecJobSourceHandler;
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
 * @author lvzk
 * @since 2022/5/11 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobInfoGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return "获取作业详情";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true)
    })
    @Output({
            @Param(explode = AutoexecJobVo.class, desc = "列表"),
    })
    @Description(desc = "获取作业详情")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        //作业基本信息
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        //判断是否有执行与接管权限
        if(!Objects.equals(jobVo.getStatus(), JobStatus.CHECKED.getValue())) {
            if (UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                jobVo.setIsCanExecute(1);
            } else if ((Objects.equals(jobVo.getSource(), JobSource.TEST.getValue()) && AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class))) {
                jobVo.setIsCanTakeOver(1);
            } else if (Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
                AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(jobVo.getOperationId());
                if (combopVo == null) {
                    throw new AutoexecCombopNotFoundException(jobVo.getOperationId());
                }
                if (autoexecCombopService.checkOperableButton(combopVo, CombopAuthorityAction.EXECUTE)) {
                    jobVo.setIsCanTakeOver(1);
                }
            }
        }
        //补充作业额外信息，如发布
        IAutoexecJobSourceHandler jobSourceHandler = AutoexecJobSourceHandlerFactory.getJobSource(jobVo.getSource());
        if(jobSourceHandler != null) {
            jobVo.setExtraInfo(jobSourceHandler.getExtraJobInfo(jobVo));
        }
        return jobVo;
    }

    @Override
    public String getToken() {
        return "autoexec/job/info/get";
    }
}
