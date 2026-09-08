package neatlogic.module.autoexec.api.script;

import neatlogic.framework.autoexec.exception.AutoexecScriptNameAmbiguousException;
import neatlogic.framework.util.$;

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
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
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
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.autoexec.fulltextindex.AutoexecFullTextIndexType;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import neatlogic.module.autoexec.service.AutoexecService;
import neatlogic.module.autoexec.service.AutoexecOperationChangeDispatcher;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Resource
    private FileMapper fileMapper;

    @Resource
    private AutoexecOperationChangeDispatcher operationChangeDispatcher;

    @Override
    public String getToken() {
        return "autoexec/script/import/forautoexec";
    }

    @Override
    public String getName() {
        return "nmaa.autoexecscriptimportpublicapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "nmaa.autoexecscriptimportpublicapi.getname")
    /** Import portable script and library identities without discarding their full catalog paths. */
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
            String catalogName = newScriptVo.getFullCatalogName();
            if (catalogName == null) catalogName = newScriptVo.getCatalogPath();
            if (catalogName == null) catalogName = newScriptVo.getCatalogName();
            if (catalogName != null && catalogName.isEmpty()) catalogName = "/";
            Long catalogId = null;
            if (StringUtils.isBlank(newScriptVo.getName())) {
                faultMessages.add($.t("nmar.import.scriptnameempty"));
            }
            if (StringUtils.isBlank(catalogName)) {
                faultMessages.add($.t("nmar.import.catalogempty"));
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isBlank(newScriptVo.getRiskName())) {
                faultMessages.add($.t("nmar.import.riskempty"));
            }
            if (StringUtils.isBlank(newScriptVo.getTypeName())) {
                faultMessages.add($.t("nmar.import.typeempty"));
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isBlank(newScriptVo.getExecMode())) {
                faultMessages.add($.t("nmar.import.execmodeempty"));
            }
            if (StringUtils.isBlank(newScriptVo.getParser())) {
                faultMessages.add($.t("nmar.import.parserempty"));
            }
            if (StringUtils.equals(newScriptVo.getParser(), ScriptParser.PACKAGE.getValue())) {
                if (newScriptVo.getPackageFileName() == null) {
                    faultMessages.add($.t("nmar.import.packagenameempty"));
                }
            } else {
                if (CollectionUtils.isEmpty(newScriptVo.getLineList())) {
                    faultMessages.add($.t("nmar.import.scriptcontentempty"));
                }
            }
            if (StringUtils.isNotBlank(newScriptVo.getTypeName()) && autoexecTypeMapper.getTypeIdByName(newScriptVo.getTypeName()) == null) {
                faultMessages.add($.t("nmar.import.typenotfoundprefix") + newScriptVo.getTypeName() + $.t("nmar.import.notfoundsuffix"));
            }
            // 从外部导入的自定义工具，catalogName可能是路径，也可能只是名称，如果是路径，要根据每一层的名称查询对应的目录
            if (StringUtils.isNotBlank(catalogName)) {
                catalogId = "/".equals(catalogName) ? 0L : autoexecScriptService.createCatalogByCatalogPath(catalogName);
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isNotBlank(newScriptVo.getRiskName()) && autoexecRiskMapper.getRiskIdByName(newScriptVo.getRiskName()) == null) {
                faultMessages.add($.t("nmar.import.risknotfoundprefix") + newScriptVo.getRiskName() + $.t("nmar.import.notfoundsuffix"));
            }
            if (newScriptVo.getIsLib() == 0 && StringUtils.isNotBlank(newScriptVo.getExecMode()) && ScriptExecMode.getExecMode(newScriptVo.getExecMode()) == null) {
                faultMessages.add($.t("nmar.import.execmodenotfoundprefix") + newScriptVo.getExecMode() + $.t("nmar.import.notfoundsuffix"));
            }
            if (StringUtils.isNotBlank(newScriptVo.getParser()) && ScriptParser.getScriptParser(newScriptVo.getParser()) == null) {
                faultMessages.add($.t("nmar.import.parsernotfoundprefix") + newScriptVo.getParser() + $.t("nmar.import.notfoundsuffix"));
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
            List<Long> resolvedLibIds = new ArrayList<>();
            AutoexecScriptVo oldScriptVo = null;
            try {
                oldScriptVo = autoexecScriptService.resolveScriptByName(newScriptVo.getName(), catalogName);
                if (CollectionUtils.isNotEmpty(newScriptVo.getUseLibName())) {
                    Map<String, AutoexecScriptVo> libraries = autoexecScriptService.resolveScriptByPathList(newScriptVo.getUseLibName());
                    for (Map.Entry<String, AutoexecScriptVo> library : libraries.entrySet()) {
                        if (library.getValue() == null || !Objects.equals(library.getValue().getIsLib(), 1)) {
                            faultMessages.add($.t("nmar.import.missingtools") + library.getKey());
                        } else {
                            resolvedLibIds.add(library.getValue().getId());
                        }
                    }
                }
            } catch (AutoexecScriptNameAmbiguousException e) {
                logger.error("Failed to resolve imported script or library", e);
                faultMessages.add(e.getMessage());
            }
            if (faultMessages.isEmpty() && MapUtils.isNotEmpty(scriptFileNameMap) && StringUtils.equals(newScriptVo.getParser(), ScriptParser.PACKAGE.getValue()) && newScriptVo.getPackageFileName() != null) {
                FileVo packageFile = new FileVo();
                //检验脚本信息
                if (scriptFileNameMap.containsKey(newScriptVo.getPackageFileName())) {
                    MultipartFile multipartFile = scriptFileNameMap.get(newScriptVo.getPackageFileName());
                    FileVo fileVo = new FileVo();
                    fileVo.setSize(multipartFile.getSize());
                    fileVo.setName(multipartFile.getName());
                    fileVo.setUserUuid(SystemUser.SYSTEM.getUserUuid());
                    fileVo.setType("autoexec");
                    fileVo.setContentType("application/x-tar");
                    newScriptVo.setPackageFileId(fileVo.getId());
                    String filePath = FileUtil.saveData(tenantUuid, multipartFile.getInputStream(), fileVo);
                    fileVo.setPath(filePath);
                    fileMapper.insertFile(fileVo);
                } else {
                    //错误信息
                    faultMessages.add($.t("nmar.import.missingpackages") + packageFile.getName());
                }
            }
            if (CollectionUtils.isEmpty(faultMessages)) {
                newScriptVo.setTypeId(autoexecTypeMapper.getTypeIdByName(newScriptVo.getTypeName()));
                newScriptVo.setRiskId(autoexecRiskMapper.getRiskIdByName(newScriptVo.getRiskName()));
                newScriptVo.setCatalogId(catalogId);

                Long scriptId = oldScriptVo != null ? oldScriptVo.getId() : newScriptVo.getId();
                newScriptVo.setDefaultProfileId(autoexecService.saveProfileOperation(newScriptVo.getDefaultProfileName(), scriptId, ToolType.SCRIPT.getValue()));
                if (oldScriptVo == null) {
                    newScriptArray.add(newScriptVo.getName());
                    newScriptVo.setFcu(UserContext.get().getUserUuid());
                    AutoexecScriptVersionVo versionVo = getVersionVo(newScriptVo, 1);
                    autoexecScriptService.persistScriptBaseInfo(newScriptVo, true);
                    autoexecScriptMapper.insertScriptVersion(versionVo);
                    if (versionVo.getArgument() != null) {
                        autoexecScriptMapper.insertScriptVersionArgument(versionVo.getArgument());
                    }
                    autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
                    autoexecScriptService.saveLineList(newScriptVo.getId(), versionVo.getId(), versionVo.getLineList());
                    if (CollectionUtils.isNotEmpty(resolvedLibIds)) {
                        autoexecScriptMapper.insertScriptVersionUseLib(versionVo.getId(), resolvedLibIds);
                    }
                    IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                    if (fullTextIndexHandler != null) {
                        fullTextIndexHandler.createIndex(versionVo.getId());
                    }
                } else {
                    newScriptVo.setId(oldScriptVo.getId());
                    if (checkBaseInfoHasBeenChanged(newScriptVo, oldScriptVo)) {
                        autoexecScriptService.persistScriptBaseInfo(newScriptVo, false);
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
                        if (CollectionUtils.isNotEmpty(resolvedLibIds)) {
                            autoexecScriptMapper.insertScriptVersionUseLib(newVersionVo.getId(), resolvedLibIds);
                        }
                        IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                        if (fullTextIndexHandler != null) {
                            fullTextIndexHandler.createIndex(newVersionVo.getId());
                        }
                    }
                }
                operationChangeDispatcher.notifyAfterCommit("script", scriptId,
                        AutoexecOperationIndexAction.UPSERT);
            } else {
                JSONObject faultObj = new JSONObject();
                String item;
                if (StringUtils.isNotBlank(newScriptVo.getName())) {
                    item = $.t("nmar.import.import") + newScriptVo.getName() + $.t("nmar.import.failed");
                } else {
                    item = $.t("nmar.import.itemprefix") + i + $.t("nmar.import.itemsuffix");
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
     */
    private void adjustParamConfig(List<AutoexecScriptVersionParamVo> oldParamList) {
        if (!oldParamList.isEmpty()) {
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
        StringBuilder txtResult = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8); BufferedReader br = new BufferedReader(isr)) {
            String lineTxt;
            while ((lineTxt = br.readLine()) != null) {
                txtResult.append(lineTxt);
            }
            return txtResult.toString();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return "";
        }
    }
}
