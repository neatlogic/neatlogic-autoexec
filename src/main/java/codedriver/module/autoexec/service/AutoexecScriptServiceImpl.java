/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ParamMode;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.constvalue.ScriptAndToolOperate;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.crossover.IAutoexecScriptServiceCrossoverService;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.dto.script.*;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.CiphertextPrefix;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dto.OperateVo;
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
public class AutoexecScriptServiceImpl implements AutoexecScriptService, IAutoexecScriptServiceCrossoverService {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecService autoexecService;


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
        version.setLineList(autoexecScriptMapper.getLineListByVersionId(versionId));
        version.setArgument(autoexecScriptMapper.getArgumentByVersionId(versionId));
        return version;
    }

    @Override
    public List<AutoexecScriptVersionVo> getScriptVersionDetailListByScriptId(AutoexecScriptVersionVo vo) {
        List<AutoexecScriptVersionVo> versionList = autoexecScriptMapper.getVersionListByScriptId(vo);
        if (CollectionUtils.isNotEmpty(versionList)) {
            for (AutoexecScriptVersionVo version : versionList) {
                version.setParamList(autoexecScriptMapper.getParamListByVersionId(version.getId()));
                version.setArgument(autoexecScriptMapper.getArgumentByVersionId(version.getId()));
                version.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
            }
        }
        return versionList;
    }

    /**
     * 校验脚本的基本信息，包括name、uk、分类、操作级别
     *
     * @param scriptVo 脚本VO
     */
    @Override
    public void validateScriptBaseInfo(AutoexecScriptVo scriptVo) {
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
        if (autoexecRiskMapper.checkRiskIsExistsById(scriptVo.getRiskId()) == 0) {
            throw new AutoexecRiskNotFoundException(scriptVo.getRiskId());
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
        Iterator<AutoexecScriptLineVo> beforeLineIterator = beforeLineList.iterator();
        Iterator<AutoexecScriptLineVo> afterLineIterator = afterLineList.iterator();
        while (beforeLineIterator.hasNext() && afterLineIterator.hasNext()) {
            String beforeContent = beforeLineIterator.next().getContentHash();
            String afterContent = DigestUtils.md5DigestAsHex(afterLineIterator.next().getContent().getBytes());
            if (!Objects.equals(beforeContent, afterContent)) {
                return true;
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
                    if (ParamType.PASSWORD.getValue().equals(paramVo.getType()) && StringUtils.isNotBlank(paramVo.getDefaultValueStr())
                            && !paramVo.getDefaultValueStr().startsWith(CiphertextPrefix.RC4.getValue())) {
                        paramVo.setDefaultValue(CiphertextPrefix.RC4.getValue() + RC4Util.encrypt((String) paramVo.getDefaultValue()));
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
    public DependencyInfoVo getScriptDependencyPageUrl(Map<String, Object> map, Long scriptId, String groupName, String pathFormat) {

        AutoexecScriptVersionVo version = null;
        Boolean hasStatus = false;
        AutoexecScriptVo scriptVo = autoexecScriptMapper.getScriptBaseInfoById(scriptId);
        if (scriptVo == null) {
            return null;
        }

        List<AutoexecScriptVersionVo> versionVoList = autoexecScriptMapper.getScriptVersionListByScriptId(scriptId);
        AutoexecScriptVersionVo versionVo = versionVoList.get(0);
        String status = versionVo.getStatus();

        if (Objects.equals(ScriptVersionStatus.PASSED.getValue(), status)) {
            AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(scriptId);
            if (activeVersion != null) {
                version = activeVersion;
                hasStatus = true;
            } else {
                throw new AutoexecScriptVersionHasNoActivedException();
            }
        } else if (hasStatus == false && Objects.equals(ScriptVersionStatus.DRAFT.getValue(), status)) {
            AutoexecScriptVersionVo recentlyDraftVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.DRAFT.getValue());
            if (recentlyDraftVersion != null) {
                version = recentlyDraftVersion;
                hasStatus = true;
            } else {
                throw new AutoexecScriptHasNoDraftVersionException();
            }
        } else if (hasStatus == false && Objects.equals(ScriptVersionStatus.REJECTED.getValue(), status)) {
            AutoexecScriptVersionVo recentlyRejectedVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(scriptId, ScriptVersionStatus.REJECTED.getValue());
            if (recentlyRejectedVersion != null) {
                version = recentlyRejectedVersion;
                hasStatus = true;
            } else {
                throw new AutoexecScriptHasNoRejectedVersionException();
            }
        }
        if (scriptVo != null && StringUtils.isNotBlank(status)) {
            JSONObject dependencyInfoConfig = new JSONObject();
            dependencyInfoConfig.put("scriptId", scriptVo.getId());
            dependencyInfoConfig.put("scriptName", scriptVo.getName());
            dependencyInfoConfig.put("versionId", versionVo.getId());
            if (versionVo.getStatusVo() != null) {
                dependencyInfoConfig.put("versionStatus", version.getStatusVo().getValue());
                dependencyInfoConfig.put("versionStatusText", version.getStatusVo().getText());
            }
            String pathFormatString = pathFormat + "-${DATA.scriptName}";
            String urlFormat = "";
            //submitted的页面不一样
            if (Objects.equals(ScriptVersionStatus.SUBMITTED.getValue(), status)) {
                urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/review-detail?versionId=${DATA.versionId}";
            } else if (version != null) {
                urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/script-detail?scriptId=${DATA.scriptId}&status=${DATA.versionStatus}";
            }
            return new DependencyInfoVo(scriptVo.getId(), dependencyInfoConfig, pathFormatString, urlFormat, groupName);
        }
        return null;
    }

}
