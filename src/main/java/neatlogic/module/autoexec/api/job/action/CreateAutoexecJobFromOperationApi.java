/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecPhaseOperationParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecJobCanNotTestException;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecToolNotFoundException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author lvzk
 * @since 2021/7/22 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecJobFromOperationApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecScriptMapper scriptMapper;

    @Resource
    AutoexecToolMapper toolMapper;

    @Override
    public String getName() {
        return "nmaa.createautoexecjobfromoperationapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.createautoexecjobfromoperationapi.input.param.desc.operationid"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "nmaa.createautoexecjobfromoperationapi.input.param.desc.param"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "nmaa.createautoexecjobfromoperationapi.input.param.desc.type"),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "nmaa.createautoexecjobfromoperationapi.input.param.desc.executeconfig", isRequired = true),
            @Param(name = "argumentMappingList", type = ApiParamType.JSONARRAY, desc = "nmaa.createautoexecjobfromoperationapi.input.param.desc.argumentmappinglist"),
    })
    @Output({
    })
    @Description(desc = "nmaa.createautoexecjobfromoperationapi.getname")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopVo combopVo = buildCombopVo(jsonObj);
        //设置作业执行节点
        if (combopVo.getConfig() != null && jsonObj.containsKey("executeConfig")) {
            AutoexecCombopExecuteConfigVo executeConfigVo = JSON.toJavaObject(jsonObj.getJSONObject("executeConfig"), AutoexecCombopExecuteConfigVo.class);
            combopVo.getConfig().setExecuteConfig(executeConfigVo);
        }
        String type = jsonObj.getString("type");
        if (Objects.equals(CombopOperationType.SCRIPT.getValue(), type)) {
            jsonObj.put("source", JobSource.SCRIPT_TEST.getValue());
        } else if (Objects.equals(CombopOperationType.TOOL.getValue(), type)) {
            jsonObj.put("source", JobSource.TOOL_TEST.getValue());
        }
        AutoexecJobVo jobVo = JSON.toJavaObject(jsonObj, AutoexecJobVo.class);
        jobVo.setRunTimeParamList(combopVo.getConfig().getRuntimeParamList() == null ? new ArrayList<>() : combopVo.getConfig().getRuntimeParamList());
        jobVo.setOperationType(type);
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setInvokeId(jobVo.getOperationId());
        jobVo.setRouteId(jobVo.getOperationId().toString());
        jobVo.setConfigStr(JSON.toJSONString(combopVo.getConfig()));
        autoexecJobService.saveAutoexecCombopJob(jobVo);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        fireAction.doService(jobVo);
        return new JSONObject() {{
            put("jobId", jobVo.getId());
        }};
    }

    /**
     * 构建虚拟组合工具（数据库不存在的组合工具，仅用于测试）
     *
     * @return 组合工具
     */
    private AutoexecCombopVo buildCombopVo(JSONObject jsonObj) {
        Long operationId = jsonObj.getLong("operationId");
        AutoexecCombopVo combopVo = new AutoexecCombopVo();
        AutoexecCombopPhaseVo autoexecCombopPhaseVo = new AutoexecCombopPhaseVo();
        AutoexecPhaseOperationParamVo phaseOperationParam;
        if (Objects.equals(jsonObj.getString("type"), CombopOperationType.SCRIPT.getValue())) {
            AutoexecScriptVersionVo scriptVersionVo = scriptMapper.getVersionByVersionId(operationId);
            if (scriptVersionVo == null) {
                throw new AutoexecScriptVersionNotFoundException(operationId);
            }
            AutoexecScriptVo scriptVo = scriptMapper.getScriptBaseInfoById(scriptVersionVo.getScriptId());
            if (scriptVo == null) {
                throw new AutoexecScriptNotFoundException(scriptVersionVo.getScriptId());
            }
            List<AutoexecScriptVersionParamVo> paramVoList = scriptMapper.getParamListByVersionId(scriptVersionVo.getId());
            scriptVersionVo.setParamList(paramVoList);
            phaseOperationParam = new AutoexecPhaseOperationParamVo(scriptVo, scriptVersionVo);
        } else {
            AutoexecToolVo toolVo = toolMapper.getToolById(operationId);
            if (toolVo == null) {
                throw new AutoexecToolNotFoundException(operationId);
            }
            phaseOperationParam = new AutoexecPhaseOperationParamVo(toolVo);
        }
        if (jsonObj.containsKey("argumentMappingList")) {
            phaseOperationParam.setArgumentMappingList(JSON.parseArray(jsonObj.getString("argumentMappingList"), ParamMappingVo.class));
        }
        checkJobExist(phaseOperationParam);
        AutoexecCombopGroupVo combopGroupVo = new AutoexecCombopGroupVo();
        combopGroupVo.setPolicy(AutoexecJobGroupPolicy.ONESHOT.getName());
        combopGroupVo.setSort(0);
        autoexecCombopPhaseVo.setGroupId(combopGroupVo.getId());
        autoexecCombopPhaseVo.setGroupSort(combopGroupVo.getSort());
        autoexecCombopPhaseVo.setSort(0);
        autoexecCombopPhaseVo.setName("test_phase");
        String execMode = Objects.equals("native", phaseOperationParam.getExecMode()) ? "runner" : phaseOperationParam.getExecMode();
        autoexecCombopPhaseVo.setExecMode(execMode);
        autoexecCombopPhaseVo.setExecModeName(ExecMode.getText(execMode));
        AutoexecCombopPhaseConfigVo combopPhaseConfigVo = new AutoexecCombopPhaseConfigVo();
        AutoexecCombopPhaseOperationVo phaseOperation = new AutoexecCombopPhaseOperationVo(phaseOperationParam);
        //脚本创建测试作业，需补充scriptId
        if (Objects.equals(phaseOperationParam.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
            AutoexecScriptVo scriptVo = scriptMapper.getScriptByVersionId(phaseOperationParam.getOperationId());
            if (scriptVo != null) {
                phaseOperation.setOperationId(scriptVo.getId());
            }
        }
        combopPhaseConfigVo.setPhaseOperationList(Collections.singletonList(phaseOperation));
        autoexecCombopPhaseVo.setConfig(combopPhaseConfigVo);
        combopVo.setName("TEST_" + phaseOperationParam.getName());
        combopVo.setId(phaseOperationParam.getOperationId());
        combopVo.setOperationType(phaseOperationParam.getOperationType());
        combopVo.setConfig(new AutoexecCombopConfigVo());
        combopVo.getConfig().setRuntimeParamList(phaseOperationParam.getInputParamList());
        combopVo.getConfig().setCombopPhaseList(Collections.singletonList(autoexecCombopPhaseVo));
        combopVo.getConfig().setCombopGroupList(Collections.singletonList(combopGroupVo));
        return combopVo;
    }

    /**
     * 检查自定义工具库 是否已经有作业在跑（running、pausing、aborting），如果存在，则无法创建新的测试作业。否则删除历史测试作业，新建测试作业
     */
    private void checkJobExist(AutoexecPhaseOperationParamVo phaseParam) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByOperationId(phaseParam.getOperationId());
        if (jobVo != null) {
            if (Arrays.asList(JobStatus.RUNNING.getValue(), JobStatus.PAUSING.getValue(), JobStatus.ABORTING.getValue()).contains(jobVo.getStatus())) {
                throw new AutoexecJobCanNotTestException(jobVo.getId().toString());
            }
            autoexecJobService.deleteJob(jobVo);
        }
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/operation/create";
    }
}
