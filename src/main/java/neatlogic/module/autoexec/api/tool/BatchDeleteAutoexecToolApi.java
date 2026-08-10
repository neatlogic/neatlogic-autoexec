/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.api.tool;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.AutoexecOperationIndexAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.service.AutoexecOperationChangeDispatcher;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class BatchDeleteAutoexecToolApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    private AutoexecOperationChangeDispatcher operationChangeDispatcher;

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
            operationChangeDispatcher.notifyAfterCommit("tool", idList, AutoexecOperationIndexAction.DELETE);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/tool/batch/delete";
    }
}
