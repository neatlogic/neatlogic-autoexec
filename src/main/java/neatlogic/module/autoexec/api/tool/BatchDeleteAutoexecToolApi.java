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

package neatlogic.module.autoexec.api.tool;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class BatchDeleteAutoexecToolApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Override
    public String getName() {
        return "nmaat.batchdeleteautoexectoolapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "importTime", type = ApiParamType.LONG, isRequired = true, desc = "common.editdate")
    })
    @Output({})
    @Description(desc = "nmaat.batchdeleteautoexectoolapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long importTime = paramObj.getLong("importTime");
        int count = autoexecToolMapper.getToolCountByImportTime(importTime);
        if (count == 0) {
            return null;
        }
        List<Long> idList = autoexecToolMapper.getToolIdListByExcludeImportTime(importTime);
        if (CollectionUtils.isNotEmpty(idList)) {
            autoexecToolMapper.deleteToolByIdList(idList);
            for (Long id : idList) {
                autoexecProfileMapper.deleteProfileOperationByOperationId(id);
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/tool/batch/delete";
    }
}
