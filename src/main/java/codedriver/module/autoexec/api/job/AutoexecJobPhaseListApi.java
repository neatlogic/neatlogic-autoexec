/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeStatusCountVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lvzk
 * @since 2022/5/6 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseListApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "获取作业阶段列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "phaseIdList", type = ApiParamType.JSONARRAY, desc = "作业阶段idList"),
    })
    @Output({
            @Param(name = "status", type = ApiParamType.STRING, desc = "作业状态"),
            @Param(name = "statusName", type = ApiParamType.STRING, desc = "作业状态名"),
            @Param(name = "phaseList", explode = AutoexecJobPhaseVo[].class, desc = "作业阶段list"),
    })
    @Description(desc = "获取作业阶段列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        List<Long> jobPhaseIdList = null;
        List<AutoexecJobPhaseVo> jobPhaseVoList = null;
        if (jsonObj.containsKey("phaseIdList")) {
            jobPhaseIdList = jsonObj.getJSONArray("phaseIdList").toJavaList(Long.class);
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (CollectionUtils.isEmpty(jobPhaseIdList)) {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobId(jobId);
        } else {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndPhaseIdList(jobId, jobPhaseIdList);
        }
        List<AutoexecJobPhaseNodeStatusCountVo> statusCountVoList = autoexecJobMapper.getJobPhaseNodeStatusCount(jobId);

       /* boolean isHasActivePhase = false;
        for (int i = 0; i < jobPhaseVoList.size(); i++) {
            AutoexecJobPhaseVo phaseVo = jobPhaseVoList.get(i);
            for (AutoexecJobPhaseNodeStatusCountVo statusCountVo : statusCountVoList) {
                if (statusCountVo.getJobPhaseId().equals(phaseVo.getId())) {
                    phaseVo.addStatusCountVo(statusCountVo);
                }
            }
            if (!isHasActivePhase && (Arrays.asList(JobPhaseStatus.RUNNING.getValue(), JobPhaseStatus.FAILED.getValue(), JobPhaseStatus.ABORTED.getValue()).contains(phaseVo.getStatus()) || i == (jobPhaseVoList.size() - 1))) {
                phaseVo.setIsActive(1);
                isHasActivePhase = true;
            }
        }
        autoexecJobService.setIsRefresh(jobPhaseVoList, result, jobVo, jsonObj.getString("jobStatus"));*/
        result.put("status", jobVo.getStatus());
        result.put("statusName", JobStatus.getText(jobVo.getStatus()));
        result.put("phaseList", jobPhaseVoList);
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/list";
    }
}
