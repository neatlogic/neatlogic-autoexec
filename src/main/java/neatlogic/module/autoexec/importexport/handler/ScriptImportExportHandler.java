/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.importexport.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.autoexec.constvalue.ScriptAction;
import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import neatlogic.framework.importexport.constvalue.FrameworkImportExportHandlerType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class ScriptImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;


    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT;
    }

    @Override
    public boolean checkImportAuth(ImportExportVo importExportVo) {
        return true;
    }

    @Override
    public boolean checkExportAuth(Object primaryKey) {
        return true;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return autoexecScriptMapper.getScriptBaseInfoByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        JSONObject data = importExportVo.getData();
        AutoexecScriptVo autoexecScriptVo = data.toJavaObject(AutoexecScriptVo.class);
        AutoexecScriptVo oldAutoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoByName(autoexecScriptVo.getName());
        if (oldAutoexecScriptVo != null) {
            autoexecScriptVo.setId(oldAutoexecScriptVo.getId());
        } else {
            if (autoexecScriptMapper.checkScriptIsExistsById(autoexecScriptVo.getId()) > 0) {
                autoexecScriptVo.setId(null);
            }
        }
        if (autoexecScriptVo.getTypeId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TYPE, autoexecScriptVo.getTypeId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecScriptVo.setTypeId((Long) newPrimaryKey);
            }
        }
        if (autoexecScriptVo.getRiskId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_RISK, autoexecScriptVo.getRiskId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecScriptVo.setRiskId((Long) newPrimaryKey);
            }
        }
        if (autoexecScriptVo.getCustomTemplateId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_CUSTOM_TEMPLATE, autoexecScriptVo.getCustomTemplateId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecScriptVo.setCustomTemplateId((Long) newPrimaryKey);
            }
        }
        if (autoexecScriptVo.getDefaultProfileId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, autoexecScriptVo.getDefaultProfileId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecScriptVo.setDefaultProfileId((Long) newPrimaryKey);
            }
        }
        if (autoexecScriptVo.getCatalogId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_CATALOG, autoexecScriptVo.getCatalogId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecScriptVo.setCatalogId((Long) newPrimaryKey);
            }
        }
        AutoexecScriptVersionVo version = autoexecScriptVo.getCurrentVersionVo();
        if (version.getPackageFileId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(FrameworkImportExportHandlerType.FILE, version.getPackageFileId(), primaryChangeList);
            if (newPrimaryKey != null) {
                version.setPackageFileId((Long) newPrimaryKey);
            }
        }
        // 保存
        version.setId(null);// 新增一个版本
        version.setStatus(ScriptVersionStatus.DRAFT.getValue());
        autoexecScriptService.saveScriptAndVersion(autoexecScriptVo, version);
        // 审批
        version.setScriptId(autoexecScriptVo.getId());
        autoexecScriptService.reviewVersion(version, ScriptAction.PASS.getValue(), "The imported version automatically passes the approval");
        return autoexecScriptVo.getId();
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
        if (autoexecScriptVo == null) {
            throw new AutoexecScriptNotFoundException(id);
        }
        if (autoexecScriptVo.getTypeId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TYPE, autoexecScriptVo.getTypeId(), dependencyList, zipOutputStream);
        }
        if (autoexecScriptVo.getRiskId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_RISK, autoexecScriptVo.getRiskId(), dependencyList, zipOutputStream);
        }
        if (autoexecScriptVo.getCustomTemplateId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_CUSTOM_TEMPLATE, autoexecScriptVo.getCustomTemplateId(), dependencyList, zipOutputStream);
        }
        if (autoexecScriptVo.getDefaultProfileId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, autoexecScriptVo.getDefaultProfileId(), dependencyList, zipOutputStream);
        }
        if (autoexecScriptVo.getCatalogId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_CATALOG, autoexecScriptVo.getCatalogId(), dependencyList, zipOutputStream);
        }
        AutoexecScriptVersionVo version = autoexecScriptMapper.getActiveVersionByScriptId(id);
        if (version == null) {
            throw new AutoexecScriptVersionHasNoActivedException(autoexecScriptVo.getName());
        }

        List<AutoexecScriptVersionParamVo> paramList = autoexecScriptMapper.getParamListByVersionId(version.getId());
        version.setParamList(paramList);
        version.setArgument(autoexecScriptMapper.getArgumentByVersionId(version.getId()));
        if (!StringUtils.equals(version.getParser(), ScriptParser.PACKAGE.getValue())) {
            version.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
        }
        if (version.getPackageFileId() != null) {
            doExportData(FrameworkImportExportHandlerType.FILE, version.getPackageFileId(), dependencyList, zipOutputStream);
        }
        //获取依赖工具
        List<Long> useLib = autoexecScriptMapper.getLibScriptIdListByVersionId(version.getId());
        version.setUseLib(useLib);
        if (CollectionUtils.isNotEmpty(useLib)) {
            for (Long useLibId : useLib) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, useLibId, dependencyList, zipOutputStream);
            }
        }
        autoexecScriptVo.setCurrentVersionVo(version);
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecScriptVo.getName());
        importExportVo.setDataWithObject(autoexecScriptVo);
        return importExportVo;
    }
}
