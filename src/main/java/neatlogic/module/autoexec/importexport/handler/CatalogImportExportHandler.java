/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
        AutoexecCatalogVo autoexecCatalogVo = autoexecCatalogMapper.getAutoexecCatalogById(id);
        if (autoexecCatalogVo == null) {
            throw new AutoexecCatalogNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecCatalogVo.getName());
        importExportVo.setDataWithObject(autoexecCatalogVo);
        return importExportVo;
    }
}
