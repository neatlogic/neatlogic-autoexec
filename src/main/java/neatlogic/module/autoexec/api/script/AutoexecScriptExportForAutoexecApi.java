/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.file.FileAccessDeniedException;
import neatlogic.framework.exception.file.FileTypeHandlerNotFoundException;
import neatlogic.framework.file.core.FileOperationType;
import neatlogic.framework.file.core.FileTypeHandlerFactory;
import neatlogic.framework.file.core.IFileTypeHandler;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.ZipUtil;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptExportForAutoexecApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Autowired
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "autoexec/script/export/forautoexec";
    }

    @Override
    public String getName() {
        return "导出脚本(供外部调用)";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "catalogName", type = ApiParamType.STRING, desc = "目录名称（完整路径）"),
            @Param(name = "catalogList", type = ApiParamType.JSONARRAY, desc = "目录名称（完整路径）列表"),
    })
    @Output({
    })
    @Description(desc = "导出脚本(供外部调用)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Set<Long> catalogIdSet = new HashSet<>();
        String catalogName = paramObj.getString("catalogName");
        JSONArray catalogList = paramObj.getJSONArray("catalogList");
        if (StringUtils.isNotBlank(catalogName) && !"/".equals(catalogName)) {
            Long catalogId = autoexecScriptService.getCatalogIdByCatalogPath(catalogName);
            if (catalogId != null) {
                AutoexecCatalogVo catalogVo = autoexecCatalogMapper.getAutoexecCatalogById(catalogId);
                if (catalogVo != null) {
                    catalogIdSet.addAll(autoexecCatalogMapper.getChildrenByLftRht(catalogVo).stream().map(AutoexecCatalogVo::getId).collect(Collectors.toList()));
                }
            }
        } else if (CollectionUtils.isNotEmpty(catalogList) && !catalogList.contains("/")) {
            for (int i = 0; i < catalogList.size(); i++) {
                String catalogPath = catalogList.getString(i);
                Long catalogId = autoexecScriptService.getCatalogIdByCatalogPath(catalogPath);
                if (catalogId != null) {
                    catalogIdSet.add(catalogId);
                }
            }
            if (catalogIdSet.size() > 0) {
                List<AutoexecCatalogVo> catalogVoList = autoexecCatalogMapper.getCatalogListByIdList(new ArrayList<>(catalogIdSet));
                catalogIdSet.clear();
                if (catalogVoList.size() > 0) {
                    for (AutoexecCatalogVo catalogVo : catalogVoList) {
                        catalogIdSet.addAll(autoexecCatalogMapper.getChildrenByLftRht(catalogVo).stream().map(AutoexecCatalogVo::getId).collect(Collectors.toList()));
                    }
                }
            }
        }
        // 查询有激活版本的脚本
        List<Long> idList = autoexecScriptMapper.getAutoexecScriptIdListWhichHasActiveVersionByCatalogIdList(new ArrayList<>(catalogIdSet));
        if (!idList.isEmpty()) {

            List<Long> packageFileIdList = new ArrayList<>();
            String fileName = FileUtil.getEncodedFileName("自定义工具." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                JSONArray jsonArray = new JSONArray();
                for (Long id : idList) {
                    AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(id);
                    AutoexecCatalogVo _catalog = autoexecCatalogMapper.getAutoexecCatalogById(script.getCatalogId());
                    if (_catalog != null) {
                        List<AutoexecCatalogVo> upwardList = autoexecCatalogMapper.getParentListAndSelfByLR(_catalog.getLft(), _catalog.getRht());
                        script.setCatalogPath(upwardList.stream().map(AutoexecCatalogVo::getName).collect(Collectors.joining("/")));
                    }
                    AutoexecScriptVersionVo version = autoexecScriptMapper.getActiveVersionWithUseLibsByScriptId(id);
                    script.setParser(version.getParser());
                    script.setArgument(autoexecScriptMapper.getArgumentByVersionId(version.getId()));
                    script.setParamList(autoexecScriptMapper.getAutoexecParamVoListByVersionId(version.getId()));
                    script.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
                    if (StringUtils.equals(script.getParser(), ScriptParser.PACKAGE.getValue()) && version.getPackageFileId() != null) {
                        FileVo file = fileMapper.getFileById(version.getPackageFileId());
                        if (file != null) {
                            packageFileIdList.add(version.getPackageFileId());
                            script.setPackageFileName(file.getName());
                        }
                    }
                    if (CollectionUtils.isNotEmpty(version.getUseLib())) {
                        List<AutoexecOperationVo> scriptList = autoexecScriptMapper.getScriptListByIdList(version.getUseLib());
                        if (CollectionUtils.isNotEmpty(scriptList)) {
                            Set<Long> scriptCatalogIdSet = scriptList.stream().map(AutoexecOperationVo::getCatalogId).collect(Collectors.toSet());
                            if (CollectionUtils.isNotEmpty(scriptCatalogIdSet)) {
                                List<AutoexecCatalogVo> scriptCatalogList = autoexecCatalogMapper.getAutoexecFullCatalogByIdList(new ArrayList<>(scriptCatalogIdSet));
                                if (CollectionUtils.isNotEmpty(scriptCatalogList)) {
                                    List<String> scriptNameList = new ArrayList<>();
                                    Map<Long, String> scriptCatalogMap = scriptCatalogList.stream().collect(Collectors.toMap(AutoexecCatalogVo::getId, AutoexecCatalogVo::getFullCatalogName));
                                    for (AutoexecOperationVo operationVo : scriptList) {
                                        if (scriptCatalogMap.containsKey(operationVo.getCatalogId())) {
                                            scriptNameList.add(scriptCatalogMap.get(operationVo.getCatalogId()) + "/" + operationVo.getName());
                                        }
                                        script.setUseLibName(scriptNameList);
                                    }
                                }
                            }
                        }
                    }
                    jsonArray.add(JSONObject.parseObject(JSON.toJSONString(script, SerializerFeature.DisableCircularReferenceDetect)));
                }
                if (CollectionUtils.isNotEmpty(jsonArray)) {
                    zos.putNextEntry(new ZipEntry("scriptInfo.json"));
                    zos.write(JSONArray.toJSONBytes(jsonArray));
                }

                if (CollectionUtils.isNotEmpty(packageFileIdList)) {
                    List<FileVo> fileVoList = fileMapper.getFileListByIdList(packageFileIdList);
                    for (FileVo fileVo : fileVoList) {
                        String userUuid = UserContext.get().getUserUuid();
                        IFileTypeHandler fileTypeHandler = FileTypeHandlerFactory.getHandler(fileVo.getType());
                        if (fileTypeHandler != null) {
                            if (StringUtils.equals(userUuid, fileVo.getUserUuid()) || fileTypeHandler.valid(userUuid, fileVo, paramObj)) {
                                try (InputStream inputStream = neatlogic.framework.common.util.FileUtil.getData(fileVo.getPath())) {
                                    if (inputStream != null) {
                                        File inputFile = new File(fileVo.getName());
                                        FileUtils.copyInputStreamToFile(inputStream, inputFile);
                                        ZipUtil.zip(zos, inputFile, fileVo.getName());
                                        zos.closeEntry();
                                    }
                                }
                            } else {
                                throw new FileAccessDeniedException(fileVo.getName(), FileOperationType.DOWNLOAD.getText());
                            }
                        } else {
                            throw new FileTypeHandlerNotFoundException(fileVo.getType());
                        }
                    }
                }
            }
        }
        return null;
    }
}
