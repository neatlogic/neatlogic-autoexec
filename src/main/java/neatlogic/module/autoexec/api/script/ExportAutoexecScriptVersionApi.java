/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author longrf
 * @date 2022/10/11 16:57
 */

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportAutoexecScriptVersionApi extends PrivateBinaryStreamApiComponentBase {

    private static final Logger logger = LoggerFactory.getLogger(ExportAutoexecScriptVersionApi.class);

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return "导出某个版本的脚本内容";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/script/version/export";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "脚本id"),
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "脚本版本id"),
    })
    @Output({
    })
    @Description(desc = "导出某个版本的脚本内容")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long id = paramObj.getLong("id");
        Long versionId = paramObj.getLong("versionId");
        AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
        if (scriptVo == null) {
            throw new AutoexecScriptNotFoundException(id);
        }
        AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (scriptVersionVo == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + FileUtil.getEncodedFileName(scriptVo.getName() + ".txt") + "\"");

        String scriptVersionContent = autoexecCombopService.getScriptVersionContent(scriptVersionVo);
        BufferedOutputStream buff = null;
        ServletOutputStream outStr = null;
        try {
            outStr = response.getOutputStream();
            buff = new BufferedOutputStream(outStr);
            buff.write(scriptVersionContent.getBytes(StandardCharsets.UTF_8));
            buff.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (buff != null) {
                buff.close();
            }
            if (buff != null) {
                outStr.close();
            }
        }
        return null;
    }
}