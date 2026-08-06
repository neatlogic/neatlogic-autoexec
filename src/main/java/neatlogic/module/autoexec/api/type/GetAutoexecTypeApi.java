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
        return "nmaa.getautoexectypeapi.getname";
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
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "nmaa.getautoexectypeapi.input.param.desc.id")
    })
    @Output({
            @Param(explode = AutoexecTypeVo.class, desc = "nmaa.getautoexectypeapi.output.param.desc.return")
    })
    @Description(desc = "nmaa.getautoexectypeapi.getname")
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
