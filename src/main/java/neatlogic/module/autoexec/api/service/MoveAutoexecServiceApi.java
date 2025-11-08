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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SERVICE_MANAGE;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecServiceNameIsRepeatException;
import neatlogic.framework.autoexec.exception.AutoexecServiceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.lrcode.constvalue.MoveType;
import neatlogic.framework.lrcode.exception.MoveTargetNodeIllegalException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@AuthAction(action = AUTOEXEC_SERVICE_MANAGE.class)
@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class MoveAutoexecServiceApi extends PrivateApiComponentBase {

    @Resource
    AutoexecServiceMapper autoexecServiceMapper;

    @Override
    public String getToken() {
        return "autoexec/service/move";
    }

    @Override
    public String getName() {
        return "移动服务目录信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.STRING, isRequired = true, desc = "被移动的服务目录id"),
            @Param(name = "targetId", type = ApiParamType.STRING, isRequired = true, desc = "目标节点id"),
            @Param(name = "moveType", type = ApiParamType.ENUM, rule = "inner,prev,next", isRequired = true, desc = "移动类型")
    })
    @Description(desc = "移动服务目录信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Long targetId = paramObj.getLong("targetId");
        String moveType = paramObj.getString("moveType");

        if (id.equals(targetId)) {
            throw new MoveTargetNodeIllegalException();
        }
        AutoexecServiceNodeVo moveServiceNode = autoexecServiceMapper.getAutoexecServiceNodeById(id);
        if (moveServiceNode == null) {
            throw new AutoexecServiceNotFoundException(id);
        }
        if (targetId != 0L) {
            AutoexecServiceNodeVo targetServiceNode = autoexecServiceMapper.getAutoexecServiceNodeById(targetId);
            if (targetServiceNode == null) {
                throw new AutoexecServiceNotFoundException(targetId);
            }
        }

        LRCodeManager.moveTreeNode("autoexec_service", "id", "parent_id", id, MoveType.getMoveType(moveType), targetId);
        moveServiceNode.setParentId(targetId);
        if (autoexecServiceMapper.checkAutoexecServiceNameIsRepeat(moveServiceNode) > 0) {
            throw new AutoexecServiceNameIsRepeatException(moveServiceNode.getName());
        }
        return null;
    }
}
