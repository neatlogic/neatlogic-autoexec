/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.job.*;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2021/11/9 12:18
 **/
@Service
public class AutoexecJobNodeOutputParamGetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeOutputParamGetHandler.class);

    @Override
    public String getName() {
        return JobAction.GET_NODE_OUTPUT_PARAM.getValue();
    }

    @Override
    public boolean myValidate(AutoexecJobVo jobVo) {
        currentPhaseIdValid(jobVo);
        currentResourceIdValid(jobVo);
        return true;
    }

    @Override
    public JSONObject doMyService(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        AutoexecJobPhaseVo phaseVo = jobVo.getCurrentPhase();
        JSONObject paramJson = jobVo.getActionParam();
        paramJson.put("jobId", nodeVo.getJobId());
        paramJson.put("phase", nodeVo.getJobPhaseName());
        paramJson.put("nodeId", nodeVo.getId());
        paramJson.put("resourceId", nodeVo.getResourceId());
        paramJson.put("phaseId", nodeVo.getJobPhaseId());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("port", nodeVo.getPort());
        paramJson.put("runnerUrl", nodeVo.getRunnerUrl());
        paramJson.put("execMode", phaseVo.getExecMode());
        JSONArray operationOutputParamArray = null;
        String url = paramJson.getString("runnerUrl") + "/api/rest/job/phase/node/output/param/get";
        JSONObject statusJson = JSONObject.parseObject(AutoexecUtil.requestRunner(url, paramJson));
        if (MapUtils.isNotEmpty(statusJson)) {
            Long jobId = paramJson.getLong("jobId");
            Long jobPhaseId = paramJson.getLong("phaseId");
            List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(jobId, jobPhaseId);
            List<AutoexecJobContentVo> paramContentVoList = autoexecJobMapper.getJobContentList(operationVoList.stream().map(AutoexecJobPhaseOperationVo::getParamHash).collect(Collectors.toList()));
            AutoexecJobPhaseNodeVo phaseNodeVo = getNodeOperationStatus(paramJson,true);
            List<AutoexecJobPhaseNodeOperationStatusVo> operationStatusVos = phaseNodeVo.getOperationStatusVoList();
            operationOutputParamArray = new JSONArray() {{
                for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
                    add(new JSONObject() {{
                        put("name", operationVo.getName());
                        List<AutoexecJobParamVo> outputParamList = new ArrayList<>();
                        List<AutoexecJobParamVo> finalOutputParamList = outputParamList;
                        paramContentVoList.forEach(o -> {
                            if (Objects.equals(operationVo.getParamHash(), o.getHash())) {
                                JSONObject json = JSONObject.parseObject(o.getContent());
                                JSONArray outputArray = json.getJSONArray("outputParamList");
                                for (Object output : outputArray) {
                                    AutoexecJobParamVo outputVo = new AutoexecJobParamVo(JSONObject.parseObject(output.toString()));
                                    JSONObject valueJson = statusJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
                                    if (valueJson != null) {
                                        outputVo.setValue(valueJson.getString(outputVo.getKey()));
                                    }
                                    finalOutputParamList.add(outputVo);
                                }
                            }
                        });
                        outputParamList = outputParamList.stream().sorted(Comparator.comparing(AutoexecJobParamVo::getSort)).collect(Collectors.toList());
                        put("paramList", outputParamList);
                        Optional<AutoexecJobPhaseNodeOperationStatusVo> operationStatusVoOptional = operationStatusVos.stream().filter(o->Objects.equals(o.getName(),operationVo.getName())).findFirst();
                        operationStatusVoOptional.ifPresent(autoexecJobPhaseNodeOperationStatusVo -> put("status", autoexecJobPhaseNodeOperationStatusVo.getStatus()));
                    }});
                }
            }};
        }

        result.put("operationOutputParamArray", operationOutputParamArray);
        return result;
    }
}
