/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeStatusCountVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.core.AutoexecJobAuthActionManager;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobDetailGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobAuthActionManager autoexecJobAuthActionManager;

    @Override
    public String getName() {
        return "获取作业详情";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
    })
    @Output({
            @Param(explode = AutoexecJobPhaseVo[].class, desc = "列表")
    })
    @Description(desc = "获取作业详情，包括：剧本列表、作业基本信息、操作按钮")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        //作业基本信息
        AutoexecJobVo jobVo = autoexecJobMapper.getAutoexecJobInfo(jobId);
        //剧本列表
        List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobId);
        List<AutoexecJobPhaseNodeStatusCountVo> statusCountVoList = autoexecJobMapper.getJobPhaseNodeStatusCount(jobId);
        for (AutoexecJobPhaseNodeStatusCountVo statusCountVo : statusCountVoList) {
            for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
                if (statusCountVo.getJobPhaseId().equals(phaseVo.getId())) {
                    phaseVo.addStatusCountVo(statusCountVo);
                }
            }
        }
        jobVo.setJobPhaseList(jobPhaseVoList);
        //操作按钮
        autoexecJobAuthActionManager.setAutoexecJobAction(jobVo);
        return jobVo;
    }

    @Override
    public String getToken() {
        return "autoexec/job/detail/get";
    }
}
