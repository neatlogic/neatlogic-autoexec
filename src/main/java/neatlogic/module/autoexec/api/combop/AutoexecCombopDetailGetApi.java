/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
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
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

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
