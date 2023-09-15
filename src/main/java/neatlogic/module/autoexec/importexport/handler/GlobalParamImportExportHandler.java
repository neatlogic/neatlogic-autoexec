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
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.exception.AutoexecGlobalParamIsNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class GlobalParamImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM;
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
        return autoexecGlobalParamMapper.getGlobalParamByKey((String) importExportBaseInfoVo.getPrimaryKey()) != null;
    }

    @Override
    public Object importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        // 导入前判断全局参数是否已经存在的标准是名称是否相同，如果存在则更新，如果不存在则新增。
        // 如果需要的数据，名称不存在但id已存在，则更新导入数据的id。
        JSONObject data = importExportVo.getData();
        AutoexecGlobalParamVo autoexecGlobalParamVo = data.toJavaObject(AutoexecGlobalParamVo.class);
        AutoexecGlobalParamVo oldAutoexecGlobalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey(autoexecGlobalParamVo.getKey());
        if (oldAutoexecGlobalParamVo != null) {
            autoexecGlobalParamVo.setId(oldAutoexecGlobalParamVo.getId());
            autoexecGlobalParamVo.setDescription(oldAutoexecGlobalParamVo.getDescription());
            autoexecGlobalParamVo.setName(oldAutoexecGlobalParamVo.getName());
            autoexecGlobalParamVo.setType(oldAutoexecGlobalParamVo.getType());
            autoexecGlobalParamVo.setDefaultValue(oldAutoexecGlobalParamVo.getDefaultValue());
        } else {
            if (autoexecGlobalParamMapper.checkGlobalParamIsExistsById(autoexecGlobalParamVo.getId()) > 0) {
                // 更新id
                autoexecGlobalParamVo.setId(null);
            }
        }
        autoexecGlobalParamVo.setFcu(UserContext.get().getUserUuid());
        autoexecGlobalParamVo.setLcu(UserContext.get().getUserUuid());
        autoexecGlobalParamMapper.insertGlobalParam(autoexecGlobalParamVo);
        return autoexecGlobalParamVo.getKey();
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        if (primaryKey instanceof Long) {
            Long id = (Long) primaryKey;
            AutoexecGlobalParamVo autoexecGlobalParamVo = autoexecGlobalParamMapper.getGlobalParamById(id);
            if (autoexecGlobalParamVo == null) {
                throw new AutoexecGlobalParamIsNotFoundException(id);
            }
            ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, autoexecGlobalParamVo.getName());
            importExportVo.setDataWithObject(autoexecGlobalParamVo);
            return importExportVo;
        } else if (primaryKey instanceof String) {
            String key = (String) primaryKey;
            AutoexecGlobalParamVo autoexecGlobalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey(key);
            if (autoexecGlobalParamVo == null) {
                throw new AutoexecGlobalParamIsNotFoundException(key);
            }
            ImportExportVo importExportVo = new ImportExportVo(this.getType().getText(), primaryKey, autoexecGlobalParamVo.getName());
            importExportVo.setDataWithObject(autoexecGlobalParamVo);
            return importExportVo;
        }
        return null;
    }
}
