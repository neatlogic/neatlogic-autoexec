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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.service.AutoexecService;
import org.apache.commons.collections4.CollectionUtils;
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
public class AutoexecCombopParamListApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/combop/param/list";
    }

    @Override
    public String getName() {
        return "nmaa.autoexeccombopparamlistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "term.autoexec.combopid"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "common.versionid")
    })
    @Output({
            @Param(explode = AutoexecParamVo[].class, desc = "term.autoexec.paramlist")
    })
    @Description(desc = "nmaa.autoexeccombopparamlistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        if (autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        Long versionId = jsonObj.getLong("versionId");
        if (versionId == null) {
            versionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopId);
            if (versionId == null) {
                return new JSONArray();
            }
        }
        AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(versionId);
        if (autoexecCombopVersionVo == null) {
            return new JSONArray();
        }
        AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
        if (config == null) {
            return new JSONArray();
        }
        List<AutoexecParamVo> autoexecParamVoList = config.getRuntimeParamList();
        if (CollectionUtils.isEmpty(autoexecParamVoList)) {
            return new JSONArray();
        }
        for (AutoexecParamVo autoexecParamVo : autoexecParamVoList) {
            autoexecService.mergeConfig(autoexecParamVo);
        }
        return autoexecParamVoList;
    }
}
