/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptActiveVersionGetApi extends PublicApiComponentBase {

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
        return "获取操作当前激活版本脚本内容";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operationId", type = ApiParamType.LONG, desc = "操作id", isRequired = true),
            @Param(name = "lastModified", type = ApiParamType.DOUBLE, desc = "最后修改时间（秒，支持小数位）")
    })
    @Output({
            @Param(name = "script", type = ApiParamType.STRING, desc = "脚本内容")
    })
    @Description(desc = "获取操作当前激活版本脚本内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long operationId = jsonObj.getLong("operationId");
        Double lastModified = jsonObj.getDouble("lastModified");
        JSONObject result = new JSONObject();
        if (autoexecScriptMapper.checkScriptIsExistsById(operationId) == 0) {
            throw new AutoexecScriptNotFoundException(operationId);
        }
        AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(operationId);
        if (scriptVersionVo == null) {
            throw new AutoexecScriptVersionHasNoActivedException();
        }
        if (lastModified != null) {
            if (lastModified * 1000 >= scriptVersionVo.getLcd().getTime()) {
                HttpServletResponse resp = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
                if (resp != null) {
                    resp.setStatus(205);
                    resp.getWriter().print(StringUtils.EMPTY);
                }
            }
        }
        String script = autoexecCombopService.getOperationActiveVersionScriptByOperation(scriptVersionVo);
        result.put("script", script);

        return result;
    }


}
