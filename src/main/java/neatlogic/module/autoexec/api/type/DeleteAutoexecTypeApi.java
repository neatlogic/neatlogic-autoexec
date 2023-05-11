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

package neatlogic.module.autoexec.api.type;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecTypeType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.exception.AutoexecTypeHasBeenReferredException;
import neatlogic.framework.autoexec.exception.AutoexecTypeIsFactoryException;
import neatlogic.framework.autoexec.exception.AutoexecTypeNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.crossover.IDeployTypeCrossoverMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAutoexecTypeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Override
    public String getToken() {
        return "autoexec/type/delete";
    }

    @Override
    public String getName() {
        return "删除工具分类";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "类型ID")})
    @Output({})
    @Description(desc = "删除工具分类")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecTypeVo type = autoexecTypeMapper.getTypeById(id);
        if (type == null) {
            throw new AutoexecTypeNotFoundException(id);
        }
        if (StringUtils.equals(AutoexecTypeType.FACTORY.getValue(), type.getType())) {
            throw new AutoexecTypeIsFactoryException(id, type.getName());
        }
        // 已经被工具或脚本引用的分类不可删除
        if (autoexecTypeMapper.checkTypeHasBeenReferredById(id) > 0) {
            throw new AutoexecTypeHasBeenReferredException(type.getName());
        }
        autoexecTypeMapper.deleteTypeById(id);
        autoexecTypeMapper.deleteTypeAuthByTypeId(id);
        IDeployTypeCrossoverMapper iDeployTypeCrossoverMapper = CrossoverServiceFactory.getApi(IDeployTypeCrossoverMapper.class);
        iDeployTypeCrossoverMapper.deleteTypeActiveByTypeId(id);
        return null;
    }
}
