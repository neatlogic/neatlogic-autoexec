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
import neatlogic.framework.autoexec.constvalue.AutoexecImportExportHandlerType;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.constvalue.ToolType;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
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
import neatlogic.module.autoexec.service.AutoexecProfileService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

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
    public boolean checkImportAuth(ImportExportVo importExportVo) {
        return true;
    }

    @Override
    public boolean checkExportAuth(Object primaryKey) {
        return true;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return autoexecProfileMapper.getProfileVoByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        AutoexecProfileVo oldAutoexecProfileVo = autoexecProfileMapper.getProfileVoByName(importExportVo.getName());
        if (oldAutoexecProfileVo == null) {
            throw new AutoexecProfileIsNotFoundException(importExportVo.getName());
        }
        return oldAutoexecProfileVo.getId();
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
        for (AutoexecOperationVo autoexecOperationVo : autoexecProfileVo.getAutoexecOperationVoList()) {
            if (Objects.equals(autoexecOperationVo.getType(), ToolType.SCRIPT.getValue())) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, autoexecOperationVo.getId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    autoexecOperationVo.setId((Long) newPrimaryKey);
                }
            } else if (Objects.equals(autoexecOperationVo.getType(), ToolType.TOOL.getValue())) {
                Object newPrimaryKey = getNewPrimaryKey(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, autoexecOperationVo.getId(), primaryChangeList);
                if (newPrimaryKey != null) {
                    autoexecOperationVo.setId((Long) newPrimaryKey);
                }
            }
        }
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
    public ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        List<AutoexecProfileParamVo> profileParamList = autoexecProfileService.getProfileParamListById(id);
        profileVo.setProfileParamVoList(profileParamList);
        for (AutoexecProfileParamVo profileParamVo : profileParamList) {
            if (Objects.equals(profileParamVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                doExportData(AutoexecImportExportHandlerType.AUTOEXEC_GLOBAL_PARAM, profileParamVo.getDefaultValue(), dependencyList, zipOutputStream);
            }
        }
        List<AutoexecOperationVo> autoexecOperationVoList =  profileVo.getAutoexecOperationVoList();
        if (CollectionUtils.isNotEmpty(autoexecOperationVoList)) {
            for (AutoexecOperationVo autoexecOperationVo : autoexecOperationVoList) {
                if (Objects.equals(autoexecOperationVo.getType(), ToolType.SCRIPT.getValue())) {
                    doExportData(AutoexecImportExportHandlerType.AUTOEXEC_SCRIPT, autoexecOperationVo.getId(), dependencyList, zipOutputStream);
                } else if (Objects.equals(autoexecOperationVo.getType(), ToolType.TOOL.getValue())) {
                    doExportData(AutoexecImportExportHandlerType.AUTOEXEC_TOOL, autoexecOperationVo.getId(), dependencyList, zipOutputStream);
                }
            }
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, profileVo.getName());
        importExportVo.setDataWithObject(profileVo);
        return importExportVo;
    }
}
