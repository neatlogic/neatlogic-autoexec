/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.importexport.handler;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dto.AutoexecRiskVo;
import neatlogic.framework.autoexec.exception.AutoexecRiskNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.service.AutoexecRiskService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class RiskImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;
    @Resource
    private AutoexecRiskService autoexecRiskService;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_RISK;
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
        return autoexecRiskMapper.getAutoexecRiskByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        AutoexecRiskVo oldAutoexecRiskVo = autoexecRiskMapper.getAutoexecRiskByName(importExportVo.getName());
        if (oldAutoexecRiskVo == null) {
            throw new AutoexecRiskNotFoundException(importExportVo.getName());
        }
        return oldAutoexecRiskVo.getId();
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        AutoexecRiskVo autoexecRiskVo = importExportVo.getData().toJavaObject(AutoexecRiskVo.class);
        autoexecRiskVo.setFcu(UserContext.get().getUserUuid());
        autoexecRiskVo.setLcu(UserContext.get().getUserUuid());
        autoexecRiskVo.setIsActive(1);
        AutoexecRiskVo oldAutoexecRiskVo = autoexecRiskMapper.getAutoexecRiskByName(autoexecRiskVo.getName());
        if (oldAutoexecRiskVo != null) {
            autoexecRiskVo.setId(oldAutoexecRiskVo.getId());
        } else {
            if (autoexecRiskMapper.getAutoexecRiskById(autoexecRiskVo.getId()) != null) {
                autoexecRiskVo.setId(null);
            }
        }
        autoexecRiskService.saveRisk(autoexecRiskVo);
        return autoexecRiskVo.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AutoexecRiskVo autoexecRiskVo = autoexecRiskMapper.getAutoexecRiskById(id);
        if (autoexecRiskVo == null) {
            throw new AutoexecRiskNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecRiskVo.getName());
        importExportVo.setDataWithObject(autoexecRiskVo);
        return importExportVo;
    }
}
