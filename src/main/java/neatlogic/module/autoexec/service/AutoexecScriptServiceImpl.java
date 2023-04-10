/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.autoexec.service;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.dto.script.*;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.autoexec.exception.customtemplate.CustomTemplateNotFoundException;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.deploy.exception.DeployJobParamIrregularException;
import neatlogic.framework.dto.OperateVo;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoexecScriptServiceImpl implements AutoexecScriptService {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;

    @Resource
    private AutoexecService autoexecService;

    @Resource
    private AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    private FileMapper fileMapper;


    /**
     * 获取脚本版本详细信息，包括参数与脚本内容
     *
     * @param versionId 版本ID
     * @return 脚本版本VO
     */
    @Override
    public AutoexecScriptVersionVo getScriptVersionDetailByVersionId(Long versionId) {
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        version.setParamList(autoexecScriptMapper.getParamListByVersionId(versionId));
        version.setArgument(autoexecScriptMapper.getArgumentByVersionId(versionId));
        if (!StringUtils.equals(version.getParser(), ScriptParser.PACKAGE.getValue())) {
            version.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
        } else if (version.getPackageFileId() != null) {
            version.setPackageFile(fileMapper.getFileById(version.getPackageFileId()));
        }
        version.setUseLibName(autoexecScriptMapper.getVersionUseLibNameByVersionId(versionId));
        return version;
    }

    @Override
    public List<AutoexecScriptVersionVo> getScriptVersionDetailListByScriptId(AutoexecScriptVersionVo vo) {
        List<AutoexecScriptVersionVo> versionList = autoexecScriptMapper.getVersionListIncludeLineByScriptId(vo);
        if (CollectionUtils.isNotEmpty(versionList)) {

            List<Long> versionIdList = versionList.stream().map(AutoexecScriptVersionVo::getId).collect(Collectors.toList());
            Map<Long, AutoexecScriptArgumentVo> argumentMap = new HashMap<>();
            Map<Long, List<AutoexecScriptVersionParamVo>> paramMap = new HashMap<>();
            Map<Long, List<Long>> useLibMap = new HashMap<>();
            Map<Long, List<String>> useLibNameMap = new HashMap<>();
            List<AutoexecScriptVersionVo> versionVoIncludeArgumentList = autoexecScriptMapper.getVersionIncludeArgumentByVersionIdList(versionIdList);
            List<AutoexecScriptVersionVo> versionVoIncludeParamList = autoexecScriptMapper.getVersionIncludeParamByVersionIdList(versionIdList);
            List<AutoexecScriptVersionVo> versionVoIncludeUseLibNameList = autoexecScriptMapper.getVersionIncludeUseLibAndNameByVersionIdList(versionIdList);

            if (CollectionUtils.isNotEmpty(versionVoIncludeArgumentList)) {
                argumentMap = versionVoIncludeArgumentList.stream().filter(e -> e.getArgument() != null).collect(Collectors.toMap(AutoexecScriptVersionVo::getId, AutoexecScriptVersionVo::getArgument));
            }
            if (CollectionUtils.isNotEmpty(versionVoIncludeParamList)) {
                paramMap = versionVoIncludeParamList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getId, AutoexecScriptVersionVo::getParamList));
            }
            if (CollectionUtils.isNotEmpty(versionVoIncludeUseLibNameList)) {
                useLibMap = versionVoIncludeUseLibNameList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getId, AutoexecScriptVersionVo::getUseLib));
            }
            if (CollectionUtils.isNotEmpty(versionVoIncludeUseLibNameList)) {
                useLibNameMap = versionVoIncludeUseLibNameList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getId, AutoexecScriptVersionVo::getUseLibName));
            }

            if (CollectionUtils.isNotEmpty(versionList)) {
                for (AutoexecScriptVersionVo version : versionList) {
                    version.setParamList(paramMap.get(version.getId()));
                    version.setArgument(argumentMap.get(version.getId()));
                    version.setUseLib(useLibMap.get(version.getId()));
                    version.setUseLibName(useLibNameMap.get(version.getId()));
                }
            }
        }
        return versionList;
    }

    /**
     * 1、当不是库文件时需要校验执行方式（execMode）、操作级别（riskId）的必填
     * 2、校验脚本的基本信息，包括name、uk、分类、操作级别
     *
     * @param scriptVo 脚本VO
     */
    @Override
    public void validateScriptBaseInfo(AutoexecScriptVo scriptVo) {

        //入参校验：当不是库文件时需要校验执行方式（execMode）、操作级别（riskId）的必填
        if (!Objects.equals(scriptVo.getIsLib(), 1)) {
            if (scriptVo.getExecMode() == null) {
                throw new DeployJobParamIrregularException("execMode");
            }
            if (scriptVo.getRiskId() == null) {
                throw new DeployJobParamIrregularException("riskId");
            }
        }

        if (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
            throw new AutoexecScriptNameOrUkRepeatException(scriptVo.getName());
        }
//        if (autoexecScriptMapper.checkScriptUkIsExists(scriptVo) > 0) {
//            throw new AutoexecScriptNameOrUkRepeatException(scriptVo.getName());
//        }
        if (autoexecTypeMapper.checkTypeIsExistsById(scriptVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(scriptVo.getTypeId());
        }
        if (!Objects.equals(scriptVo.getCatalogId(), AutoexecCatalogVo.ROOT_ID) && autoexecCatalogMapper.checkAutoexecCatalogIsExists(scriptVo.getCatalogId()) == 0) {
            throw new AutoexecCatalogNotFoundException(scriptVo.getCatalogId());
        }
        if (!Objects.equals(scriptVo.getIsLib(), 1) && autoexecRiskMapper.checkRiskIsExistsById(scriptVo.getRiskId()) == 0) {
            throw new AutoexecRiskNotFoundException(scriptVo.getRiskId());
        }
        if (scriptVo.getCustomTemplateId() != null && autoexecCustomTemplateMapper.checkCustomTemplateIsExistsById(scriptVo.getCustomTemplateId()) == 0) {
            throw new CustomTemplateNotFoundException(scriptVo.getCustomTemplateId());
        }
        Long defaultProfileId = scriptVo.getDefaultProfileId();
        if (defaultProfileId != null) {
            if (autoexecProfileMapper.checkProfileIsExists(defaultProfileId) == 0) {
                throw new AutoexecProfileIsNotFoundException(defaultProfileId);
            }
            if (autoexecProfileMapper.getProfileIdByProfileIdAndOperationId(defaultProfileId, scriptVo.getId()) == null) {
                throw new AutoexecProfileNotReferencedByOperationException(defaultProfileId, scriptVo.getId());
            }
        }
    }

    @Override
    public List<Long> getCatalogIdList(Long catalogId) {
        if (catalogId != null && !Objects.equals(catalogId, AutoexecCatalogVo.ROOT_ID)) {
            AutoexecCatalogVo catalogTmp = autoexecCatalogMapper.getAutoexecCatalogById(catalogId);
            if (catalogTmp != null) {
                return autoexecCatalogMapper.getChildrenByLftRht(catalogTmp).stream().map(AutoexecCatalogVo::getId).collect(Collectors.toList());
            }
        }
        return null;
    }

    /**
     * 检查脚本内容是否有变更
     *
     * @param before 当前版本
     * @param after  待更新的内容
     * @return 是否有变更
     */
    @Override
    public boolean checkScriptVersionNeedToUpdate(AutoexecScriptVersionVo before, AutoexecScriptVersionVo after) {
        if (!Objects.equals(before.getParser(), after.getParser())) {
            return true;
        }
        if (StringUtils.equals(before.getParser(), ScriptParser.PACKAGE.getValue())) {
            return true;
        }
        List<AutoexecScriptVersionParamVo> beforeParamList = before.getParamList() != null ? before.getParamList() : new ArrayList<>();
        List<AutoexecScriptVersionParamVo> afterParamList = after.getParamList() != null ? after.getParamList() : new ArrayList<>();
        if (beforeParamList.size() != afterParamList.size()) {
            return true;
        }
        List<AutoexecScriptVersionParamVo> beforeInputParamList = beforeParamList.stream().filter(o -> o.getMode().equals(ParamMode.INPUT.getValue())).collect(Collectors.toList());
        List<AutoexecScriptVersionParamVo> beforeOutputParamList = beforeParamList.stream().filter(o -> o.getMode().equals(ParamMode.OUTPUT.getValue())).collect(Collectors.toList());
        List<AutoexecScriptVersionParamVo> afterInputParamList = afterParamList.stream().filter(o -> o.getMode().equals(ParamMode.INPUT.getValue())).collect(Collectors.toList());
        List<AutoexecScriptVersionParamVo> afterOutputParamList = afterParamList.stream().filter(o -> o.getMode().equals(ParamMode.OUTPUT.getValue())).collect(Collectors.toList());
        if (beforeInputParamList.size() != afterInputParamList.size()) {
            return true;
        }
        if (beforeOutputParamList.size() != afterOutputParamList.size()) {
            return true;
        }
        if (compareParamList(beforeInputParamList, afterInputParamList)) {
            return true;
        }
        if (compareParamList(beforeOutputParamList, afterOutputParamList)) {
            return true;
        }
        if (!Objects.equals(before.getArgument(), after.getArgument())) {
            return true;
        }
        List<AutoexecScriptLineVo> beforeLineList = new ArrayList<>();
        beforeLineList.addAll(before.getLineList());
        List<AutoexecScriptLineVo> afterLineList = new ArrayList<>();
        afterLineList.addAll(after.getLineList());
        if (beforeLineList.size() != afterLineList.size()) {
            return true;
        }
        for (int i = 0; i < beforeLineList.size(); i++) {
            String beforeContent = beforeLineList.get(i).getContentHash();
            String afterContent = null;
            if (StringUtils.isNotBlank(afterLineList.get(i).getContent())) {
                afterContent = DigestUtils.md5DigestAsHex(afterLineList.get(i).getContent().getBytes());
            }
            if (!Objects.equals(beforeContent, afterContent)) {
                return true;
            }
        }
        if (before.getUseLib().size() != after.getUseLibName().size()) {
            return true;
        } else {
            List<String> oldUseLibName = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(before.getUseLib())) {
                List<AutoexecOperationVo> scriptList = autoexecScriptMapper.getScriptListByIdList(before.getUseLib());
                if (CollectionUtils.isNotEmpty(scriptList)) {
                    Set<Long> scriptCatalogIdSet = scriptList.stream().map(AutoexecOperationVo::getCatalogId).collect(Collectors.toSet());
                    if (CollectionUtils.isNotEmpty(scriptCatalogIdSet)) {
                        List<AutoexecCatalogVo> scriptCatalogSet = autoexecCatalogMapper.getAutoexecFullCatalogByIdList(new ArrayList<>(scriptCatalogIdSet));
                        if (CollectionUtils.isNotEmpty(scriptCatalogSet)) {
                            Map<Long, String> scriptCatalogMap = scriptCatalogSet.stream().collect(Collectors.toMap(AutoexecCatalogVo::getId, AutoexecCatalogVo::getFullCatalogName));
                            for (AutoexecOperationVo operationVo : scriptList) {
                                if (scriptCatalogMap.containsKey(operationVo.getCatalogId())) {
                                    oldUseLibName.add(scriptCatalogMap.get(operationVo.getCatalogId()) + "/" + operationVo.getName());
                                }
                            }
                        }
                    }
                }
            }
            for (String newName : after.getUseLibName()) {
                if (!oldUseLibName.contains(newName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 比较参数是否有变更
     *
     * @param beforeParamList 旧参数列表
     * @param afterParamList  新参数列表
     * @return
     */
    private boolean compareParamList(List<AutoexecScriptVersionParamVo> beforeParamList, List<AutoexecScriptVersionParamVo> afterParamList) {
        Iterator<AutoexecScriptVersionParamVo> beforeParamIterator = beforeParamList.iterator();
        Iterator<AutoexecScriptVersionParamVo> afterParamIterator = afterParamList.iterator();
        while (beforeParamIterator.hasNext() && afterParamIterator.hasNext()) {
            AutoexecScriptVersionParamVo beforeNextParam = beforeParamIterator.next();
            AutoexecScriptVersionParamVo afterNextParam = afterParamIterator.next();
            if (!Objects.equals(beforeNextParam, afterNextParam)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取版本操作列表
     *
     * @param version
     * @return
     */
    @Override
    public List<OperateVo> getOperateListForScriptVersion(AutoexecScriptVersionVo version) {
        List<OperateVo> operateList = null;
        if (version != null) {
            operateList = new ArrayList<>();
            Boolean hasSearchAuth = AuthActionChecker.check(AUTOEXEC_SCRIPT_SEARCH.class.getSimpleName());
            Boolean hasModifyAuth = AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName());
            Boolean hasManageAuth = AuthActionChecker.check(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName());
            if (Objects.equals(version.getStatus(), ScriptVersionStatus.DRAFT.getValue())) {
                if (hasModifyAuth) {
                    operateList.add(new OperateVo(ScriptAndToolOperate.SAVE.getValue(), ScriptAndToolOperate.SAVE.getText()));
                    OperateVo submit = new OperateVo(ScriptAndToolOperate.SUBMIT.getValue(), ScriptAndToolOperate.SUBMIT.getText());
                    if (autoexecScriptMapper.checkScriptHasSubmittedVersionByScriptId(version.getScriptId()) > 0) {
                        submit.setDisabled(1);
                        submit.setDisabledReason("当前自定义工具已经有其他待审核版本");
                    }
                    operateList.add(submit);
                    operateList.add(new OperateVo(ScriptAndToolOperate.VALIDATE.getValue(), ScriptAndToolOperate.VALIDATE.getText()));
                    operateList.add(new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText()));
                    operateList.add(new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText()));
                    operateList.add(new OperateVo(ScriptAndToolOperate.VERSION_DELETE.getValue(), ScriptAndToolOperate.VERSION_DELETE.getText()));
                }
            } else if (Objects.equals(version.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
                if (hasManageAuth) {
                    operateList.add(new OperateVo(ScriptAndToolOperate.PASS.getValue(), ScriptAndToolOperate.PASS.getText()));
                    operateList.add(new OperateVo(ScriptAndToolOperate.REJECT.getValue(), ScriptAndToolOperate.REJECT.getText()));
                }
                if (hasModifyAuth) {
                    operateList.add(new OperateVo(ScriptAndToolOperate.REVOKE.getValue(), ScriptAndToolOperate.REVOKE.getText()));
                }
            } else if (Objects.equals(version.getStatus(), ScriptVersionStatus.REJECTED.getValue())) {
                if (hasModifyAuth) {
                    operateList.add(new OperateVo(ScriptAndToolOperate.SAVE.getValue(), ScriptAndToolOperate.SAVE.getText()));
                    OperateVo submit = new OperateVo(ScriptAndToolOperate.SUBMIT.getValue(), ScriptAndToolOperate.SUBMIT.getText());
                    if (autoexecScriptMapper.checkScriptHasSubmittedVersionByScriptId(version.getScriptId()) > 0) {
                        submit.setDisabled(1);
                        submit.setDisabledReason("当前自定义工具已经有其他待审核版本");
                    }
                    operateList.add(submit);
                    operateList.add(new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText()));
                    operateList.add(new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText()));
                    operateList.add(new OperateVo(ScriptAndToolOperate.VERSION_DELETE.getValue(), ScriptAndToolOperate.VERSION_DELETE.getText()));
                }
            } else if (Objects.equals(version.getStatus(), ScriptVersionStatus.PASSED.getValue())) {
                if (Objects.equals(version.getIsActive(), 1)) {
                    if (hasSearchAuth) {
                        operateList.add(new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText()));
                    }
                    if (hasModifyAuth) {
                        operateList.add(new OperateVo(ScriptAndToolOperate.EDIT.getValue(), ScriptAndToolOperate.EDIT.getText()));
                    }
                } else if (!Objects.equals(version.getIsActive(), 1)) {
                    if (hasSearchAuth) {
                        operateList.add(new OperateVo(ScriptAndToolOperate.COMPARE.getValue(), ScriptAndToolOperate.COMPARE.getText()));
                    }
                    if (hasManageAuth) {
                        operateList.add(new OperateVo(ScriptAndToolOperate.SWITCH_VERSION.getValue(), ScriptAndToolOperate.SWITCH_VERSION.getText()));
                    }
                    if (hasModifyAuth) {
                        operateList.add(new OperateVo(ScriptAndToolOperate.VERSION_DELETE.getValue(), ScriptAndToolOperate.VERSION_DELETE.getText()));
                    }
                }
            }
        }
        return operateList;
    }

    @Override
    public void saveParamList(Long versionId, List<AutoexecScriptVersionParamVo> newParamList) {
        if (CollectionUtils.isNotEmpty(newParamList)) {
            List<AutoexecScriptVersionParamVo> inputParamList = newParamList.stream().filter(o -> ParamMode.INPUT.getValue().equals(o.getMode())).collect(Collectors.toList());
            List<AutoexecScriptVersionParamVo> outputParamList = newParamList.stream().filter(o -> ParamMode.OUTPUT.getValue().equals(o.getMode())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(inputParamList)) {
                for (int i = 0; i < inputParamList.size(); i++) {
                    AutoexecScriptVersionParamVo paramVo = inputParamList.get(i);
                    paramVo.setScriptVersionId(versionId);
                    // 如果默认值不以"RC4:"开头，说明修改了密码，则重新加密
                    if (ParamType.PASSWORD.getValue().equals(paramVo.getType()) && StringUtils.isNotBlank(paramVo.getDefaultValueStr())) {
                        paramVo.setDefaultValue(RC4Util.encrypt((String) paramVo.getDefaultValue()));
                    }
                    paramVo.setSort(i);
                    if (paramVo.getConfig() == null) {
                        autoexecService.mergeConfig(paramVo);
                    }
                }
                autoexecScriptMapper.insertScriptVersionParamList(inputParamList);
            }
            if (CollectionUtils.isNotEmpty(outputParamList)) {
                for (int i = 0; i < outputParamList.size(); i++) {
                    AutoexecScriptVersionParamVo paramVo = outputParamList.get(i);
                    paramVo.setScriptVersionId(versionId);
                    paramVo.setSort(i);
                    if (paramVo.getConfig() == null) {
                        autoexecService.mergeConfig(paramVo);
                    }
                }
                autoexecScriptMapper.insertScriptVersionParamList(outputParamList);
            }
        }
    }

    @Override
    public void saveLineList(Long scriptId, Long versionId, List<AutoexecScriptLineVo> lineList) {
        if (CollectionUtils.isNotEmpty(lineList)) {
            int lineNumber = 0;
            List<AutoexecScriptLineVo> buffer = new ArrayList<>(100);
            for (AutoexecScriptLineVo line : lineList) {
                line.setId(null);
                line.setLineNumber(++lineNumber);
                line.setScriptId(scriptId);
                line.setScriptVersionId(versionId);
                if (StringUtils.isNotBlank(line.getContent())) {
                    AutoexecScriptLineContentVo content = new AutoexecScriptLineContentVo(line.getContent());
                    line.setContentHash(content.getHash());
                    if (autoexecScriptMapper.checkScriptLineContentHashIsExists(content.getHash()) == 0) {
                        autoexecScriptMapper.insertScriptLineContent(content);
                    }
                }
                buffer.add(line);
                if (buffer.size() >= 100) {
                    autoexecScriptMapper.insertScriptLineList(buffer);
                    buffer.clear();
                }
            }
            if (CollectionUtils.isNotEmpty(buffer)) {
                autoexecScriptMapper.insertScriptLineList(buffer);
            }
        }
    }

    /**
     * 批量插入脚本参数
     *
     * @param paramList 参数列表
     * @param batchSize 每批的数量
     */
    @Override
    public void batchInsertScriptVersionParamList(List<AutoexecScriptVersionParamVo> paramList, int batchSize) {
        if (CollectionUtils.isNotEmpty(paramList)) {
            int begin = 0;
            int end = begin + batchSize;
            while (paramList.size() - 1 >= begin) {
                autoexecScriptMapper.insertScriptVersionParamList(paramList.subList(begin, Math.min(paramList.size(), end)));
                begin = end;
                end = begin + batchSize;
            }
        }
    }

    /**
     * 批量插入脚本内容行
     *
     * @param lineList  内容行列表
     * @param batchSize 每批的数量
     */
    @Override
    public void batchInsertScriptLineList(List<AutoexecScriptLineVo> lineList, int batchSize) {
        if (CollectionUtils.isNotEmpty(lineList)) {
            int begin = 0;
            int end = begin + batchSize;
            while (lineList.size() - 1 >= begin) {
                autoexecScriptMapper.insertScriptLineList(lineList.subList(begin, Math.min(lineList.size(), end)));
                begin = end;
                end = begin + batchSize;
            }
        }
    }

    /**
     * 记录活动
     *
     * @param auditVo 活动VO
     */
    @Override
    public void audit(AutoexecScriptAuditVo auditVo) {
        auditVo.setFcu(UserContext.get().getUserUuid());
        if (MapUtils.isNotEmpty(auditVo.getConfig())) {
            AutoexecScriptAuditContentVo contentVo = new AutoexecScriptAuditContentVo(auditVo.getConfig().toJSONString());
            autoexecScriptMapper.insertScriptAuditDetail(contentVo);
            auditVo.setContentHash(contentVo.getHash());
        }
        autoexecScriptMapper.insertScriptAudit(auditVo);
    }

    @Override
    public DependencyInfoVo getScriptDependencyPageUrl(Map<String, Object> map, Long scriptId, String groupName) {
        AutoexecScriptVersionVo version;
        AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(scriptId);
        if (scriptVo == null) {
            return null;
        }
        // 按照“激活-草稿-待审核-已驳回”的顺序查找当前脚本的版本
        version = autoexecScriptMapper.getActiveVersionByScriptId(scriptId);
        if (version == null) {
            version = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.DRAFT.getValue());
            if (version == null) {
                version = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.SUBMITTED.getValue());
                if (version == null) {
                    version = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.REJECTED.getValue());
                }
            }
        }
        if (version != null) {
            JSONObject dependencyInfoConfig = new JSONObject();
            dependencyInfoConfig.put("scriptId", scriptVo.getId());
//            dependencyInfoConfig.put("scriptName", scriptVo.getName());
            dependencyInfoConfig.put("versionId", version.getId());
            dependencyInfoConfig.put("versionStatus", version.getStatus());
            dependencyInfoConfig.put("versionStatusText", ScriptVersionStatus.getText(version.getStatus()));
            List<String> pathList = new ArrayList<>();
            pathList.add("自定义工具库");
            String lastName = scriptVo.getName();
//            String pathFormatString =  "自定义工具库-${DATA.scriptName}";
            String urlFormat;
            //submitted的页面不一样
            if (Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), version.getStatus())) {
                urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/review-detail?versionId=${DATA.versionId}";
            } else {
                urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/script-detail?scriptId=${DATA.scriptId}&status=${DATA.versionStatus}";
            }
            return new DependencyInfoVo(scriptVo.getId(), dependencyInfoConfig, lastName, pathList, urlFormat, groupName);
        }
        return null;
    }

    @Override
    public Long getCatalogIdByCatalogPath(String catalogPath) {
        Long catalogId = null;
        if (catalogPath.contains("/")) {
            String[] split = catalogPath.split("/");
            AutoexecCatalogVo catalogVo = null;
            for (int j = 0; j < split.length; j++) {
                String name = split[j];
                if (j == 0) {
                    catalogVo = autoexecCatalogMapper.getAutoexecCatalogByNameAndParentId(name, AutoexecCatalogVo.ROOT_ID);
                } else if (catalogVo != null) {
                    catalogVo = autoexecCatalogMapper.getAutoexecCatalogByNameAndParentId(name, catalogVo.getId());
                }
            }
            if (catalogVo != null) {
                catalogId = catalogVo.getId();
            }
        } else {
            AutoexecCatalogVo catalog = autoexecCatalogMapper.getAutoexecCatalogByName(catalogPath);
            if (catalog != null) {
                catalogId = catalog.getId();
            }
        }
        return catalogId;
    }

    @Override
    public Long createCatalogByCatalogPath(String catalogPath) {
        Long catalogId;
        if (catalogPath.contains("/")) {
            String[] split = catalogPath.split("/");
            AutoexecCatalogVo catalogVo = new AutoexecCatalogVo();
            catalogVo.setId(AutoexecCatalogVo.ROOT_ID);
            int index = -1;
            for (String name : split) {
                AutoexecCatalogVo vo = autoexecCatalogMapper.getAutoexecCatalogByNameAndParentId(name, catalogVo.getId());
                if (vo != null) {
                    catalogVo = vo;
                    index++;
                } else {
                    break;
                }
            }
            if (index != split.length) {
                for (int i = index + 1; i < split.length; i++) {
                    String name = split[i];
                    int lft = LRCodeManager.beforeAddTreeNode("autoexec_catalog", "id", "parent_id", catalogVo.getId());
                    AutoexecCatalogVo vo = new AutoexecCatalogVo(name, catalogVo.getId(), lft, lft + 1);
                    autoexecCatalogMapper.insertAutoexecCatalog(vo);
                    catalogVo = vo;
                }
            }
            catalogId = catalogVo.getId();
        } else {
            AutoexecCatalogVo catalog = autoexecCatalogMapper.getAutoexecCatalogByName(catalogPath);
            if (catalog != null) {
                catalogId = catalog.getId();
            } else {
                int lft = LRCodeManager.beforeAddTreeNode("autoexec_catalog", "id", "parent_id", AutoexecCatalogVo.ROOT_ID);
                AutoexecCatalogVo vo = new AutoexecCatalogVo(catalogPath, AutoexecCatalogVo.ROOT_ID, lft, lft + 1);
                autoexecCatalogMapper.insertAutoexecCatalog(vo);
                catalogId = vo.getId();
            }
        }
        return catalogId;
    }

    @Override
    public void deleteScriptById(Long id) {
        if (id != null) {
            // 检查脚本是否被组合工具引用
            if (DependencyManager.getDependencyCount(AutoexecFromType.SCRIPT, id) > 0) {
                throw new AutoexecScriptHasReferenceException();
            }
            List<Long> versionIdList = autoexecScriptMapper.getVersionIdListByScriptId(id);
            if (CollectionUtils.isNotEmpty(versionIdList)) {
                autoexecScriptMapper.deleteParamByVersionIdList(versionIdList);
                autoexecScriptMapper.deleteArgumentByVersionIdList(versionIdList);
            }
            //删除依赖工具关系
            autoexecScriptMapper.deleteScriptVersionLibByLibScriptId(id);
            autoexecScriptMapper.deleteScriptVersionLibByScriptId(id);
            //删除脚本和profile的关系
            autoexecProfileMapper.deleteProfileOperationByOperationId(id);
            autoexecScriptMapper.deleteScriptLineByScriptId(id);
            autoexecScriptMapper.deleteScriptAuditByScriptId(id);
            autoexecScriptMapper.deleteScriptVersionByScriptId(id);
            autoexecScriptMapper.deleteScriptById(id);
        }
    }
}
