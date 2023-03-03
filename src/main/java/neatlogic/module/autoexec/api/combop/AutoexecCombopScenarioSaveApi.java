/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

//@Service
//@Transactional
@Deprecated
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopScenarioSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecScenarioMapper autoexecScenarioMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/scenario/save";
    }

    @Override
    public String getName() {
        return "保存组合工具场景列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "组合工具id"),
            @Param(name = "defaultScenarioId", type = ApiParamType.LONG, desc = "默认场景id"),
            @Param(name = "scenarioList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "场景列表[{\"scenarioId\": \"场景id\", \"scenarioName\": \"场景名\", \"combopPhaseNameList\": \"阶段名列表\"}]")
    })
    @Description(desc = "保存组合工具场景列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (Objects.equals(autoexecCombopVo.getEditable(), 0)) {
            throw new PermissionDeniedException();
        }
        Long defaultScenarioId = jsonObj.getLong("defaultScenarioId");
        if (defaultScenarioId != null) {
            if (autoexecScenarioMapper.checkScenarioIsExistsById(defaultScenarioId) == 0) {
                throw new AutoexecScenarioIsNotFoundException(defaultScenarioId);
            }
        }
        List<AutoexecCombopScenarioVo> scenarioList = jsonObj.getJSONArray("scenarioList").toJavaList(AutoexecCombopScenarioVo.class);
        for (AutoexecCombopScenarioVo autoexecCombopScenarioVo : scenarioList) {
            if (autoexecScenarioMapper.checkScenarioIsExistsById(autoexecCombopScenarioVo.getScenarioId()) == 0) {
                throw new AutoexecScenarioIsNotFoundException(autoexecCombopScenarioVo.getScenarioName());
            }
        }
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        config.setScenarioList(scenarioList);
        if (defaultScenarioId != null) {
            config.setDefaultScenarioId(defaultScenarioId);
        }
        autoexecCombopVo.setConfigStr(null);
        autoexecCombopMapper.updateAutoexecCombopConfigById(autoexecCombopVo);
        return null;
    }

}
