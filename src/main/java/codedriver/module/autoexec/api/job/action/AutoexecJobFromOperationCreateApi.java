/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecPhaseOperationParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecJobCanNotTestException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecToolNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
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
public class AutoexecJobFromOperationCreateApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    TeamMapper teamMapper;

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
    })
    @Output({
    })
    @Description(desc = "作业创建（来自 工具库|自定义工具库）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject paramJson = jsonObj.getJSONObject("param");
        AutoexecCombopVo combopVo = buildCombopVo(jsonObj);
        //设置作业执行节点
        if(combopVo.getConfig() != null && jsonObj.containsKey("executeConfig")){
            AutoexecCombopExecuteConfigVo executeConfigVo = JSON.toJavaObject(jsonObj.getJSONObject("executeConfig"), AutoexecCombopExecuteConfigVo.class);
            combopVo.getConfig().setExecuteConfig(executeConfigVo);
        }
        combopVo.setIsTest(true);
        AutoexecJobVo jobVo = autoexecJobService.saveAutoexecCombopJob(combopVo, jsonObj.getString("source"), null, paramJson);
        jobVo.setAction(JobAction.FIRE.getValue());
        jobVo.setCurrentPhaseSort(0);
        autoexecJobActionService.fire(jobVo);
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
        String type = jsonObj.getString("type");
        JSONObject paramJson = jsonObj.getJSONObject("param");
        String source = jsonObj.getString("source");
        AutoexecScriptVersionVo scriptVersionVo;
        AutoexecToolVo toolVo;
        AutoexecCombopVo combopVo = new AutoexecCombopVo();
        JSONArray combopPhaseArray = new JSONArray();
        AutoexecPhaseOperationParamVo phaseParam;
        if (Objects.equals(type, CombopOperationType.SCRIPT.getValue())) {
            scriptVersionVo = scriptMapper.getVersionByVersionId(operationId);
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
            toolVo = toolMapper.getToolById(operationId);
            if (toolVo == null) {
                throw new AutoexecToolNotFoundException(operationId);
            }
            phaseParam = new AutoexecPhaseOperationParamVo(toolVo);
        }
        checkJobExist(phaseParam);
        initPhaseArray(combopPhaseArray,phaseParam);
        combopVo.setName("TEST_"+phaseParam.getName());
        combopVo.setId(phaseParam.getOperationId());
        combopVo.setOperationType(phaseParam.getOperationType());
        initRuntimeParamList(combopVo,phaseParam);
        combopVo.setConfig("{}");
        combopVo.getConfig().setCombopPhaseList(combopPhaseArray.toJavaList(AutoexecCombopPhaseVo.class));
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

    /**
     * 初始化运行参是
     * @param combopVo 组合工具
     * @param phaseOperationParamVo scriptVersion|tool
     */
    private void initRuntimeParamList(AutoexecCombopVo combopVo, AutoexecPhaseOperationParamVo phaseOperationParamVo) {
        List<AutoexecCombopParamVo> runtimeParamList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(phaseOperationParamVo.getInputParamList())) {
            phaseOperationParamVo.getInputParamList().forEach(o -> {
                runtimeParamList.add(new AutoexecCombopParamVo(o));
            });
            combopVo.setRuntimeParamList(runtimeParamList);
        }
    }

    /**
     * 构建combopPhaseList
     * @param combopPhaseArray 虚拟组合工具phaseArray
     * @param phaseOperationParamVo scriptVersion|tool
     */
    private void initPhaseArray(JSONArray combopPhaseArray, AutoexecPhaseOperationParamVo phaseOperationParamVo){
        combopPhaseArray.add(new JSONObject() {{
            put("sort", 0);
            put("name", "test_phase");
            put("execMode", phaseOperationParamVo.getExecMode());
            put("execModeName", ExecMode.getText(phaseOperationParamVo.getExecMode()));
            put("config", new JSONObject() {{
                put("executeConfig", null);
                put("phaseOperationList" ,new JSONArray(){{
                    add(new JSONObject(){{
                        put("name", phaseOperationParamVo.getName().replaceAll("//","_"));
                        put("operationId", phaseOperationParamVo.getOperationId());
                        put("operationType",phaseOperationParamVo.getOperationType());
                        put("execMode",phaseOperationParamVo.getExecMode());
                        put("failPolicy", FailPolicy.STOP.getValue());
                        put("parser", phaseOperationParamVo.getParser());
                        put("sort", 0);
                        put("config",new JSONObject(){{
                            put("paramMappingList",new JSONArray(){{
                                if(CollectionUtils.isNotEmpty(phaseOperationParamVo.getInputParamList())) {
                                    for(AutoexecParamVo paramVo : phaseOperationParamVo.getInputParamList()) {
                                        add(new JSONObject() {{
                                            put("key",paramVo.getKey());
                                            put("mappingMode", ParamMappingMode.RUNTIME_PARAM.getValue());
                                            put("value",paramVo.getKey());
                                        }});
                                    }
                                }
                            }});
                        }});
                        put("inputParamList", phaseOperationParamVo.getInputParamList());
                        put("outParamList", phaseOperationParamVo.getOutputParamList());
                    }});
                }});
            }});
        }});
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/operation/create";
    }
}