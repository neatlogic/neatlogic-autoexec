/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseRunnerNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobWaitingDetailApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "nmaaj.getautoexecjobqueuestatusapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.jobid"),
            @Param(name = "groupSort", type = ApiParamType.LONG, desc = "term.autoexec.groupid"),
            @Param(name = "phaseId", type = ApiParamType.LONG, desc = "term.process.phaseid"),
    })
    @Description(desc = "nmaaj.getautoexecjobqueuestatusapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Integer groupSort = jsonObj.getInteger("groupSort");
        Long phaseId = jsonObj.getLong("phaseId");
        JSONObject result = new JSONObject();
        JSONArray queueStatusArray = new JSONArray();
        //作业基本信息
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        List<RunnerMapVo> runnerVos;
        if (groupSort != null) {
            List<AutoexecJobPhaseVo> phaseVos = autoexecJobMapper.getJobPhaseListByJobIdAndGroupSort(jobId, groupSort);
            runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobIdAndPhaseIdList(jobVo.getId(), phaseVos.stream().map(AutoexecJobPhaseVo::getId).collect(Collectors.toList()));
            if (CollectionUtils.isEmpty(runnerVos)) {
                throw new AutoexecJobPhaseRunnerNotFoundException(jobVo.getExecuteJobPhaseList().stream().map(AutoexecJobPhaseVo::getName).collect(Collectors.joining("','")));
            }
        } else {
            runnerVos = autoexecJobMapper.getJobPhaseRunnerMapByJobId(jobId);
        }

        autoexecJobService.checkRunnerHealth(runnerVos);
        for (RunnerMapVo runner : runnerVos) {
            String url = runner.getUrl() + "api/rest/job/waiting/detail/get";
            HttpRequestUtil requestUtil = HttpRequestUtil.post(url).setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            if (StringUtils.isNotBlank(requestUtil.getError())) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }
            JSONObject resultJson = requestUtil.getResultJson();
            if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
                throw new RunnerHttpRequestException(url + ":" + requestUtil.getError());
            }

            JSONObject queueJson = resultJson.getJSONObject("Return");
            if (MapUtils.isNotEmpty(queueJson)) {
                for (Map.Entry<String, Object> entry : queueJson.entrySet()) {
                    String key = entry.getKey();
                    if (Objects.equals(key, "count")) {
                        continue;
                    }
                    JSONObject value = JSON.parseObject(entry.getValue().toString());
                    JSONObject queueStatus = new JSONObject();
                    queueStatus.put("sort", key + "/" + queueJson.getString("count"));
                    queueStatus.put("command", value.getString("command"));
                    queueStatus.put("fcd", TimeUtil.convertDateToString(new Date(value.getLong("fcd")),TimeUtil.YYYY_MM_DD_HH_MM_SS));
                    queueStatus.put("runner", runner.getName()+":"+runner.getPort());
                    queueStatus.put("runnerId", runner.getId());
                    queueStatusArray.add(queueStatus);
                }
            }
        }
        result.put("tbodyList",queueStatusArray);
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/waiting/detail/get";
    }
}
