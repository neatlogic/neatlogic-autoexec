/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.constvalue.JobPhaseStatus;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeStatusCountVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static neatlogic.framework.common.util.CommonUtil.distinctByKey;

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
