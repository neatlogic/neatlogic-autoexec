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
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileOperationVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.dependency.AutoexecGlobalParamProfileDependencyHandler;
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.module.autoexec.service.AutoexecProfileService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ProfileImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    private AutoexecProfileService autoexecProfileService;

    @Override
    public ImportExportHandlerType getType() {
        return AutoexecImportExportHandlerType.AUTOEXEC_PROFILE;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return autoexecProfileMapper.getProfileVoByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Long importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        JSONObject data = importExportVo.getData();
        AutoexecProfileVo autoexecProfileVo = data.toJavaObject(AutoexecProfileVo.class);
        AutoexecProfileVo oldAutoexecProfileVo = autoexecProfileMapper.getProfileVoByName(autoexecProfileVo.getName());
        if (oldAutoexecProfileVo != null) {
            List<AutoexecProfileParamVo> profileParamVoList = autoexecProfileMapper.getProfileParamListByProfileId(oldAutoexecProfileVo.getId());
            if (CollectionUtils.isNotEmpty(profileParamVoList)) {
                for (AutoexecProfileParamVo paramVo : profileParamVoList) {
                    //删除当前profile参数与全局参数的关系
                    DependencyManager.delete(AutoexecGlobalParamProfileDependencyHandler.class, paramVo.getId());
                }
                autoexecProfileMapper.deleteProfileParamByProfileId(oldAutoexecProfileVo.getId());
            }
            autoexecProfileMapper.deleteProfileOperationByProfileId(oldAutoexecProfileVo.getId());
            autoexecProfileVo.setId(oldAutoexecProfileVo.getId());
        } else {
            if (autoexecProfileMapper.getProfileVoById(autoexecProfileVo.getId()) != null) {
                autoexecProfileVo.setId(null);
            }
        }
        Long updateTag = System.currentTimeMillis();
        List<AutoexecOperationVo> autoexecOperationVoList = new ArrayList<>();
        List<AutoexecProfileOperationVo> autoexecProfileOperationList = autoexecProfileVo.getProfileOperationList();
        for (AutoexecProfileOperationVo autoexecProfileOperationVo : autoexecProfileOperationList) {
            autoexecProfileOperationVo.setProfileId(autoexecProfileVo.getId());
            if (Objects.equals(autoexecProfileOperationVo.getType(), ToolType.SCRIPT.getValue())) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, autoexecProfileOperationVo.getOperationId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    autoexecProfileOperationVo.setOperationId((Long) newPrimaryKey);
                }
            } else if (Objects.equals(autoexecProfileOperationVo.getType(), ToolType.TOOL.getValue())) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, autoexecProfileOperationVo.getOperationId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    autoexecProfileOperationVo.setOperationId((Long) newPrimaryKey);
                }
            }
            AutoexecOperationVo autoexecOperationVo = new AutoexecOperationVo();
            autoexecOperationVo.setId(autoexecProfileOperationVo.getOperationId());
            autoexecOperationVo.setType(autoexecProfileOperationVo.getType());
            autoexecOperationVoList.add(autoexecOperationVo);
        }
        autoexecProfileVo.setAutoexecOperationVoList(autoexecOperationVoList);
        List<AutoexecProfileParamVo> profileParamVoList = autoexecProfileVo.getProfileParamVoList();
        if (CollectionUtils.isNotEmpty(profileParamVoList)) {
            for (AutoexecProfileParamVo paramVo : profileParamVoList) {
                paramVo.setProfileId(autoexecProfileVo.getId());
                if (Objects.equals(paramVo.getOperationType(), ToolType.SCRIPT.getValue())) {
                    Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, paramVo.getOperationId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        paramVo.setOperationId((Long) newPrimaryKey);
                    }
                } else if (Objects.equals(paramVo.getOperationType(), ToolType.TOOL.getValue())) {
                    Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, paramVo.getOperationId(), primaryChangeList);
                    if (newPrimaryKey != null) {
                        paramVo.setOperationId((Long) newPrimaryKey);
                    }
                }
            }
        }
        autoexecProfileService.saveProfile(autoexecProfileVo);
        return autoexecProfileVo.getId();
    }

    @Override
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportVo> dependencyList) {
        Long id = (Long) primaryKey;
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        List<AutoexecProfileParamVo> profileParamList = autoexecProfileService.getProfileParamListById(id);
        profileVo.setProfileParamVoList(profileParamList);
        for (AutoexecProfileParamVo profileParamVo : profileParamList) {
            if (Objects.equals(profileParamVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, profileParamVo.getDefaultValue(), dependencyList);
            }
        }
        List<AutoexecProfileOperationVo> autoexecProfileOperationList = autoexecProfileMapper.getAutoexecProfileOperationListByProfileId(id);
        profileVo.setProfileOperationList(autoexecProfileOperationList);
        for (AutoexecProfileOperationVo autoexecProfileOperationVo : autoexecProfileOperationList) {
            if (Objects.equals(autoexecProfileOperationVo.getType(), ToolType.SCRIPT.getValue())) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, autoexecProfileOperationVo.getOperationId(), dependencyList);
            } else if (Objects.equals(autoexecProfileOperationVo.getType(), ToolType.TOOL.getValue())) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, autoexecProfileOperationVo.getOperationId(), dependencyList);
            }
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, profileVo.getName());
        importExportVo.setDataWithObject(profileVo);
        return importExportVo;
    }
}
