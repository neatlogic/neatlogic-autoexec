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
