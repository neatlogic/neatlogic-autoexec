/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecPhaseOperationParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecJobCanNotTestException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecToolNotFoundException;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/7/22 11:20
 **/

@Transactional
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
        return "作业创建（来自 工具库|自定义工具库）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.LONG, isRequired = true, desc = "自定义工具库版本ID|工具库ID"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "来源 itsm|human   ITSM|人工发起的等，不传默认是人工发起的"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "script,tool", isRequired = true, desc = "类型 script|tool   自定义工具库|工具库"),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标", isRequired = true),
            @Param(name = "argumentMappingList", type = ApiParamType.JSONARRAY, desc = "自由参数"),
    })
    @Output({
    })
    @Description(desc = "作业创建（来自 工具库|自定义工具库）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopVo combopVo = buildCombopVo(jsonObj);
        //设置作业执行节点
        if(combopVo.getConfig() != null && jsonObj.containsKey("executeConfig")){
            AutoexecCombopExecuteConfigVo executeConfigVo = JSON.toJavaObject(jsonObj.getJSONObject("executeConfig"), AutoexecCombopExecuteConfigVo.class);
            combopVo.getConfig().setExecuteConfig(executeConfigVo);
        }
        jsonObj.put("source",JobSource.TEST.getValue());
        AutoexecJobVo jobVo = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);
        jobVo.setRunTimeParamList(combopVo.getRuntimeParamList());
        jobVo.setOperationType(jsonObj.getString("type"));
        jobVo.setIsFirstFire(1);
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setInvokeId(jobVo.getOperationId());
        jobVo.setConfigStr(combopVo.getConfigStr());
        autoexecJobService.saveAutoexecCombopJob(jobVo);
        IAutoexecJobActionHandler fireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.FIRE.getValue());
        fireAction.doService(jobVo);
        return new JSONObject(){{
            put("jobId",jobVo.getId());
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
        AutoexecPhaseOperationParamVo phaseParam;
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
            phaseParam = new AutoexecPhaseOperationParamVo(scriptVo,scriptVersionVo);
        } else {
            AutoexecToolVo toolVo = toolMapper.getToolById(operationId);
            if (toolVo == null) {
                throw new AutoexecToolNotFoundException(operationId);
            }
            phaseParam = new AutoexecPhaseOperationParamVo(toolVo);
        }
        if(jsonObj.containsKey("argumentMappingList")) {
            phaseParam.setArgumentMappingList(JSONObject.parseArray(jsonObj.getString("argumentMappingList"), ParamMappingVo.class));
        }
        checkJobExist(phaseParam);
        AutoexecCombopGroupVo combopGroupVo = new AutoexecCombopGroupVo();
        combopGroupVo.setPolicy(AutoexecJobGroupPolicy.ONESHOT.getName());
        combopGroupVo.setSort(0);
        autoexecCombopPhaseVo.setGroupId(combopGroupVo.getId());
        autoexecCombopPhaseVo.setGroupSort(combopGroupVo.getSort());
        autoexecCombopPhaseVo.setSort(0);
        autoexecCombopPhaseVo.setName("test_phase");
        autoexecCombopPhaseVo.setExecMode(phaseParam.getExecMode());
        autoexecCombopPhaseVo.setExecModeName(ExecMode.getText(phaseParam.getExecMode()));
        AutoexecCombopPhaseConfigVo combopPhaseConfigVo = new AutoexecCombopPhaseConfigVo();
        AutoexecCombopPhaseOperationVo phaseOperation = new AutoexecCombopPhaseOperationVo(phaseParam);
        combopPhaseConfigVo.setPhaseOperationList(Collections.singletonList(phaseOperation));
        autoexecCombopPhaseVo.setConfig(combopPhaseConfigVo);
        combopVo.setName("TEST_"+phaseParam.getName());
        combopVo.setId(phaseParam.getOperationId());
        combopVo.setOperationType(phaseParam.getOperationType());
        combopVo.setRuntimeParamList(phaseParam.getInputParamList());
        combopVo.setConfig(new AutoexecCombopConfigVo());
        combopVo.getConfig().setCombopPhaseList(Collections.singletonList(autoexecCombopPhaseVo));
        combopVo.getConfig().setCombopGroupList(Collections.singletonList(combopGroupVo));
        return combopVo;
    }

    /**
     * 检查自定义工具库 是否已经有作业在跑（running、pausing、aborting），如果存在，则无法创建新的测试作业。否则删除历史测试作业，新建测试作业
     *
     */
    private void checkJobExist(AutoexecPhaseOperationParamVo phaseParam){
       AutoexecJobVo jobVo = autoexecJobMapper.getJobLockByOperationId(phaseParam.getOperationId());
       if(jobVo != null){
           if(Arrays.asList(JobStatus.RUNNING.getValue(),JobStatus.PAUSING.getValue(), JobStatus.ABORTING.getValue()).contains(jobVo.getStatus())){
               throw new AutoexecJobCanNotTestException(jobVo.getId().toString());
           }
           autoexecJobService.deleteJob(jobVo.getId());
       }
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/operation/create";
    }
}
