/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptArgumentVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.file.FileAccessDeniedException;
import neatlogic.framework.exception.file.FileNotFoundException;
import neatlogic.framework.exception.file.FileTypeHandlerNotFoundException;
import neatlogic.framework.file.core.FileOperationType;
import neatlogic.framework.file.core.FileTypeHandlerFactory;
import neatlogic.framework.file.core.IFileTypeHandler;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.ZipUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptExportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

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
        List<AutoexecScriptVo> scriptBaseInfoVoList = autoexecScriptMapper.getAutoexecScriptBaseInfoByIdList(existedIdList);

        Map<Long, AutoexecScriptVersionVo> lineMap = new HashMap<>();
        Map<Long, AutoexecScriptArgumentVo> argumentMap = new HashMap<>();
        Map<Long, List<AutoexecParamVo>> paramMap = new HashMap<>();
        Map<Long, List<Long>> useLibMap = new HashMap<>();
        Map<Long, FileVo> fileVoMap = new HashMap<>();
        List<AutoexecScriptVersionVo> versionVoIncludeLineList = autoexecScriptMapper.getActiveVersionListIncludeLineByScriptIdList(existedIdList);
        List<AutoexecScriptVersionVo> versionVoIncludeArgumentList = autoexecScriptMapper.getActiveVersionIncludeArgumentByScriptIdList(existedIdList);
        List<AutoexecScriptVo> scriptVoIncludeParamList = autoexecScriptMapper.getScriptListIncludeActiveVersionParamByScriptIdList(existedIdList);
        List<AutoexecScriptVersionVo> versionVoIncludeUseLibNameList = autoexecScriptMapper.getActiveVersionIncludeUseLibAndNameByScriptIdList(existedIdList);
        List<AutoexecScriptVersionVo> versionVoIncludeFileList = autoexecScriptMapper.getActiveVersionIncludeFileByScriptIdList(existedIdList);

        if (CollectionUtils.isNotEmpty(versionVoIncludeLineList)) {
            lineMap = versionVoIncludeLineList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, e -> e));
        }
        if (CollectionUtils.isNotEmpty(versionVoIncludeArgumentList)) {
            argumentMap = versionVoIncludeArgumentList.stream().filter(e -> e.getArgument() != null).collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, AutoexecScriptVersionVo::getArgument));
        }
        if (CollectionUtils.isNotEmpty(scriptVoIncludeParamList)) {
            paramMap = scriptVoIncludeParamList.stream().collect(Collectors.toMap(AutoexecScriptVo::getId, AutoexecScriptVo::getParamList));
        }
        if (CollectionUtils.isNotEmpty(versionVoIncludeUseLibNameList)) {
            useLibMap = versionVoIncludeUseLibNameList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, AutoexecScriptVersionVo::getUseLib));
        }
        if (CollectionUtils.isNotEmpty(versionVoIncludeFileList)) {
            fileVoMap = versionVoIncludeFileList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, AutoexecScriptVersionVo::getPackageFile));
        }
        String fileName = FileUtil.getEncodedFileName("自定义工具." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pak");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (AutoexecScriptVo scriptVo : scriptBaseInfoVoList) {
                AutoexecScriptVersionVo version = lineMap.get(scriptVo.getId());
                if (version == null) {
                    continue;
                }
                //脚本基本信息
                scriptVo.setParser(version.getParser());
                scriptVo.setArgument(argumentMap.get(scriptVo.getId()));
                scriptVo.setParamList(paramMap.get(scriptVo.getId()));
                scriptVo.setLineList(version.getLineList());
                scriptVo.setUseLib(useLibMap.get(scriptVo.getId()));
                scriptVo.setPackageFileId(version.getPackageFileId());

                //脚本附带tar文件
                if (StringUtils.equals(scriptVo.getParser(), ScriptParser.PACKAGE.getValue()) && scriptVo.getPackageFileId() != null) {
                    FileVo fileVo = fileVoMap.get(scriptVo.getId());
                    if (fileVo != null) {
                        scriptVo.setPackageFile(fileVo);
                        zos.putNextEntry(new ZipEntry(scriptVo.getName() + ".json"));
                        zos.write(JSONObject.toJSONBytes(scriptVo));
                        String userUuid = UserContext.get().getUserUuid();
                        IFileTypeHandler fileTypeHandler = FileTypeHandlerFactory.getHandler(fileVo.getType());
                        if (fileTypeHandler != null) {
                            if (StringUtils.equals(userUuid, fileVo.getUserUuid()) || fileTypeHandler.valid(userUuid, fileVo, paramObj)) {
                                InputStream inputStream = neatlogic.framework.common.util.FileUtil.getData(fileVo.getPath());
                                if (inputStream != null) {
                                    File inputFile = new File(fileVo.getName());
                                    FileUtils.copyInputStreamToFile(inputStream, inputFile);
                                    ZipUtil.zip(zos, inputFile, fileVo.getName());
                                    zos.closeEntry();
                                    inputStream.close();
                                }
                            } else {
                                throw new FileAccessDeniedException(fileVo.getName(), FileOperationType.DOWNLOAD.getText());
                            }
                        } else {
                            throw new FileTypeHandlerNotFoundException(fileVo.getType());
                        }
                    } else {
                        throw new FileNotFoundException(scriptVo.getPackageFileId());
                    }
                } else {
                    zos.putNextEntry(new ZipEntry(scriptVo.getName() + ".json"));
                    zos.write(JSONObject.toJSONBytes(scriptVo));
                }
            }
        }
        return null;
    }
}
