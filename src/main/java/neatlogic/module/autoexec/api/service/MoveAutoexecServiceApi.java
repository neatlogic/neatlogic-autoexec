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

package neatlogic.module.autoexec.api.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SERVICE_MANAGE;
import neatlogic.module.autoexec.dao.mapper.AutoexecServiceMapper;
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
        AutoexecServiceNodeVo targetServiceNode = autoexecServiceMapper.getAutoexecServiceNodeById(targetId);
        if (moveServiceNode == null) {
            throw new AutoexecServiceNotFoundException(id);
        }
        if (targetServiceNode == null) {
            throw new AutoexecServiceNotFoundException(targetId);
        }

        LRCodeManager.moveTreeNode("autoexec_service", "id", "parent_id", id, MoveType.getMoveType(moveType), targetId);
        moveServiceNode.setParentId(targetId);
        if (autoexecServiceMapper.checkAutoexecServiceNameIsRepeat(moveServiceNode) > 0) {
            throw new AutoexecServiceNameIsRepeatException(moveServiceNode.getName());
        }
        return null;
    }
}
