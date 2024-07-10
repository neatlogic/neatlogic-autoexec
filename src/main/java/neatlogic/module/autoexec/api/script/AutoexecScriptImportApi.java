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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.constvalue.ScriptAction;
import neatlogic.framework.autoexec.constvalue.ScriptExecMode;
import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptArgumentVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.file.FileExtNotAllowedException;
import neatlogic.framework.exception.file.FileNotUploadException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import neatlogic.module.autoexec.service.AutoexecService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptImportApi extends PrivateBinaryStreamApiComponentBase {

    //Logger logger = LoggerFactory.getLogger(AutoexecScriptImportApi.class);

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Resource
    private AutoexecService autoexecService;

    @Resource
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "autoexec/script/import";
    }

    @Override
    public String getName() {
        return "导入脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "isReplace", type = ApiParamType.INTEGER, desc = "是否覆盖原来的待审核脚本，默认不覆盖"),
            @Param(name = "scriptIdList", type = ApiParamType.STRING, desc = "需要导入的脚本id列表"),
    })
    @Output({
            @Param(name = "successCount", type = ApiParamType.INTEGER, desc = "导入成功数量"),
            @Param(name = "failureCount", type = ApiParamType.INTEGER, desc = "导入失败数量"),
            @Param(name = "failureReasonList", type = ApiParamType.JSONARRAY, desc = "失败原因")
    })
    @Description(desc = "导入脚本")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject resultObj = new JSONObject();
        List<Long> scriptIdList = new ArrayList<>();
        int isReplace = paramObj.getInteger("isReplace") != null ? paramObj.getInteger("isReplace") : 0;
        if (isReplace == 1) {
            if (!paramObj.containsKey("scriptIdList")) {
                throw new ParamNotExistsException("scriptIdList");
            } else {
                String scriptIds = paramObj.getString("scriptIdList");
                String[] split = scriptIds.split(",");
                for (String str : split) {
                    scriptIdList.add(Long.valueOf(str));
                }
            }
        }

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        Map<Long, List<Long>> scriptVersionUseLibMap = new HashMap<>();
        Map<String, File> scriptVersionFileMap = new HashMap<>();
        Map<String, Long> fileNameIdMap = new HashMap<>();
        JSONArray failReasonList = new JSONArray();
        byte[] buf = new byte[1024];
        int successCount = 0;
        int failureCount = 0;
        for (Map.Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            try (ZipInputStream zis = new ZipInputStream(multipartFile.getInputStream())) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        int len;
                        while ((len = zis.read(buf)) != -1) {
                            out.write(buf, 0, len);
                        }
                        AutoexecScriptVo scriptVo;
                        try {
                            scriptVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), new TypeReference<AutoexecScriptVo>() {
                            });
                            //System.out.println(scriptVo.getName());
                            if (StringUtils.equals(scriptVo.getParser(), ScriptParser.PACKAGE.getValue()) && scriptVo.getPackageFile() != null) {
                                FileVo packageFile = scriptVo.getPackageFile();
                                packageFile.setId(null);
                            }
                        } catch (JSONException e) {
                            System.out.println(ze.getName());
                            File file = new File(ze.getName());
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(out.toByteArray());
                            fos.close();
                            scriptVersionFileMap.put(ze.getName(), file);
                            continue;
                        }

                        if (CollectionUtils.isNotEmpty(scriptIdList) && !scriptIdList.contains(scriptVo.getId())) {
                            continue;
                        }
                        JSONObject result = save(scriptVo, isReplace);
                        if (result.containsKey("failReasonList")) {
                            failReasonList.add(result);
                            failureCount++;
                        } else if (result.containsKey("hasSubmittedVersion") && result.getBoolean("hasSubmittedVersion")) {
                            result.put("isWarn", 1);
                            result.put("id", scriptVo.getId());
                            result.put("item", scriptVo.getName());
                            result.put("fileName", multipartFile.getOriginalFilename());
                            failReasonList.add(result);
                            failureCount++;
                        } else if (result.containsKey("newVersionId")) {
                            //脚本版本的依赖工具列表
                            if (CollectionUtils.isNotEmpty(scriptVo.getUseLib())) {
                                scriptVersionUseLibMap.put(result.getLong("newVersionId"), scriptVo.getUseLib());
                            }
                            if (result.containsKey("newPackageFileName") && result.containsKey("newPackageFileId")) {
                                fileNameIdMap.put(result.getString("newPackageFileName"), result.getLong("newPackageFileId"));
                            }
                            successCount++;
                        }
                        out.reset();
                    }
                }
            } catch (IOException e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }

        //等所有的脚本保存完之后,再存脚本版本的依赖工具，避免情况：需要依赖的脚本在后面才存进去而导致前面的脚本没有依赖成功
        if (MapUtils.isNotEmpty(scriptVersionUseLibMap)) {
            Set<Long> libScriptIdList = new HashSet<>();
            for (List<Long> useLibList : scriptVersionUseLibMap.values()) {
                libScriptIdList.addAll(useLibList);
            }
            List<Long> existLibScriptList = autoexecScriptMapper.checkScriptIdListExists(new ArrayList<>(libScriptIdList));
            if (CollectionUtils.isNotEmpty(existLibScriptList)) {
                for (Map.Entry<Long, List<Long>> entry : scriptVersionUseLibMap.entrySet()) {
                    List<Long> useLib = entry.getValue();
                    useLib = useLib.stream().filter(existLibScriptList::contains).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(useLib)) {
                        autoexecScriptMapper.insertScriptVersionUseLib(entry.getKey(), useLib);
                    }
                }
            }
        }

        if (MapUtils.isNotEmpty(fileNameIdMap)) {
            String tenantUuid = TenantContext.get().getTenantUuid();
            for (Map.Entry<String, Long> entry : fileNameIdMap.entrySet()) {
                File file = scriptVersionFileMap.get(entry.getKey());
                FileVo fileVo = new FileVo();
                fileVo.setId(entry.getValue());
                fileVo.setName(entry.getKey());
                fileVo.setSize(file.length());
                fileVo.setUserUuid(SystemUser.SYSTEM.getUserUuid());
                fileVo.setType("autoexec");
                fileVo.setContentType("application/x-tar");
                String filePath = FileUtil.saveData(tenantUuid, Files.newInputStream(file.toPath()), fileVo);
                fileVo.setPath(filePath);
                fileMapper.insertFile(fileVo);
            }
        }

        resultObj.put("successCount", successCount);
        resultObj.put("failed", failureCount);
        if (CollectionUtils.isNotEmpty(failReasonList)) {
            resultObj.put("failureReasonList", failReasonList);
        }
        return resultObj;
    }

    private JSONObject save(AutoexecScriptVo scriptVo, int isReplace) {
        JSONObject result = new JSONObject();
        List<String> failReasonList = new ArrayList<>();
        Long id = scriptVo.getId();
        String name = scriptVo.getName();
        String fullCatalogName = scriptVo.getFullCatalogName();
        AutoexecScriptVersionVo submittedVersionVo = autoexecScriptMapper.getVersionByScriptIdAndVersionStatus(id, ScriptVersionStatus.SUBMITTED.getValue());
        if (submittedVersionVo != null) {
            if (isReplace == 1) {
                if (autoexecScriptMapper.getScriptLockById(id) == null) {
                    throw new AutoexecScriptNotFoundException(id);
                }
                boolean hasOnlyOneVersion = autoexecScriptMapper.getVersionCountByScriptId(id) == 1;
                autoexecScriptMapper.deleteParamByVersionId(submittedVersionVo.getId());
                autoexecScriptMapper.deleteArgumentByVersionId(submittedVersionVo.getId());
                autoexecScriptMapper.deleteScriptLineByVersionId(submittedVersionVo.getId());
                autoexecScriptMapper.deleteScriptVersionLibByScriptVersionId(submittedVersionVo.getId());
                autoexecScriptMapper.deleteVersionByVersionId(submittedVersionVo.getId());
                // 只剩一个版本时，直接删除整个脚本
                if (hasOnlyOneVersion) {
                    autoexecScriptMapper.deleteScriptById(id);
                } else {
                    JSONObject auditContent = new JSONObject();
                    auditContent.put("version", submittedVersionVo.getVersion());
                    AutoexecScriptAuditVo auditVo = new AutoexecScriptAuditVo(id, submittedVersionVo.getId(), ScriptAction.DELETE.getValue(), auditContent);
                    autoexecScriptService.audit(auditVo);
                }
            } else {
                result.put("hasSubmittedVersion", true);
                return result;
            }
        }
        int index = 0;
        while (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
            index++;
            scriptVo.setName(name + "_" + index);
        }
        Long typeId = autoexecTypeMapper.getTypeIdByName(scriptVo.getTypeName());
        scriptVo.setTypeId(typeId);
        if (typeId == null) {
            failReasonList.add("不存在的工具类型：" + scriptVo.getTypeName());
        }
        // 根据全目录名称匹配，只有当目录存在时才保存目录id，目录名称为空或目录不存在时，挂在所有下
        scriptVo.setCatalogId(AutoexecCatalogVo.ROOT_ID);
        if (StringUtils.isNotBlank(fullCatalogName)) {
            AutoexecCatalogVo catalog = autoexecCatalogMapper.getAutoexecCatalogByFullName(fullCatalogName);
            if (catalog != null) {
                scriptVo.setCatalogId(catalog.getId());
            }
        }

        //不是库文件，才需要检验操作级别和执行方式的必填
        if (scriptVo.getIsLib() == 0) {
            Long risk = autoexecRiskMapper.getRiskIdByName(scriptVo.getRiskName());
            scriptVo.setRiskId(risk);
            if (risk == null) {
                failReasonList.add("不存在的操作级别：" + scriptVo.getRiskName());
            }
            if (ScriptExecMode.getExecMode(scriptVo.getExecMode()) == null) {
                failReasonList.add("不存在的执行方式：" + scriptVo.getExecMode());
            }
        }

        String defaultProfileName = scriptVo.getDefaultProfileName();
        if (StringUtils.isNotBlank(defaultProfileName)) {
            AutoexecProfileVo profile = autoexecProfileMapper.getProfileVoByName(defaultProfileName);
            if (profile == null || autoexecProfileMapper.getProfileIdByProfileIdAndOperationId(profile.getId(), scriptVo.getId()) == null) {
                scriptVo.setDefaultProfileId(null);
            } else {
                scriptVo.setDefaultProfileId(profile.getId());
            }
        }
        if (CollectionUtils.isEmpty(failReasonList)) {
            AutoexecScriptVo oldScriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
            if (oldScriptVo != null) {
                scriptVo.setLcu(UserContext.get().getUserUuid());
                autoexecScriptMapper.updateScriptBaseInfo(scriptVo);
            } else {
                scriptVo.setFcu(UserContext.get().getUserUuid());
                scriptVo.setId(null);
                id = scriptVo.getId();
                autoexecScriptMapper.insertScript(scriptVo);
            }

            AutoexecScriptArgumentVo argument = scriptVo.getVersionArgument();
            try {
                if (CollectionUtils.isNotEmpty(scriptVo.getParamList())) {
                    autoexecService.validateParamList(scriptVo.getParamList());
                }
                if (argument != null) {
                    autoexecService.validateArgument(argument);
                }
            } catch (ApiRuntimeException ex) {
                failReasonList.add(ex.getMessage());
            }
            if (StringUtils.isBlank(scriptVo.getParser())) {
                failReasonList.add("脚本解析器为空");
            } else if (ScriptParser.getScriptParser(scriptVo.getParser()) == null) {
                failReasonList.add("不存在的脚本解析器[" + scriptVo.getParser() + "]");
            }
            if (StringUtils.equals(scriptVo.getParser(), ScriptParser.PACKAGE.getValue())) {
                FileVo fileVo = scriptVo.getPackageFile();
                if (fileVo == null) {
                    failReasonList.add("脚本依赖包缺失");
                } else {
                    fileVo.setId(null);
                    if (StringUtils.isNotEmpty(fileVo.getName()) && !fileVo.getName().endsWith(".tar")) {
                        failReasonList.add("脚本依赖包必须是tar文件");
                    }
                    result.put("newPackageFileName", fileVo.getName());
                    result.put("newPackageFileId", fileVo.getId());
                }
            }
            AutoexecScriptVersionVo versionVo = new AutoexecScriptVersionVo(id, ScriptVersionStatus.SUBMITTED.getValue());
            versionVo.setIsActive(0);
            versionVo.setParser(scriptVo.getParser());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd");
            versionVo.setTitle(scriptVo.getName() + "_" + LocalDate.now().format(formatter));
            autoexecScriptService.saveParamList(versionVo.getId(), scriptVo.getVersionParamList());
            if (argument != null) {
                argument.setScriptVersionId(versionVo.getId());
                autoexecScriptMapper.insertScriptVersionArgument(argument);
            }
            if (CollectionUtils.isNotEmpty(scriptVo.getLineList())) {
                autoexecScriptService.saveLineList(scriptVo.getId(), versionVo.getId(), scriptVo.getLineList());
            }
            if (StringUtils.equals(scriptVo.getParser(), ScriptParser.PACKAGE.getValue()) && scriptVo.getPackageFile() != null) {
                versionVo.setPackageFileId(scriptVo.getPackageFile().getId());
            }

            autoexecScriptMapper.insertScriptVersion(versionVo);
            result.put("newVersionId", versionVo.getId());

//            if (CollectionUtils.isNotEmpty(versionList)) {
//                for (AutoexecScriptVersionVo versionVo : versionList) {
//                    String versionStr = "版本-" + versionVo.getVersion() + "：";
//                    AutoexecScriptArgumentVo argument = versionVo.getArgument();
//                    try {
//                        if (CollectionUtils.isNotEmpty(versionVo.getParamList())) {
//                            autoexecService.validateParamList(versionVo.getParamList());
//                        }
//                        if (argument != null) {
//                            autoexecService.validateArgument(argument);
//                        }
//                    } catch (ApiRuntimeException ex) {
//                        failReasonList.add(versionStr + ex.getMessage());
//                        continue;
//                    }
//                    if (StringUtils.isBlank(versionVo.getParser())) {
//                        failReasonList.add(versionStr + "脚本解析器为空");
//                        continue;
//                    } else if (ScriptParser.getScriptParser(versionVo.getParser()) == null) {
//                        failReasonList.add(versionStr + "不存在的脚本解析器[" + versionVo.getParser() + "]");
//                        continue;
//                    }
//                    AutoexecScriptVersionVo oldVersion = autoexecScriptMapper.getVersionByVersionIdForUpdate(versionVo.getId());
//                    if (oldVersion != null) {
//                        oldVersion.setParamList(autoexecScriptMapper.getParamListByVersionId(versionVo.getId()));
//                        oldVersion.setArgument(autoexecScriptMapper.getArgumentByVersionId(versionVo.getId()));
//                        oldVersion.setLineList(autoexecScriptMapper.getLineListByVersionId(versionVo.getId()));
//                        autoexecScriptMapper.deleteScriptVersionLibByScriptVersionId(versionVo.getId());
//                        if (autoexecScriptService.checkScriptVersionNeedToUpdate(oldVersion, versionVo)) {
//                            autoexecScriptMapper.deleteParamByVersionId(versionVo.getId());
//                            autoexecScriptMapper.deleteArgumentByVersionId(versionVo.getId());
//                            autoexecScriptMapper.deleteScriptLineByVersionId(versionVo.getId());
//                            autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
//                            if (argument != null) {
//                                argument.setScriptVersionId(versionVo.getId());
//                                autoexecScriptMapper.insertScriptVersionArgument(argument);
//                            }
//                            autoexecScriptService.saveLineList(id, versionVo.getId(), versionVo.getLineList());
//                            versionVo.setLcu(UserContext.get().getUserUuid());
//                            autoexecScriptMapper.updateScriptVersion(versionVo);
//                        }
//                    } else {
//                        versionVo.setScriptId(id);
//                        versionVo.setId(null);
//                        autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
//                        if (argument != null) {
//                            argument.setScriptVersionId(versionVo.getId());
//                            autoexecScriptMapper.insertScriptVersionArgument(argument);
//                        }
//                        autoexecScriptService.saveLineList(id, versionVo.getId(), versionVo.getLineList());
//                        autoexecScriptMapper.insertScriptVersion(versionVo);
//                    }
//                }
//            }
        }
        if (CollectionUtils.isNotEmpty(failReasonList)) {
            result.put("item", "导入：" + name + "时出现如下问题：");
            result.put("failReasonList", failReasonList);
        }
        return result;
    }


}
