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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopDraftVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopRejectedVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopSubmittedVersionNotFoundException;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopNotFoundEditTargetException;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopVersionNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.notify.handler.AutoexecCombopNotifyPolicyHandler;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopBasicInfoGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/basic/info/get";
    }

    @Override
    public String getName() {
        return "nmaac.autoexeccombopbasicinfogetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "common.versionid"),
            @Param(name = "versionStatus", type = ApiParamType.ENUM, rule = "draft,submitted,passed,rejected", desc = "common.status")
    })
    @Output({
            @Param(explode = AutoexecCombopVo.class, desc = "term.autoexec.combopbaseinfo")
    })
    @Description(desc = "nmaac.autoexeccombopbasicinfogetapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
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
        // owner字段必须在校验权限后，再加上前缀user#
        autoexecCombopVo.setOwner(GroupSearch.USER.getValuePlugin() + autoexecCombopVo.getOwner());
        Long activeVersionId = autoexecCombopVo.getActiveVersionId();
        Long versionId = jsonObj.getLong("versionId");
        if (versionId != null) {
            AutoexecCombopVersionVo versionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(versionId);
            if (versionVo == null) {
                throw new AutoexecCombopVersionNotFoundEditTargetException(versionId);
            }
            autoexecCombopVo.setSpecifyVersionId(versionId);
        } else {
            String versionStatus = jsonObj.getString("versionStatus");
            if (StringUtils.isNotBlank(versionStatus)) {
                if (Objects.equals(versionStatus, ScriptVersionStatus.PASSED.getValue())) {
                    if (activeVersionId == null) {
                        throw new AutoexecCombopActiveVersionNotFoundException(autoexecCombopVo.getName());
                    }
                    autoexecCombopVo.setSpecifyVersionId(activeVersionId);
                } else {
                    Long maxVersionId = autoexecCombopVersionMapper.getAutoexecCombopMaxVersionIdByCombopIdAndStatus(id, versionStatus);
                    if (maxVersionId == null) {
                        if (Objects.equals(versionStatus, ScriptVersionStatus.DRAFT.getValue())) {
                            throw new AutoexecCombopDraftVersionNotFoundException(autoexecCombopVo.getName());
                        } else if (Objects.equals(versionStatus, ScriptVersionStatus.SUBMITTED.getValue())) {
                            throw new AutoexecCombopSubmittedVersionNotFoundException(autoexecCombopVo.getName());
                        } else if (Objects.equals(versionStatus, ScriptVersionStatus.REJECTED.getValue())) {
                            throw new AutoexecCombopRejectedVersionNotFoundException(autoexecCombopVo.getName());
                        }
                    }
                    autoexecCombopVo.setSpecifyVersionId(maxVersionId);
                }
            }
        }
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(config.getInvokeNotifyPolicyConfig(), AutoexecCombopNotifyPolicyHandler.class);
        config.setInvokeNotifyPolicyConfig(invokeNotifyPolicyConfigVo);
        return autoexecCombopVo;
    }
}
