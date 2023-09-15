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

import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.service.AutoexecCatalogService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class CatalogImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;
    @Resource
    private AutoexecCatalogService autoexecCatalogService;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_CATALOG;
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
        if (autoexecCatalogMapper.getAutoexecCatalogByName(importExportBaseInfoVo.getName()) != null) {
            return true;
        }
        return false;
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        AutoexecCatalogVo autoexecCatalogVo = importExportVo.getData().toJavaObject(AutoexecCatalogVo.class);
        AutoexecCatalogVo oldAutoexecCatalogVo = autoexecCatalogMapper.getAutoexecCatalogByName(autoexecCatalogVo.getName());
        if (oldAutoexecCatalogVo != null) {
            return oldAutoexecCatalogVo.getId();
        }
        if (autoexecCatalogMapper.getAutoexecCatalogById(autoexecCatalogVo.getId()) != null) {
            autoexecCatalogVo.setId(null);
        }
        if (autoexecCatalogMapper.getAutoexecCatalogById(autoexecCatalogVo.getParentId()) == null) {
            autoexecCatalogVo.setParentId(AutoexecCatalogVo.ROOT_ID);
        }
        return autoexecCatalogService.saveAutoexecCatalog(autoexecCatalogVo);
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AutoexecCatalogVo autoexecCatalogVo = autoexecCatalogMapper.getAutoexecCatalogById(id);
        if (autoexecCatalogVo == null) {
            throw new AutoexecCatalogNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecCatalogVo.getName());
        importExportVo.setDataWithObject(autoexecCatalogVo);
        return importExportVo;
    }
}
