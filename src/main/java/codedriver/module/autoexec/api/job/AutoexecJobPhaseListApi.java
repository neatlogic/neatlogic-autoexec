/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeStatusCountVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import codedriver.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static codedriver.framework.common.util.CommonUtil.distinctByKey;

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
        List<AutoexecJobPhaseVo> jobPhaseVoList;
        if (jsonObj.containsKey("phaseIdList")) {
            jobPhaseIdList = jsonObj.getJSONArray("phaseIdList").toJavaList(Long.class);
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (CollectionUtils.isEmpty(jobPhaseIdList)) {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobId);
        } else {
            jobPhaseVoList = autoexecJobMapper.getJobPhaseListWithGroupByJobIdAndPhaseIdList(jobId, jobPhaseIdList);
        }

        //过滤出需要根据入参phaseList 更新执行目标的阶段List
        List<AutoexecJobPhaseVo> jobPrPhaseList = jobPhaseVoList.stream().filter(o -> Objects.equals(o.getIsPreOutputUpdateNode(), 1) && Objects.equals(o.getStatus(), JobPhaseStatus.COMPLETED.getValue())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(jobPrPhaseList)) {
            for (AutoexecJobPhaseVo prePhase : jobPrPhaseList) {
                jobPhaseVoList.addAll(autoexecJobService.getJobPhaseListByPreOutput(jobVo, prePhase));
            }
            jobPhaseVoList = jobPhaseVoList.stream().filter(distinctByKey(AutoexecJobPhaseVo::getId)).collect(Collectors.toList());
        }

        List<AutoexecJobPhaseNodeStatusCountVo> statusCountVoList = autoexecJobMapper.getJobPhaseNodeStatusCount(jobId);
        for (AutoexecJobPhaseVo phaseVo : jobPhaseVoList) {
            for (AutoexecJobPhaseNodeStatusCountVo statusCountVo : statusCountVoList) {
                if (statusCountVo.getJobPhaseId().equals(phaseVo.getId())) {
                    phaseVo.addStatusCountVo(statusCountVo);
                }
            }
            List<AutoexecJobPhaseNodeStatusCountVo> jobPhaseNodeStatusCountVoList = phaseVo.getStatusCountVoList();
            AtomicInteger succeedCount = new AtomicInteger(0);
            AtomicInteger totalCount = new AtomicInteger();
            jobPhaseNodeStatusCountVoList.forEach(o -> {
                if (Objects.equals(o.getStatus(), JobNodeStatus.SUCCEED.getValue())) {
                    succeedCount.set(o.getCount());

                }
                totalCount.addAndGet(o.getCount());
            });
            phaseVo.setCompletionRate((int) (Double.parseDouble(Integer.toString(succeedCount.get())) / Double.parseDouble(Integer.toString(totalCount.get())) * 100));
        }
        result.put("status", jobVo.getStatus());
        result.put("statusName", JobStatus.getText(jobVo.getStatus()));
        result.put("phaseList", jobPhaseVoList);
        IAutoexecJobSourceTypeHandler jobSourceTypeHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobVo.getSource());
        if (jobSourceTypeHandler != null) {
            result.putAll(jobSourceTypeHandler.getExtraRefreshJobInfo(jobVo));
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/list";
    }
}
