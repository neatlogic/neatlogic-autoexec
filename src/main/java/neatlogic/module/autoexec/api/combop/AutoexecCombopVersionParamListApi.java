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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 查询组合工具顶层参数列表接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopVersionParamListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/version/param/list";
    }

    @Override
    public String getName() {
        return "nmaa.autoexeccombopversionparamlistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopVersionId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.combopversionid")
    })
    @Output({
            @Param(explode = AutoexecParamVo[].class, desc = "term.autoexec.paramlist")
    })
    @Description(desc = "nmaa.autoexeccombopversionparamlistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopVersionId = jsonObj.getLong("combopVersionId");
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(combopVersionId);
        if (autoexecCombopVersionVo == null) {
            throw new AutoexecCombopVersionNotFoundException(combopVersionId);
        }
        AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
        List<AutoexecParamVo> autoexecParamVoList = config.getRuntimeParamList();
//        for (AutoexecParamVo autoexecParamVo : autoexecParamVoList) {
//            autoexecService.mergeConfig(autoexecParamVo);
//        }
        return autoexecParamVoList;
    }
}
