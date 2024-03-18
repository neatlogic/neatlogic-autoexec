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
import neatlogic.framework.autoexec.constvalue.AutoexecTypeAuthorityAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeAuthVo;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.exception.AutoexecTypeNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.service.AutoexecTypeService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Component
public class TypeImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;
    @Resource
    private AutoexecTypeService autoexecTypeService;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_TYPE;
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
        return autoexecTypeMapper.getTypeIdByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        AutoexecTypeVo oldAutoexecTypeVo = autoexecTypeMapper.getTypeByName(importExportVo.getName());
        if (oldAutoexecTypeVo == null) {
            throw new AutoexecTypeNotFoundException(importExportVo.getName());
        }
        return oldAutoexecTypeVo.getId();
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        AutoexecTypeVo autoexecTypeVo = importExportVo.getData().toJavaObject(AutoexecTypeVo.class);
        AutoexecTypeVo oldAutoexecTypeVo = autoexecTypeMapper.getTypeByName(autoexecTypeVo.getName());
        if (oldAutoexecTypeVo != null) {
            autoexecTypeVo.setId(oldAutoexecTypeVo.getId());
        } else {
            if (autoexecTypeMapper.getTypeById(autoexecTypeVo.getId()) != null) {
                autoexecTypeVo.setId(null);
            }
        }
        autoexecTypeService.saveAutoexecType(autoexecTypeVo);
        return autoexecTypeVo.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AutoexecTypeVo autoexecTypeVo = autoexecTypeMapper.getTypeById(id);
        if (autoexecTypeVo == null) {
            throw new AutoexecTypeNotFoundException(id);
        }
        List<AutoexecTypeAuthVo> addAuthVoList = autoexecTypeMapper.getAutoexecTypeAuthListByTypeIdAndAction(id, AutoexecTypeAuthorityAction.ADD.getValue());
        List<String> authList = addAuthVoList.stream().map(e -> e.getAuthType() + "#" + e.getAuthUuid()).collect(Collectors.toList());
        autoexecTypeVo.setAuthList(authList);
        List<AutoexecTypeAuthVo> reviewAuthVoList = autoexecTypeMapper.getAutoexecTypeAuthListByTypeIdAndAction(id, AutoexecTypeAuthorityAction.REVIEW.getValue());
        List<String> reviewAuthList = reviewAuthVoList.stream().map(e -> e.getAuthType() + "#" + e.getAuthUuid()).collect(Collectors.toList());
        autoexecTypeVo.setReviewAuthList(reviewAuthList);
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecTypeVo.getName());
        importExportVo.setDataWithObject(autoexecTypeVo);
        return importExportVo;
    }
}
