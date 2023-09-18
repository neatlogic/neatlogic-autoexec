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
import neatlogic.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.autoexec.exception.customtemplate.CustomTemplateNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.module.autoexec.service.AutoexecCustomTemplateService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class CustomTemplateImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;

    @Resource
    private AutoexecCustomTemplateService autoexecCustomTemplateService;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_CUSTOM_TEMPLATE;
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
        return autoexecCustomTemplateMapper.getCustomTemplateByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        CustomTemplateVo oldCustomTemplateVo = autoexecCustomTemplateMapper.getCustomTemplateByName(importExportVo.getName());
        if (oldCustomTemplateVo == null) {
            throw new AutoexecCatalogNotFoundException(importExportVo.getName());
        }
        return oldCustomTemplateVo.getId();
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        CustomTemplateVo customTemplateVo = importExportVo.getData().toJavaObject(CustomTemplateVo.class);
        customTemplateVo.setLcu(UserContext.get().getUserUuid());
        customTemplateVo.setFcu(UserContext.get().getUserUuid());
        customTemplateVo.setIsActive(1);
        CustomTemplateVo oldCustomTemplateVo = autoexecCustomTemplateMapper.getCustomTemplateByName(customTemplateVo.getName());
        if (oldCustomTemplateVo != null) {
            customTemplateVo.setId(oldCustomTemplateVo.getId());
        } else {
            if (autoexecCustomTemplateMapper.getCustomTemplateById(customTemplateVo.getId()) != null) {
                customTemplateVo.setId(null);
            }
        }
        autoexecCustomTemplateService.saveCustomTemplate(customTemplateVo);
        return customTemplateVo.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        CustomTemplateVo customTemplateVo = autoexecCustomTemplateMapper.getCustomTemplateById(id);
        if (customTemplateVo == null) {
            throw new CustomTemplateNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, customTemplateVo.getName());
        importExportVo.setDataWithObject(customTemplateVo);
        return importExportVo;
    }
}
