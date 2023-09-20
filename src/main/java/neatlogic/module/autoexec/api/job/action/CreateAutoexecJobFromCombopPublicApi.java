/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopVersionNotFoundEditTargetException;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecJobActionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2023/9/20 11:20
 **/

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateAutoexecJobFromCombopPublicApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    AutoexecCombopMapper combopMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return "创建作业供外部调用";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopName", type = ApiParamType.STRING, isRequired = true, desc = "组合工具名"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.name"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "term.autoexec.executeparam"),
            @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.source"),
            @Param(name = "invokeId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.invokeid"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.parentid"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.scenarioname"),
            @Param(name = "roundCount", type = ApiParamType.LONG, desc = "执行"),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.executeconfig"),
            @Param(name = "planStartTime", type = ApiParamType.LONG, desc = "common.planstarttime"),
            @Param(name = "triggerType", type = ApiParamType.ENUM, member = JobTriggerType.class, desc = "nmaaja.createautoexecjobfromcombopapi.input.param.desc.triggertype"),
            @Param(name = "assignExecUserId", type = ApiParamType.STRING, desc = "指定执行用户id,如果没有指定则默认是当前认证用户")
    })
    @Output({
    })
    @Description(desc = "创建作业供外部调用（来自组合工具）")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String combopName = jsonObj.getString("combopName");
        AutoexecCombopVo combopVo = combopMapper.getAutoexecCombopByName(combopName);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopName);
        }
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopVo.getId());
        if (activeVersionId == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(combopName);
        }
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundEditTargetException(activeVersionId);
        }
        AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();

        String assignExecUserUuid = UserContext.get().getUserUuid();
        String assignExecUserId = jsonObj.getString("assignExecUserId");
        UserVo assignExecUserVo = userMapper.getUserByUserId(assignExecUserId);
        if (assignExecUserVo != null) {
            assignExecUserUuid = assignExecUserVo.getUuid();
        }
        JSONObject param = jsonObj.getJSONObject("param");
        jsonObj.put("param", initParam(param, versionConfig));
        jsonObj.put("assignExecUser", assignExecUserUuid);
        jsonObj.put("operationType", CombopOperationType.COMBOP.getValue());
        jsonObj.put("operationId", combopVo.getId());
        AutoexecJobVo autoexecJobParam = JSONObject.toJavaObject(jsonObj, AutoexecJobVo.class);
        AutoexecCombopExecuteConfigVo executeConfigVo = autoexecJobParam.getExecuteConfig();
        if (executeConfigVo != null) {
            if (StringUtils.isNotBlank(executeConfigVo.getProtocol())) {
                IResourceAccountCrossoverMapper accountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
                AccountProtocolVo accountProtocolVo = accountCrossoverMapper.getAccountProtocolVoByProtocolName(executeConfigVo.getProtocol());
                if (accountProtocolVo == null) {
                    throw new ResourceCenterAccountProtocolNotFoundException(executeConfigVo.getProtocol());
                }
                executeConfigVo.setProtocolId(accountProtocolVo.getId());
            }
        }
        autoexecJobActionService.validateAndCreateJobFromCombop(autoexecJobParam);
        String triggerType = jsonObj.getString("triggerType");
        Long planStartTime = jsonObj.getLong("planStartTime");
        autoexecJobActionService.settingJobFireMode(triggerType, planStartTime, autoexecJobParam);
        return new JSONObject() {{
            put("jobId", autoexecJobParam.getId());
        }};
    }

    private JSONObject initParam(JSONObject param, AutoexecCombopVersionConfigVo versionConfig) {
        List<AutoexecParamVo> paramList = versionConfig.getRuntimeParamList().stream().filter(o -> !Arrays.asList(ParamType.FILE.getValue()).contains(o.getType())).collect(Collectors.toList());

        JSONObject newParam = new JSONObject();
        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Optional<AutoexecParamVo> paramVoOptional = paramList.stream().filter(o -> Objects.equals(o.getKey(), key)).findFirst();
            if (paramVoOptional.isPresent()) {
                IScriptParamType paramType = ScriptParamTypeFactory.getHandler(paramVoOptional.get().getType());
                if (paramType != null) {
                    value = paramType.getExchangeParamByValue(value);
                }
                newParam.put(key, value);
            }
        }
        return newParam;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/create/public";
    }
}
