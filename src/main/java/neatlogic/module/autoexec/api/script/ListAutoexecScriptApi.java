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
package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/10/14 10:49
 */

@Service
@AuthAction(action = AUTOEXEC.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAutoexecScriptApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Override
    public String getName() {
        return "查询脚本列表";
    }

    @Override
    public String getToken() {
        return "autoexec/script/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "分类id列表"),
            @Param(name = "versionStatus", type = ApiParamType.ENUM, rule = "draft,submitted,passed,rejected", desc = "状态"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的脚本id列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecScriptVo[].class, desc = "查询脚本列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "脚本列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AutoexecScriptVo scriptVo = JSON.toJavaObject(paramObj, AutoexecScriptVo.class);
        int rowNum = autoexecScriptMapper.searchScriptCount(scriptVo);
        List<AutoexecScriptVo> returnList = new ArrayList<>();
        if (rowNum > 0) {
            scriptVo.setRowNum(rowNum);
            returnList = autoexecScriptMapper.searchScript(scriptVo);
        }
        return TableResultUtil.getResult(returnList, scriptVo);
    }
}
