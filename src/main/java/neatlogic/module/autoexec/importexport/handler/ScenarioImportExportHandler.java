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
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
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
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return autoexecScenarioMapper.getScenarioByName(importExportBaseInfoVo.getName()) != null;
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
            autoexecScenarioVo.setDescription(oldAutoexecScenarioVo.getDescription());
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
