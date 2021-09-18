/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptExportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/export";
    }

    @Override
    public String getName() {
        return "导出脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "脚本ID列表"),
    })
    @Output({
    })
    @Description(desc = "导出脚本")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Long> idList = paramObj.getJSONArray("idList").toJavaList(Long.class);
        List<Long> existedIdList = autoexecScriptMapper.checkScriptIdListExists(idList);
        idList.removeAll(existedIdList);
        if (CollectionUtils.isNotEmpty(idList)) {
            throw new AutoexecScriptNotFoundException(StringUtils.join(idList, ","));
        }
        String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"),
                "自定义工具." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pak");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Long id : existedIdList) {
                AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(id);
                List<AutoexecScriptVersionVo> versionList = autoexecScriptService
                        .getScriptVersionDetailListByScriptId(new AutoexecScriptVersionVo(id, ScriptVersionStatus.PASSED.getValue()));
                script.setVersionList(versionList);
                zos.putNextEntry(new ZipEntry(script.getName() + ".json"));
                zos.write(JSONObject.toJSONBytes(script));
                zos.closeEntry();
            }
        }
        return null;
    }


}
