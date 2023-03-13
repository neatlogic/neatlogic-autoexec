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
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionHasBeenActiveException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 删除组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 15:29
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class AutoexecCombopVersionDeleteApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/version/delete";
    }

    @Override
    public String getName() {
        return "删除组合工具版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "组合工具版本id")
    })
    @Description(desc = "删除组合工具版本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(id);
        if (autoexecCombopVersionVo != null) {
            // 激活版本不能删除
            if (Objects.equals(autoexecCombopVersionVo.getStatus(), ScriptVersionStatus.PASSED.getValue())
                && autoexecCombopVersionVo.getIsActive() == 1) {
                throw new AutoexecCombopVersionHasBeenActiveException();
            }
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
            autoexecCombopService.setOperableButtonList(autoexecCombopVo);
            if (Objects.equals(autoexecCombopVo.getDeletable(), 0)) {
                throw new PermissionDeniedException();
            }
            // 如果这个组合工具只有一个版本，该版本删除的同时删除组合工具基本信息
            List<AutoexecCombopVersionVo> versionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(autoexecCombopVo.getId());
            if (versionList.size() == 1) {
                autoexecCombopMapper.deleteAutoexecCombopById(autoexecCombopVo.getId());
                autoexecCombopMapper.deleteAutoexecCombopAuthorityByCombopId(autoexecCombopVo.getId());
                autoexecCombopService.deleteDependency(autoexecCombopVo);
                if (Objects.equals(autoexecCombopVo.getOperationType(), CombopOperationType.SCRIPT.getValue())
                        || Objects.equals(autoexecCombopVo.getOperationType(), CombopOperationType.TOOL.getValue())) {
                    autoexecCombopMapper.deleteAutoexecOperationGenerateCombop(autoexecCombopVo.getId());
                }
            }
            autoexecCombopVersionMapper.deleteAutoexecCombopVersionById(id);
            autoexecCombopService.deleteDependency(autoexecCombopVersionVo);
        }
        return null;
    }
}
