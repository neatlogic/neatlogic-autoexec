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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

@Component
public class ScenarioImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecScenarioMapper autoexecScenarioMapper;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_SCENARIO;
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
        return autoexecScenarioMapper.getScenarioByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        AutoexecScenarioVo oldAutoexecScenarioVo = autoexecScenarioMapper.getScenarioByName(importExportVo.getName());
        if (oldAutoexecScenarioVo == null) {
            throw new AutoexecScenarioIsNotFoundException(importExportVo.getName());
        }
        return oldAutoexecScenarioVo.getId();
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        // 导入前判断场景是否已经存在的标准是名称是否相同，如果存在则更新，如果不存在则新增。
        // 如果需要的数据，名称不存在但id已存在，则更新导入数据的id。
        JSONObject data = importExportVo.getData();
        AutoexecScenarioVo autoexecScenarioVo = data.toJavaObject(AutoexecScenarioVo.class);
        AutoexecScenarioVo oldAutoexecScenarioVo = autoexecScenarioMapper.getScenarioByName(autoexecScenarioVo.getName());
        if (oldAutoexecScenarioVo != null) {
            autoexecScenarioVo.setId(oldAutoexecScenarioVo.getId());
            if (Objects.equals(autoexecScenarioVo.getDescription(), oldAutoexecScenarioVo.getDescription())) {
                return autoexecScenarioVo.getId();
            }
        } else {
            if (autoexecScenarioMapper.checkScenarioIsExistsById(autoexecScenarioVo.getId()) > 0) {
                // 更新id
                autoexecScenarioVo.setId(null);
            }
        }
        autoexecScenarioVo.setFcu(UserContext.get().getUserUuid());
        autoexecScenarioVo.setLcu(UserContext.get().getUserUuid());
        autoexecScenarioMapper.insertScenario(autoexecScenarioVo);
        return autoexecScenarioVo.getId();
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AutoexecScenarioVo autoexecScenario = autoexecScenarioMapper.getScenarioById(id);
        if (autoexecScenario == null) {
            throw new AutoexecScenarioIsNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecScenario.getName());
        importExportVo.setDataWithObject(autoexecScenario);
        return importExportVo;
    }
}
