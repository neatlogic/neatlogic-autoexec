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
