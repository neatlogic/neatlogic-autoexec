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

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Objects;

@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecScriptActiveVersionApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/script/active/version/get";
    }

    @Override
    public String getName() {
        return "nmaa.getautoexecscriptactiveversionapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.LONG, desc = "term.autoexec.operationid", isRequired = true),
            @Param(name = "lastModified", type = ApiParamType.DOUBLE, desc = "nmaa.common.input.param.desc.lastmodified")
    })
    @Output({
            @Param(name = "scriptVersionVo", explode = AutoexecScriptVersionVo.class, desc = "nmaa.getautoexecscriptactiveversionapi.output.param.desc.scriptversionvo")
    })
    @Description(desc = "nmaa.getautoexecscriptactiveversionapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long operationId = jsonObj.getLong("operationId");
        BigDecimal lastModified = null;
        if (jsonObj.getDouble("lastModified") != null) {
            lastModified = new BigDecimal(Double.toString(jsonObj.getDouble("lastModified")));
        }
        if (autoexecScriptMapper.checkScriptIsExistsById(operationId) == 0) {
            throw new AutoexecScriptNotFoundException(operationId);
        }
        AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
        if (scriptVersionVo == null) {
            throw new AutoexecScriptVersionHasNoActivedException(operationId.toString());
        }
        AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(scriptVersionVo.getScriptId());
        if (scriptVo == null) {
            throw new AutoexecScriptNotFoundException(scriptVersionVo.getScriptId());
        }
        if (lastModified != null) {
            if (lastModified.multiply(new BigDecimal("1000")).longValue() >= scriptVersionVo.getLcd().getTime()) {
                HttpServletResponse resp = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
                if (resp != null) {
                    resp.setStatus(205);
                    resp.getWriter().print(StringUtils.EMPTY);
                }
            }
        }
        JSONObject result = new JSONObject();
        result.put("script", autoexecCombopService.getScriptVersionContent(scriptVersionVo));
        result.put("config", new JSONObject() {{
            put("scriptName", scriptVo.getName());
            put("parser", scriptVersionVo.getParser());
        }});
        return result;
    }


}
