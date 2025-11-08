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

import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
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
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        AutoexecCatalogVo oldAutoexecCatalogVo = autoexecCatalogMapper.getAutoexecCatalogByName(importExportVo.getName());
        if (oldAutoexecCatalogVo == null) {
            throw new AutoexecCatalogNotFoundException(importExportVo.getName());
        }
        return oldAutoexecCatalogVo.getId();
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
        if (id == 0L) {
            return null;
        }
        AutoexecCatalogVo autoexecCatalogVo = autoexecCatalogMapper.getAutoexecCatalogById(id);
        if (autoexecCatalogVo == null) {
            throw new AutoexecCatalogNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecCatalogVo.getName());
        importExportVo.setDataWithObject(autoexecCatalogVo);
        return importExportVo;
    }
}
