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

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dto.AutoexecRiskVo;
import neatlogic.framework.autoexec.exception.AutoexecRiskNotFoundException;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.service.AutoexecRiskService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

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
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return autoexecRiskMapper.getAutoexecRiskByName(importExportBaseInfoVo.getName()) != null;
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
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportVo> dependencyList) {
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
