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
package neatlogic.module.autoexec.api.type;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecTypeAuthorityAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeAuthVo;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.exception.AutoexecTypeNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/12/6 14:37
 */

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecTypeApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;
    @Override
    public String getName() {
        return "获取自动化工具分类";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/type/get";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "工具分类 id")
    })
    @Output({
            @Param(explode = AutoexecTypeVo.class, desc = "自动化工具分信息")
    })
    @Description(desc = "获取自动化工具分类")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(id);
        if (autoexecTypeVo == null) {
            throw new AutoexecTypeNotFoundException(id);
        }
        List<AutoexecTypeAuthVo> authList = autoexecTypeMapper.getAutoexecTypeAuthListByTypeIdAndAction(id, AutoexecTypeAuthorityAction.REVIEW.getValue());
        if (CollectionUtils.isNotEmpty(authList)) {
            List<String> reviewAuthList = new ArrayList<>();
            for (AutoexecTypeAuthVo authVo : authList) {
                reviewAuthList.add(authVo.getAuthType() + "#" + authVo.getAuthUuid());
            }
            autoexecTypeVo.setReviewAuthList(reviewAuthList);
        }
        return autoexecTypeVo;
    }
}
