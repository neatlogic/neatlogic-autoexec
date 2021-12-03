/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/9/16 14:15
 **/
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecJobResourceInspectUpdateApi extends PublicApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "更新巡检资源作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资产id", isRequired = true),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名", isRequired = true),
            @Param(name = "inspectTime", type = ApiParamType.LONG, desc = "巡检时间", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "更新巡检资源作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String phaseName = jsonObj.getString("phaseName");
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId,phaseName);
        if(phaseVo == null){
            throw new AutoexecJobPhaseNotFoundException(phaseName);
        }
        autoexecJobMapper.replaceIntoJobResourceInspect(jobId,jsonObj.getLong("resourceId"),phaseVo.getId(),jsonObj.getDate("inspectTime"));
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/resource/inspect/update";
    }
}
