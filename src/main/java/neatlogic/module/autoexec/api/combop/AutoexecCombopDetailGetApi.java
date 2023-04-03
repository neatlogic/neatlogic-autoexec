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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
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
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本id")
    })
    @Output({
            @Param(explode = AutoexecCombopVo.class, desc = "组合工具基本信息")
    })
    @Description(desc = "查询组合工具基本信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Long versionId = paramObj.getLong("versionId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(id);
        }
        AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(autoexecCombopVo.getTypeId());
        if (autoexecTypeVo != null) {
            autoexecCombopVo.setTypeName(autoexecTypeVo.getName() + "[" + autoexecTypeVo.getDescription()+ "]");
        } else {
            autoexecCombopVo.setTypeName(autoexecCombopVo.getTypeId().toString());
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(id);
        if (activeVersionId != null) {
            autoexecCombopVo.setActiveVersionId(activeVersionId);
            if (versionId == null) {
                versionId = activeVersionId;
            }
        }
        if (versionId != null) {
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(versionId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(versionId);
            }
            AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
            autoexecCombopService.needExecuteConfig(autoexecCombopVersionVo);
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
        return "获取组合工具及激活版本详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }
}
