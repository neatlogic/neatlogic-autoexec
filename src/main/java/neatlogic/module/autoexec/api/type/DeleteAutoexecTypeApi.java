/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.type;

import com.alibaba.fastjson.JSONObject;
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
