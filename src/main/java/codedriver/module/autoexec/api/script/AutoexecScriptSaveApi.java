/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.*;
import codedriver.framework.autoexec.exception.AutoexecScriptNameOrLabelRepeatException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionCannotEditException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/save";
    }

    @Override
    public String getName() {
        return "保存脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID(没有id和versionId,表示首次创建脚本;有id没有versionId,表示新增一个版本;没有id有versionId,表示编辑某个版本)"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "脚本版本ID"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z]+$", isRequired = true, xss = true, desc = "唯一标识"),
            @Param(name = "label", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, xss = true, desc = "名称"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "local,remote,localremote", desc = "执行方式", isRequired = true),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "脚本分类ID", isRequired = true),
            @Param(name = "riskId", type = ApiParamType.LONG, desc = "操作级别ID", isRequired = true),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, desc = "参数列表"),
            @Param(name = "parser", type = ApiParamType.ENUM, rule = "python,vbs,shell,perl,powershell,bat,xml", desc = "脚本解析器"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, desc = "脚本内容行数据列表"),
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本id"),
    })
    @Description(desc = "保存脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        JSONObject result = new JSONObject();
        AutoexecScriptVo scriptVo = JSON.toJavaObject(jsonObj, AutoexecScriptVo.class);
        boolean needSave = true;

        /**
         * 没有id和versionId，表示首次创建脚本
         * 有id没有versionId，表示新增一个版本，脚本基本信息不作修改
         * 没有id有versionId，表示编辑某个版本，脚本基本信息不作修改
         */
        AutoexecScriptVersionVo versionVo = new AutoexecScriptVersionVo();
        versionVo.setParser(scriptVo.getParser());
        versionVo.setLcu(UserContext.get().getUserUuid());
        versionVo.setStatus(ScriptVersionStatus.DRAFT.getValue());

        // todo 校验脚本内容

        if (jsonObj.getLong("id") == null) {
            if (scriptVo.getVersionId() == null) { // 首次创建脚本
                autoexecScriptService.validateScriptBaseInfo(scriptVo);
                scriptVo.setFcu(UserContext.get().getUserUuid());
                autoexecScriptMapper.insertScript(scriptVo);
                versionVo.setScriptId(scriptVo.getId());
                versionVo.setVersion(0);
                versionVo.setIsActive(0);
                autoexecScriptMapper.insertScriptVersion(versionVo);
                scriptVo.setVersionId(versionVo.getId());
            } else {  // 编辑版本
                AutoexecScriptVersionVo currentVersion = autoexecScriptService.getScriptVersionDetailByVersionId(scriptVo.getVersionId());
                // 处于待审批和已通过状态的版本，任何权限都无法编辑
                if (ScriptVersionStatus.SUBMITTED.getValue().equals(currentVersion.getStatus())
                        || ScriptVersionStatus.PASSED.getValue().equals(currentVersion.getStatus())) {
                    throw new AutoexecScriptVersionCannotEditException();
                }
                // 检查内容是否有变更，没有则不执行更新
                needSave = checkScriptVersionNeedToUpdate(currentVersion, scriptVo);
                if (needSave) {
                    versionVo.setId(currentVersion.getId());
                    autoexecScriptMapper.deleteParamByVersionId(currentVersion.getId());
                    autoexecScriptMapper.deleteScriptLineByVersionId(currentVersion.getId());
                    autoexecScriptMapper.updateScriptVersion(versionVo);
                }
            }
        } else { // 新增版本
            if (autoexecScriptMapper.checkScriptIsExistsById(scriptVo.getId()) == 0) {
                throw new AutoexecScriptNotFoundException(scriptVo.getId());
            }
            Integer maxVersion = autoexecScriptMapper.getMaxVersionByScriptId(scriptVo.getId());
            versionVo.setVersion(maxVersion != null ? maxVersion + 1 : 0);
            versionVo.setScriptId(scriptVo.getId());
            versionVo.setIsActive(0);
            autoexecScriptMapper.insertScriptVersion(versionVo);
            scriptVo.setVersionId(versionVo.getId());
        }
        if (needSave) {
            // 保存参数
            List<AutoexecScriptVersionParamVo> paramList = scriptVo.getParamList();
            if (CollectionUtils.isNotEmpty(paramList)) {
                paramList.stream().forEach(o -> o.setScriptVersionId(versionVo.getId()));
                autoexecScriptMapper.insertScriptVersionParamList(paramList);
            }
            // 保存脚本内容
            saveScriptLineList(scriptVo);
        }
        result.put("id", scriptVo.getId());
        result.put("versionId", scriptVo.getVersionId());
        return result;
    }

    /**
     * 保存脚本内容行
     * @param scriptVo 脚本VO
     */
    private void saveScriptLineList(AutoexecScriptVo scriptVo) {
        if (CollectionUtils.isNotEmpty(scriptVo.getLineList())) {
            int lineNumber = 0;
            List<AutoexecScriptLineVo> lineList = new ArrayList<>();
            for (String line : scriptVo.getLineList()) {
                AutoexecScriptLineVo lineVo = new AutoexecScriptLineVo();
                lineVo.setLineNumber(++lineNumber);
                lineVo.setScriptId(scriptVo.getId());
                lineVo.setScriptVersionId(scriptVo.getVersionId());
                if (StringUtils.isNotBlank(line)) {
                    AutoexecScriptLineContentVo content = new AutoexecScriptLineContentVo(line);
                    lineVo.setContentHash(content.getHash());
                    if (autoexecScriptMapper.checkScriptLineContentHashIsExists(content.getHash()) == 0) {
                        autoexecScriptMapper.insertScriptLineContent(content);
                    }
                }
                lineList.add(lineVo);
                if (lineList.size() >= 100) {
                    autoexecScriptMapper.insertScriptLineList(lineList);
                    lineList.clear();
                }
            }
            if (CollectionUtils.isNotEmpty(lineList)) {
                autoexecScriptMapper.insertScriptLineList(lineList);
            }
        }
    }

    /**
     * 检查脚本内容是否有变更
     * @param before 当前版本
     * @param after  待更新的内容
     * @return 是否有变更
     */
    private boolean checkScriptVersionNeedToUpdate(AutoexecScriptVersionVo before, AutoexecScriptVo after) {
        if (!Objects.equals(before.getParser(), after.getParser())) {
            return true;
        }
        List<AutoexecScriptVersionParamVo> beforeParamList = new ArrayList<>();
        beforeParamList.addAll(before.getParamList());
        List<AutoexecScriptVersionParamVo> afterParamList = new ArrayList<>();
        afterParamList.addAll(after.getParamList());
        if (beforeParamList.size() != afterParamList.size()) {
            return true;
        }
        Iterator<AutoexecScriptVersionParamVo> beforeParamIterator = beforeParamList.iterator();
        Iterator<AutoexecScriptVersionParamVo> afterParamIterator = afterParamList.iterator();
        while (beforeParamIterator.hasNext() && afterParamIterator.hasNext()) {
            AutoexecScriptVersionParamVo beforeNextParam = beforeParamIterator.next();
            AutoexecScriptVersionParamVo afterNextParam = afterParamIterator.next();
            if (!Objects.equals(beforeNextParam.getKey(), afterNextParam.getKey())) {
                return true;
            }
            if (!Objects.equals(beforeNextParam.getDefaultValue(), afterNextParam.getDefaultValue())) {
                return true;
            }
            if (!Objects.equals(beforeNextParam.getType(), afterNextParam.getType())) {
                return true;
            }
            if (!Objects.equals(beforeNextParam.getHandler(), afterNextParam.getHandler())) {
                return true;
            }
            if (!Objects.equals(beforeNextParam.getIsRequired(), afterNextParam.getIsRequired())) {
                return true;
            }
            if (!Objects.equals(beforeNextParam.getDescription(), afterNextParam.getDescription())) {
                return true;
            }
        }
        List<AutoexecScriptLineVo> beforeLineList = new ArrayList<>();
        beforeLineList.addAll(before.getLineList());
        List<String> afterLineList = new ArrayList<>();
        afterLineList.addAll(after.getLineList());
        if (beforeLineList.size() != afterLineList.size()) {
            return true;
        }
        Iterator<AutoexecScriptLineVo> beforeLineIterator = beforeLineList.iterator();
        Iterator<String> afterLineIterator = afterLineList.iterator();
        while (beforeLineIterator.hasNext() && afterLineIterator.hasNext()) {
            String beforeContent = beforeLineIterator.next().getContent();
            String afterContent = afterLineIterator.next();
            if (!Objects.equals(beforeContent, afterContent)) {
                return true;
            }
        }
        return false;
    }

    public IValid name() {
        return value -> {
            AutoexecScriptVo scriptVo = JSON.toJavaObject(value, AutoexecScriptVo.class);
            if (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
                return new FieldValidResultVo(new AutoexecScriptNameOrLabelRepeatException(scriptVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

    public IValid label() {
        return value -> {
            AutoexecScriptVo scriptVo = JSON.toJavaObject(value, AutoexecScriptVo.class);
            if (autoexecScriptMapper.checkScriptLabelIsExists(scriptVo) > 0) {
                return new FieldValidResultVo(new AutoexecScriptNameOrLabelRepeatException(scriptVo.getLabel()));
            }
            return new FieldValidResultVo();
        };
    }


}