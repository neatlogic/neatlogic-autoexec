package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamConfigVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptArgumentVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.exception.file.FileNotUploadException;
import neatlogic.framework.exception.user.NoTenantException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import neatlogic.framework.fulltextindex.core.IFullTextIndexHandler;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.autoexec.fulltextindex.AutoexecFullTextIndexType;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import neatlogic.module.autoexec.service.AutoexecService;
import neatlogic.module.framework.file.handler.LocalFileSystemHandler;
import neatlogic.module.framework.file.handler.MinioFileSystemHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("deprecation")
@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptImportPublicApi extends PrivateBinaryStreamApiComponentBase {

    Logger logger = LoggerFactory.getLogger(AutoexecScriptImportPublicApi.class);

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecService autoexecService;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Autowired
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "autoexec/script/import/fromjson";
    }

    @Override
    public String getName() {
        return "导入脚本(通过固定格式json文件)";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "导入脚本(通过固定格式json文件)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {

        // 根据名称判断脚本存不存在，如果存在且内容有变化就生成新的激活版本，不存在直接生成新的激活版本
        // todo 用户令牌可用之后，要根据导入用户决定是否自动审核通过
        JSONObject result = new JSONObject();
        JSONArray faultArray = new JSONArray();
        Set<String> newScriptArray = new HashSet<>(); // 新增的脚本
        Set<String> updatedScriptArray = new HashSet<>(); // 更新了基本信息或生成了新版本的脚本
        result.put("faultArray", faultArray);
        result.put("newScriptArray", newScriptArray);
        result.put("updatedScriptArray", updatedScriptArray);

        String tenantUuid = TenantContext.get().getTenantUuid();
        if (StringUtils.isBlank(tenantUuid)) {
            throw new NoTenantException();
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        Map<String, MultipartFile> scriptFileNameMap = new HashMap<>();
        List<AutoexecScriptVo> importScriptList = new ArrayList<>();
        for (Map.Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            String name = multipartFile.getName();
            if (name.equals("scriptInfo.json")) {
                String s = MultipartFileToString(multipartFile);
                if (StringUtils.isNotEmpty(s)) {
                    JSONArray jsonArray = JSONArray.parseArray(s);
                    importScriptList.addAll(jsonArray.toJavaList(AutoexecScriptVo.class));
                }
            } else if (name.endsWith(".tar")) {
                scriptFileNameMap.put(multipartFile.getName(), multipartFile);
            }
        }

        if (CollectionUtils.isEmpty(importScriptList)) {
            return null;
        }
        int i = 1;
        for (AutoexecScriptVo newScriptVo : importScriptList) {
            List<String> faultMessages = new ArrayList<>();
            String catalogName = newScriptVo.getCatalogName();
            Long catalogId = null;
            if (StringUtils.isBlank(newScriptVo.getName())) {
                faultMessages.add("自定义工具名称为空");
            }
            if (StringUtils.isBlank(catalogName)) {
                faultMessages.add("工具目录为空");
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isBlank(newScriptVo.getRiskName())) {
                faultMessages.add("操作级别为空");
            }
            if (StringUtils.isBlank(newScriptVo.getTypeName())) {
                faultMessages.add("工具分类为空");
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isBlank(newScriptVo.getExecMode())) {
                faultMessages.add("执行方式为空");
            }
            if (StringUtils.isBlank(newScriptVo.getParser())) {
                faultMessages.add("脚本解析器为空");
            }
            if (StringUtils.equals(newScriptVo.getParser(), ScriptParser.PACKAGE.getValue())) {
                if (newScriptVo.getPackageFileName() == null) {
                    faultMessages.add("脚本依赖包名字为空");
                }
            } else {
                if (CollectionUtils.isEmpty(newScriptVo.getLineList())) {
                    faultMessages.add("脚本内容为空");
                }
            }
            if (StringUtils.isNotBlank(newScriptVo.getTypeName()) && autoexecTypeMapper.getTypeIdByName(newScriptVo.getTypeName()) == null) {
                faultMessages.add("工具分类：'" + newScriptVo.getTypeName() + "'不存在");
            }
            // 从外部导入的自定义工具，catalogName可能是路径，也可能只是名称，如果是路径，要根据每一层的名称查询对应的目录
            if (StringUtils.isNotBlank(catalogName)) {
                catalogId = autoexecScriptService.createCatalogByCatalogPath(catalogName);
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isNotBlank(newScriptVo.getRiskName()) && autoexecRiskMapper.getRiskIdByName(newScriptVo.getRiskName()) == null) {
                faultMessages.add("操作级别：'" + newScriptVo.getRiskName() + "'不存在");
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isNotBlank(newScriptVo.getExecMode()) && ScriptExecMode.getExecMode(newScriptVo.getExecMode()) == null) {
                faultMessages.add("执行方式：'" + newScriptVo.getExecMode() + "'不存在");
            }
            if (StringUtils.isNotBlank(newScriptVo.getParser()) && ScriptParser.getScriptParser(newScriptVo.getParser()) == null) {
                faultMessages.add("脚本解析器：'" + newScriptVo.getParser() + "'不存在");
            }
            if (newScriptVo.getArgument() != null) {
                try {
                    autoexecService.validateArgument(newScriptVo.getArgument());
                } catch (Exception ex) {
                    faultMessages.add(ex.getMessage());
                }
            }
            if (CollectionUtils.isNotEmpty(newScriptVo.getParamList())) {
                try {
                    autoexecService.validateParamList(newScriptVo.getParamList());
                } catch (Exception ex) {
                    faultMessages.add(ex.getMessage());
                }
            }
            if (CollectionUtils.isNotEmpty(newScriptVo.getUseLibName())) {
                Map<String, String> scriptNameMap = new HashMap<>();
                newScriptVo.getUseLibName().forEach(e -> scriptNameMap.put(e.substring(e.lastIndexOf("/") + 1), e));
                List<AutoexecScriptVo> scriptList = autoexecScriptMapper.getScriptListByNameList(new ArrayList<>(scriptNameMap.keySet()));
                if (CollectionUtils.isNotEmpty(scriptList)) {
                    List<String> notExistNameList = new ArrayList<>();
                    List<String> existScriptName = scriptList.stream().map(AutoexecScriptVo::getName).collect(toList());
                    for (Map.Entry<String, String> entry : scriptNameMap.entrySet()) {
                        if (!existScriptName.contains(entry.getKey())) {
                            notExistNameList.add(entry.getValue());
                        }
                    }
                    if (CollectionUtils.isNotEmpty(notExistNameList)) {
                        faultMessages.add("以下依赖工具不存在：" + notExistNameList);
                    }
                } else {
                    faultMessages.add("以下依赖工具不存在：" + newScriptVo.getUseLibName());
                }
            }
            if (MapUtils.isNotEmpty(scriptFileNameMap) && StringUtils.equals(newScriptVo.getParser(), ScriptParser.PACKAGE.getValue()) && newScriptVo.getPackageFileName() != null) {
                FileVo packageFile = new FileVo();
                //检验脚本信息
                if (scriptFileNameMap.containsKey(newScriptVo.getPackageFileName() )) {
                    MultipartFile multipartFile = scriptFileNameMap.get(newScriptVo.getPackageFileName() );
                    FileVo fileVo = new FileVo();
                    fileVo.setSize(multipartFile.getSize());
                    fileVo.setName (multipartFile.getName());
                    fileVo.setUserUuid(SystemUser.SYSTEM.getUserUuid());
                    fileVo.setType("autoexec");
                    fileVo.setContentType("application/x-tar");
                    newScriptVo.setPackageFileId(fileVo.getId());
                    String filePath;
                    try {
                        filePath = FileUtil.saveData(MinioFileSystemHandler.NAME, tenantUuid, multipartFile.getInputStream(), fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType());
                    } catch (Exception ex) {
                        //如果没有配置minioUrl，则表示不使用minio，无需抛异常
                        if (StringUtils.isNotBlank(Config.MINIO_URL())) {
                            logger.error(ex.getMessage(), ex);
                        }
                        // 如果minio出现异常，则上传到本地
                        filePath = FileUtil.saveData(LocalFileSystemHandler.NAME, tenantUuid, multipartFile.getInputStream(), fileVo.getId().toString(), fileVo.getContentType(), fileVo.getType());
                    }
                    fileVo.setPath(filePath);
                    fileMapper.insertFile(fileVo);
                } else {
                    //错误信息
                    faultMessages.add("以下依赖脚本包（tar）未上传：" + packageFile.getName());
                }
            }
            if (CollectionUtils.isEmpty(faultMessages)) {
                newScriptVo.setTypeId(autoexecTypeMapper.getTypeIdByName(newScriptVo.getTypeName()));
                newScriptVo.setRiskId(autoexecRiskMapper.getRiskIdByName(newScriptVo.getRiskName()));
                newScriptVo.setCatalogId(catalogId);

                AutoexecScriptVo oldScriptVo = autoexecScriptMapper.getScriptBaseInfoByName(newScriptVo.getName());
                Long scriptId = oldScriptVo != null ? oldScriptVo.getId() : newScriptVo.getId();
                newScriptVo.setDefaultProfileId(autoexecService.saveProfileOperation(newScriptVo.getDefaultProfileName(), scriptId, ToolType.SCRIPT.getValue()));
                if (oldScriptVo == null) {
                    newScriptArray.add(newScriptVo.getName());
                    newScriptVo.setFcu(UserContext.get().getUserUuid());
                    AutoexecScriptVersionVo versionVo = getVersionVo(newScriptVo, 1);
                    autoexecScriptMapper.insertScript(newScriptVo);
                    autoexecScriptMapper.insertScriptVersion(versionVo);
                    if (versionVo.getArgument() != null) {
                        autoexecScriptMapper.insertScriptVersionArgument(versionVo.getArgument());
                    }
                    autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
                    autoexecScriptService.saveLineList(newScriptVo.getId(), versionVo.getId(), versionVo.getLineList());
                    if (CollectionUtils.isNotEmpty(versionVo.getUseLibName())) {
                        List<String> scriptNameList = new ArrayList<>();
                        versionVo.getUseLibName().forEach(e -> scriptNameList.add(e.substring(e.lastIndexOf("/") + 1)));
                        if (CollectionUtils.isNotEmpty(scriptNameList)) {
                            List<Long> scriptIdList = autoexecScriptMapper.getScriptIdListByNameList(scriptNameList);
                            if (CollectionUtils.isNotEmpty(scriptIdList)) {
                                autoexecScriptMapper.insertScriptVersionUseLib(versionVo.getId(), scriptIdList);
                            }
                        }
                    }
                    IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                    if (fullTextIndexHandler != null) {
                        fullTextIndexHandler.createIndex(versionVo.getId());
                    }
                } else {
                    newScriptVo.setId(oldScriptVo.getId());
                    if (checkBaseInfoHasBeenChanged(newScriptVo, oldScriptVo)) {
                        autoexecScriptMapper.updateScriptBaseInfo(newScriptVo);
                        updatedScriptArray.add(newScriptVo.getName());
                    }
                    Integer maxVersion = autoexecScriptMapper.getMaxVersionByScriptId(oldScriptVo.getId());
                    AutoexecScriptVersionVo newVersionVo = getVersionVo(newScriptVo, maxVersion != null ? maxVersion + 1 : 1);
                    AutoexecScriptVersionVo oldVersionVo = autoexecScriptMapper.getActiveVersionWithUseLibsByScriptId(oldScriptVo.getId());
                    boolean needUpdate = true;
                    if (oldVersionVo != null) {
                        oldVersionVo.setArgument(autoexecScriptMapper.getArgumentByVersionId(oldVersionVo.getId()));
                        List<AutoexecScriptVersionParamVo> oldParamList = autoexecScriptMapper.getParamListByVersionId(oldVersionVo.getId());
                        oldVersionVo.setParamList(oldParamList);
                        oldVersionVo.setLineList(autoexecScriptMapper.getLineListByVersionId(oldVersionVo.getId()));
                        adjustParamConfig(oldParamList);
                        if (!autoexecScriptService.checkScriptVersionNeedToUpdate(oldVersionVo, newVersionVo)) {
                            needUpdate = false;
                        } else {
                            oldVersionVo.setIsActive(0);
                            oldVersionVo.setLcu(UserContext.get().getUserUuid());
                            autoexecScriptMapper.updateScriptVersion(oldVersionVo);
                        }
                    }
                    if (needUpdate) {
                        updatedScriptArray.add(newScriptVo.getName());
                        if (newVersionVo.getArgument() != null) {
                            autoexecScriptMapper.insertScriptVersionArgument(newVersionVo.getArgument());
                        }
                        autoexecScriptService.saveParamList(newVersionVo.getId(), newVersionVo.getParamList());
                        autoexecScriptService.saveLineList(newScriptVo.getId(), newVersionVo.getId(), newVersionVo.getLineList());
                        autoexecScriptMapper.insertScriptVersion(newVersionVo);
                        if (CollectionUtils.isNotEmpty(newScriptVo.getUseLibName())) {
                            List<String> scriptNameList = new ArrayList<>();
                            newScriptVo.getUseLibName().forEach(e -> scriptNameList.add(e.substring(e.lastIndexOf("/") + 1)));
                            if (CollectionUtils.isNotEmpty(scriptNameList)) {
                                List<Long> scriptIdList = autoexecScriptMapper.getScriptIdListByNameList(scriptNameList);
                                if (CollectionUtils.isNotEmpty(scriptIdList)) {
                                    autoexecScriptMapper.insertScriptVersionUseLib(newVersionVo.getId(), scriptIdList);
                                }
                            }
                        }
                        IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                        if (fullTextIndexHandler != null) {
                            fullTextIndexHandler.createIndex(newVersionVo.getId());
                        }
                    }
                }
            } else {
                JSONObject faultObj = new JSONObject();
                String item;
                if (StringUtils.isNotBlank(newScriptVo.getName())) {
                    item = "导入" + newScriptVo.getName() + "失败";
                } else {
                    item = "导入第[" + i + "]个失败";
                }
                faultObj.put("item", item);
                faultObj.put("faultMessages", faultMessages);
                faultArray.add(faultObj);
            }
            i++;
        }
        return result;
    }

    /**
     * 从autoexecscripts导入而来的脚本，参数中的config与系统的config结构不完全一致，
     * 可能导致对比时误判，故校正原有的config，使其与导入的config保持一致
     *
     * @param oldParamList
     */
    private void adjustParamConfig(List<AutoexecScriptVersionParamVo> oldParamList) {
        if (oldParamList.size() > 0) {
            for (AutoexecScriptVersionParamVo paramVo : oldParamList) {
                AutoexecParamConfigVo config = paramVo.getConfig();
                if (config != null) {
                    if (needDataSourceTypeList.contains(paramVo.getType()) && ParamMode.INPUT.getValue().equals(paramVo.getMode())) {
                        config.setType(null);
                        config.setIsRequired(null);
                        config.setDefaultValue(null);
                    } else {
                        paramVo.setConfig(null);
                    }
                }
            }
        }
    }

    /**
     * 检查脚本基本信息是否变更
     *
     * @param newScriptVo 导入的脚本
     * @param oldScriptVo 系统中的脚本
     * @return
     */
    private boolean checkBaseInfoHasBeenChanged(AutoexecScriptVo newScriptVo, AutoexecScriptVo oldScriptVo) {
        if (!Objects.equals(newScriptVo.getCatalogId(), oldScriptVo.getCatalogId())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getTypeName(), oldScriptVo.getTypeName())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getExecMode(), oldScriptVo.getExecMode())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getIsLib(), oldScriptVo.getIsLib())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getRiskName(), oldScriptVo.getRiskName())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getDescription(), oldScriptVo.getDescription())) {
            return true;
        }
        return !Objects.equals(newScriptVo.getDefaultProfileName(), oldScriptVo.getDefaultProfileName());
    }

    /**
     * 构造AutoexecScriptVersionVo
     *
     * @param scriptVo
     * @param version
     * @return
     */
    private AutoexecScriptVersionVo getVersionVo(AutoexecScriptVo scriptVo, Integer version) {
        AutoexecScriptVersionVo versionVo = new AutoexecScriptVersionVo();
        versionVo.setScriptId(scriptVo.getId());
        versionVo.setTitle(scriptVo.getName());
        versionVo.setParser(scriptVo.getParser());
        versionVo.setUseLibName(CollectionUtils.isNotEmpty(scriptVo.getUseLibName()) ? scriptVo.getUseLibName() : new ArrayList<>());
        versionVo.setIsActive(1);
        versionVo.setLcu(UserContext.get().getUserUuid());
        versionVo.setStatus(ScriptVersionStatus.PASSED.getValue());
        versionVo.setVersion(version);
        versionVo.setPackageFileId(scriptVo.getPackageFileId());
        versionVo.setReviewer(UserContext.get().getUserUuid());
        AutoexecScriptArgumentVo argument = scriptVo.getVersionArgument();
        if (argument != null) {
            argument.setScriptVersionId(versionVo.getId());
            versionVo.setArgument(argument);
        }
        versionVo.setParamList(scriptVo.getVersionParamList());
        versionVo.setLineList(scriptVo.getLineList());
        return versionVo;
    }

    static List<String> needDataSourceTypeList = new ArrayList<>();

    static {
        needDataSourceTypeList.add(ParamType.SELECT.getValue());
        needDataSourceTypeList.add(ParamType.MULTISELECT.getValue());
        needDataSourceTypeList.add(ParamType.RADIO.getValue());
        needDataSourceTypeList.add(ParamType.CHECKBOX.getValue());
    }

    private String MultipartFileToString(MultipartFile multipartFile) {
        InputStreamReader isr;
        BufferedReader br;
        StringBuilder txtResult = new StringBuilder();
        try {
            isr = new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
            String lineTxt;
            while ((lineTxt = br.readLine()) != null) {
                txtResult.append(lineTxt);
            }
            isr.close();
            br.close();
            return txtResult.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
