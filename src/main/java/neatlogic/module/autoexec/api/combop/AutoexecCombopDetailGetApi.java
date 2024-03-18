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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopNotFoundEditTargetException;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopVersionNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class AutoexecCombopDetailGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "common.versionid")
    })
    @Output({
            @Param(explode = AutoexecCombopVo.class, desc = "term.autoexec.combopdetailsinfo")
    })
    @Description(desc = "nmaac.autoexeccombopdetailgetapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Long versionId = paramObj.getLong("versionId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopService.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundEditTargetException(id);
        }
        AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(autoexecCombopVo.getTypeId());
        if (autoexecTypeVo != null) {
            autoexecCombopVo.setTypeName(autoexecTypeVo.getName() + "[" + autoexecTypeVo.getDescription()+ "]");
        } else {
            autoexecCombopVo.setTypeName(autoexecCombopVo.getTypeId().toString());
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        Long activeVersionId = autoexecCombopVo.getActiveVersionId();
        if (activeVersionId != null && versionId == null) {
            versionId = activeVersionId;
        }
        if (versionId != null) {
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(versionId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundEditTargetException(versionId);
            }
            if (Objects.equals(versionId, activeVersionId)) {
                try {
                    autoexecCombopService.verifyAutoexecCombopVersionConfig(autoexecCombopVersionVo.getConfig(), false);
                } catch (Exception e) {
                    autoexecCombopVo.setLcu(UserContext.get().getUserUuid());
                    autoexecCombopMapper.updateAutoexecCombopIsActiveById(autoexecCombopVo);

                    JSONObject configExpiredReason = new JSONObject();
                    JSONArray reasonList = new JSONArray();
                    JSONObject errorMessageObj = new JSONObject();
                    errorMessageObj.put("description", e.getMessage());
                    reasonList.add(errorMessageObj);
                    configExpiredReason.put("reasonList", reasonList);
                    autoexecCombopVersionVo.setConfigExpired(1);
                    autoexecCombopVersionVo.setConfigExpiredReason(configExpiredReason);

                    autoexecCombopVo.setIsActive(0);
                    autoexecCombopVo.setConfigExpired(1);
                    autoexecCombopVo.setConfigExpiredReason(configExpiredReason);
                }
            }
            AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
            autoexecCombopService.needExecuteConfig(autoexecCombopVersionVo);
            autoexecCombopVo.setAllPhasesAreRunnerOrSqlExecMode(autoexecCombopVersionVo.getAllPhasesAreRunnerOrSqlExecMode());
            autoexecCombopVo.setNeedExecuteNode(autoexecCombopVersionVo.getNeedExecuteNode());
            autoexecCombopVo.setNeedExecuteUser(autoexecCombopVersionVo.getNeedExecuteUser());
            autoexecCombopVo.setNeedProtocol(autoexecCombopVersionVo.getNeedProtocol());
            autoexecCombopVo.setNeedRoundCount(autoexecCombopVersionVo.getNeedRoundCount());
            AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
            config.setExecuteConfig(versionConfig.getExecuteConfig());
            config.setCombopGroupList(versionConfig.getCombopGroupList());
            config.setCombopPhaseList(versionConfig.getCombopPhaseList());
            config.setRuntimeParamList(versionConfig.getRuntimeParamList());
            config.setScenarioList(versionConfig.getScenarioList());
            config.setDefaultScenarioId(versionConfig.getDefaultScenarioId());
        }
        return autoexecCombopVo;
    }

    @Override
    public String getToken() {
        return "autoexec/combop/detail/get";
    }

    @Override
    public String getName() {
        return "nmaac.autoexeccombopdetailgetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }
}
