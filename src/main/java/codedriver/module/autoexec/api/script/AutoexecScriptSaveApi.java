/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNameOrUkRepeatException;
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
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Resource
    private AutoexecService autoexecService;

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
//            @Param(name = "uk", type = ApiParamType.REGEX, rule = "^[A-Za-z]+$", isRequired = true, xss = true, desc = "唯一标识"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", maxLength = 50, isRequired = true, xss = true, desc = "名称"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sql", desc = "执行方式", isRequired = true),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "脚本分类ID", isRequired = true),
            @Param(name = "riskId", type = ApiParamType.LONG, desc = "操作级别ID", isRequired = true),
            @Param(name = "title", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", maxLength = 50, isRequired = true, xss = true, desc = "版本标题"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, desc = "参数列表"),
            @Param(name = "parser", type = ApiParamType.ENUM, rule = "python,ruby,vbscript,shell,perl,powershell,cmd,bash,ksh,csh,sh,javascript,xml,sql", desc = "脚本解析器"),
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "脚本内容行数据列表,e.g:[{\"content\":\"#!/usr/bin/env bash\"},{\"content\":\"show_ascii_berry()\"}]"),
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "版本id"),
            @Param(name = "isReviewable", type = ApiParamType.ENUM, rule = "0,1", desc = "是否能审批(1:能;0:不能)"),
    })
    @Description(desc = "保存脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        JSONObject result = new JSONObject();
        AutoexecScriptVo scriptVo = JSON.toJavaObject(jsonObj, AutoexecScriptVo.class);
        boolean needSave = true;
        List<AutoexecScriptVersionParamVo> oldParamList = null;

        /**
         * 没有id和versionId，表示首次创建脚本
         * 有id没有versionId，表示新增一个版本，脚本基本信息不作修改
         * 没有id有versionId，表示编辑某个版本，脚本基本信息不作修改
         */
        AutoexecScriptVersionVo versionVo = new AutoexecScriptVersionVo();
        versionVo.setTitle(jsonObj.getString("title"));
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
                //versionVo.setVersion(1);
                versionVo.setIsActive(0);
                autoexecScriptMapper.insertScriptVersion(versionVo);
                scriptVo.setVersionId(versionVo.getId());
            } else {  // 编辑版本
                AutoexecScriptVersionVo currentVersion = autoexecScriptService.getScriptVersionDetailByVersionId(scriptVo.getVersionId());
                scriptVo.setId(currentVersion.getScriptId());
                oldParamList = currentVersion.getParamList();
                // 处于待审批和已通过状态的版本，任何权限都无法编辑
                if (ScriptVersionStatus.SUBMITTED.getValue().equals(currentVersion.getStatus())
                        || ScriptVersionStatus.PASSED.getValue().equals(currentVersion.getStatus())) {
                    throw new AutoexecScriptVersionCannotEditException();
                }
                // 检查内容是否有变更，没有则不执行更新
                AutoexecScriptVersionVo newVersion = new AutoexecScriptVersionVo();
                newVersion.setParser(scriptVo.getParser());
                newVersion.setParamList(scriptVo.getParamList());
                newVersion.setLineList(scriptVo.getLineList());
                needSave = autoexecScriptService.checkScriptVersionNeedToUpdate(currentVersion, newVersion);
                if (needSave) {
                    autoexecScriptMapper.deleteParamByVersionId(currentVersion.getId());
                    autoexecScriptMapper.deleteScriptLineByVersionId(currentVersion.getId());
                }
                versionVo.setId(currentVersion.getId());
                autoexecScriptMapper.updateScriptVersion(versionVo);
            }
        } else { // 新增版本
            if (autoexecScriptMapper.checkScriptIsExistsById(scriptVo.getId()) == 0) {
                throw new AutoexecScriptNotFoundException(scriptVo.getId());
            }
            //Integer maxVersion = autoexecScriptMapper.getMaxVersionByScriptId(scriptVo.getId());
            //versionVo.setVersion(maxVersion != null ? maxVersion + 1 : 1);
            versionVo.setScriptId(scriptVo.getId());
            versionVo.setIsActive(0);
            autoexecScriptMapper.insertScriptVersion(versionVo);
            scriptVo.setVersionId(versionVo.getId());
        }
        if (needSave) {
            // 保存参数
            List<AutoexecScriptVersionParamVo> paramList = scriptVo.getParamList();
            if (CollectionUtils.isNotEmpty(paramList)) {
                autoexecService.validateParamList(paramList);
            }
            autoexecScriptService.saveParamList(versionVo.getId(), oldParamList, paramList);
            // 保存脚本内容
            autoexecScriptService.saveLineList(scriptVo.getId(), scriptVo.getVersionId(), scriptVo.getLineList());
        }
        result.put("id", scriptVo.getId());
        result.put("versionId", scriptVo.getVersionId());
        result.put("isReviewable", AuthActionChecker.check(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName()) ? 1 : 0);
        return result;
    }

    public IValid name() {
        return value -> {
            AutoexecScriptVo scriptVo = JSON.toJavaObject(value, AutoexecScriptVo.class);
            if (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
                return new FieldValidResultVo(new AutoexecScriptNameOrUkRepeatException(scriptVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

//    public IValid uk() {
//        return value -> {
//            AutoexecScriptVo scriptVo = JSON.toJavaObject(value, AutoexecScriptVo.class);
//            if (autoexecScriptMapper.checkScriptUkIsExists(scriptVo) > 0) {
//                return new FieldValidResultVo(new AutoexecScriptNameOrUkRepeatException(scriptVo.getUk()));
//            }
//            return new FieldValidResultVo();
//        };
//    }


}
