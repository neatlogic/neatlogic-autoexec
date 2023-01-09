/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
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
            @Param(name = "defaultScenarioId", type = ApiParamType.LONG, isRequired = true, desc = "默认场景id"),
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
        if (autoexecScenarioMapper.checkScenarioIsExistsById(defaultScenarioId) == 0) {
            throw new AutoexecScenarioIsNotFoundException(defaultScenarioId);
        }
        List<AutoexecCombopScenarioVo> scenarioList = jsonObj.getJSONArray("scenarioList").toJavaList(AutoexecCombopScenarioVo.class);
        for (AutoexecCombopScenarioVo autoexecCombopScenarioVo : scenarioList) {
            if (autoexecScenarioMapper.checkScenarioIsExistsById(autoexecCombopScenarioVo.getScenarioId()) == 0) {
                throw new AutoexecScenarioIsNotFoundException(autoexecCombopScenarioVo.getScenarioName());
            }
        }
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        config.setScenarioList(scenarioList);
        config.setDefaultScenarioId(defaultScenarioId);
        autoexecCombopVo.setConfigStr(null);
        autoexecCombopMapper.updateAutoexecCombopConfigById(autoexecCombopVo);
        return null;
    }

}
