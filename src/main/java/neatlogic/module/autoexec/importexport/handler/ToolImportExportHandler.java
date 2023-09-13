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
import neatlogic.framework.autoexec.constvalue.AutoexecProfileParamInvokeType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.exception.AutoexecToolNotFoundException;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Component
public class ToolImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_TOOL;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return autoexecToolMapper.getToolByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        JSONObject data = importExportVo.getData();
        AutoexecToolVo autoexecToolVo = data.toJavaObject(AutoexecToolVo.class);
        AutoexecToolVo oldAutoexecToolVo = autoexecToolMapper.getToolByName(autoexecToolVo.getName());
        if (oldAutoexecToolVo != null) {
            autoexecToolVo.setId(oldAutoexecToolVo.getId());
        } else {
            if (autoexecToolMapper.getToolById(autoexecToolVo.getId()) != null) {
                autoexecToolVo.setId(null);
            }
        }
        if (autoexecToolVo.getTypeId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TYPE, autoexecToolVo.getTypeId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecToolVo.setTypeId((Long) newPrimaryKey);
            }
        }
        if (autoexecToolVo.getRiskId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_RISK, autoexecToolVo.getRiskId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecToolVo.setRiskId((Long) newPrimaryKey);
            }
        }
        if (autoexecToolVo.getCustomTemplateId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_CUSTOM_TEMPLATE, autoexecToolVo.getCustomTemplateId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecToolVo.setCustomTemplateId((Long) newPrimaryKey);
            }
        }
        if (autoexecToolVo.getDefaultProfileId() != null) {
            Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, autoexecToolVo.getDefaultProfileId(), primaryChangeList);
            if (newPrimaryKey != null) {
                autoexecToolVo.setDefaultProfileId((Long) newPrimaryKey);
            }
        }
        autoexecToolVo.setConfigStr(JSONObject.toJSONString(autoexecToolVo.getConfig()));
        autoexecToolMapper.insertTool(autoexecToolVo);
        return autoexecToolVo.getId();
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportVo> dependencyList) {
        Long id = (Long) primaryKey;
        AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(id);
        if (autoexecToolVo == null) {
            throw new AutoexecToolNotFoundException(id);
        }
        if (autoexecToolVo.getTypeId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TYPE, autoexecToolVo.getTypeId(), dependencyList);
        }
        if (autoexecToolVo.getRiskId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_RISK, autoexecToolVo.getRiskId(), dependencyList);
        }
        if (autoexecToolVo.getCustomTemplateId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_CUSTOM_TEMPLATE, autoexecToolVo.getCustomTemplateId(), dependencyList);
        }
        if (autoexecToolVo.getDefaultProfileId() != null) {
            doExportData(AutoexecImportExportHandlerType.AUTOEXEC_PROFILE, autoexecToolVo.getDefaultProfileId(), dependencyList);
        }
        List<AutoexecParamVo> inputParamList = autoexecToolVo.getInputParamList();
        if (CollectionUtils.isNotEmpty(inputParamList)) {
            for (AutoexecParamVo autoexecParamVo : inputParamList) {
                if (Objects.equals(autoexecParamVo.getMappingMode(), AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue())) {
                    if (autoexecParamVo.getDefaultValue() != null) {
                        doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, autoexecParamVo.getDefaultValue(), dependencyList);
                    }
                }
            }
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecToolVo.getName());
        importExportVo.setDataWithObject(autoexecToolVo);
        return importExportVo;
    }
}
