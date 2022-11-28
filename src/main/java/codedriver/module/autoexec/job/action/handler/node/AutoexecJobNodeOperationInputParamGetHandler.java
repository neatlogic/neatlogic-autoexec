/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.job.action.handler.node;

import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/11/23 12:18
 **/
@Service
public class AutoexecJobNodeOperationInputParamGetHandler extends AutoexecJobActionHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoexecJobNodeOperationInputParamGetHandler.class);
    @Resource
    AutoexecJobService autoexecJobService;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return JobAction.GET_NODE_OPERATION_INPUT_PARAM.getValue();
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
        JSONArray operationInputParamList = new JSONArray();
        result.put("operationInputParamArray", operationInputParamList);
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramJson = jobVo.getActionParam();
        paramJson.put("phase", nodeVo.getJobPhaseName());
        List<AutoexecJobPhaseOperationVo> operationVoList = autoexecJobMapper.getJobPhaseOperationByJobIdAndPhaseId(jobVo.getId(), jobVo.getCurrentPhase().getId());
        paramJson.put("execMode", jobVo.getCurrentPhase().getExecMode());
        paramJson.put("ip", nodeVo.getHost());
        paramJson.put("port", nodeVo.getPort());
        String url = nodeVo.getRunnerUrl() + "/api/rest/job/phase/node/input/param/get";
        JSONObject paramsJson = JSONObject.parseObject(AutoexecUtil.requestRunner(url, paramJson));

        //批量查找入参
        List<AutoexecOperationVo> operationVos = new ArrayList<>();
        List<AutoexecOperationVo> argumentsOperationVos = new ArrayList<>();
        List<AutoexecJobPhaseOperationVo> scriptList = new ArrayList<>();
        List<Long> toolIdList = new ArrayList<>();
        for (AutoexecJobPhaseOperationVo jobPhaseOperationVo : operationVoList) {
            if (Objects.equals(CombopOperationType.SCRIPT.getValue(), jobPhaseOperationVo.getType())) {
                scriptList.add(jobPhaseOperationVo);
            } else {
                toolIdList.add(jobPhaseOperationVo.getId());
            }
        }
        if (CollectionUtils.isNotEmpty(scriptList)) {
            operationVos.addAll(autoexecScriptMapper.getAutoexecOperationInputParamList(scriptList));
            argumentsOperationVos.addAll(autoexecScriptMapper.getArgumentListByScriptIdList(scriptList.stream().map(AutoexecJobPhaseOperationVo::getOperationId).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            operationVos.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
            argumentsOperationVos.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
        }
        Map<Long, AutoexecOperationVo> operationVoMap = operationVos.stream().collect(Collectors.toMap(AutoexecOperationBaseVo::getId, o -> o));
        Map<Long, AutoexecOperationVo> argumentsOperationVoMap = argumentsOperationVos.stream().collect(Collectors.toMap(AutoexecOperationBaseVo::getId, o -> o));
        for (AutoexecJobPhaseOperationVo operationVo : operationVoList) {
            JSONObject paramOperation = paramsJson.getJSONObject(operationVo.getName() + "_" + operationVo.getId());
            JSONObject inputParams = null;
            JSONArray arguments = null;
            if (MapUtils.isNotEmpty(paramOperation)) {
                inputParams = paramOperation.getJSONObject("options");
                arguments = paramOperation.getJSONArray("arguments");
            }
            JSONArray paramList = new JSONArray();
            JSONObject operationParam = new JSONObject();
            JSONObject argument = null;
            operationParam.put("name", operationVo.getName());
            operationParam.put("paramList", paramList);
            //arguments
            if (argumentsOperationVoMap.containsKey(operationVo.getOperationId())) {
                AutoexecParamVo argumentParam = argumentsOperationVoMap.get(operationVo.getOperationId()).getArgument();
                argument = new JSONObject();
                argument.put("name", argumentParam.getName());
                argument.put("key", "arguments");
                if (MapUtils.isNotEmpty(inputParams)) {
                    argument.put("valueList", arguments.stream().map(o->JSONObject.parseObject(o.toString()).getString("value")).collect(Collectors.toList()));
                }
                argument.put("description", argumentParam.getDescription());
                operationParam.put("argument", argument);
            }
            //input
            if (operationVoMap.containsKey(operationVo.getOperationId())) {
                List<AutoexecParamVo> inputParamList = operationVoMap.get(operationVo.getOperationId()).getInputParamList();
                for (AutoexecParamVo paramVo : inputParamList) {
                    JSONObject param = new JSONObject();
                    param.put("name", paramVo.getName());
                    param.put("key", paramVo.getKey());
                    if (MapUtils.isNotEmpty(inputParams)) {
                        param.put("value", inputParams.get(paramVo.getKey()));
                    }
                    param.put("description", paramVo.getDescription());
                    paramList.add(param);
                }
            }

            if (CollectionUtils.isNotEmpty(paramList)) {
                operationInputParamList.add(operationParam);
            }
        }
        return result;
    }
}
