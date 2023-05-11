/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamConfigVo;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dependency.MatrixAutoexecCombopParamDependencyHandler;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 保存组合工具运行参数接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
//@Service
@Deprecated
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopParamSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/combop/param/save";
    }

    @Override
    public String getName() {
        return "保存组合工具运行参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具主键id"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "参数列表[{\"key\": \"参数名\", \"name\": \"中文名\", \"defaultValue\": \"默认值\", \"description\": \"描述\", \"isRequired\": \"是否必填\", \"type\": \"参数类型\"}]")
    })
    @Description(desc = "保存组合工具运行参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (Objects.equals(autoexecCombopVo.getEditable(), 0)) {
            throw new PermissionDeniedException();
        }
//        List<AutoexecParamVo> autoexecParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
//        if (CollectionUtils.isNotEmpty(autoexecParamList)) {
////            autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(combopId);
//            DependencyManager.delete(MatrixAutoexecCombopParamDependencyHandler.class, combopId);
//        }

        List<AutoexecCombopParamVo> autoexecCombopParamVoList = new ArrayList<>();
        JSONArray paramList = jsonObj.getJSONArray("paramList");
        List<AutoexecCombopParamVo> runtimeParamList = paramList.toJavaList(AutoexecCombopParamVo.class);
        autoexecService.validateRuntimeParamList(runtimeParamList);
        for (int i = 0; i < runtimeParamList.size(); i++) {
            AutoexecCombopParamVo autoexecCombopParamVo = runtimeParamList.get(i);
            if (autoexecCombopParamVo != null) {
                String key = autoexecCombopParamVo.getKey();
                String type = autoexecCombopParamVo.getType();
                ParamType paramType = ParamType.getParamType(type);
                Object value = autoexecCombopParamVo.getDefaultValue();
                // 如果默认值不以"RC4:"开头，说明修改了密码，则重新加密
                if (paramType == ParamType.PASSWORD && value != null) {
                    autoexecCombopParamVo.setDefaultValue(RC4Util.encrypt((String) value));
                } else if (paramType == ParamType.SELECT || paramType == ParamType.MULTISELECT || paramType == ParamType.CHECKBOX || paramType == ParamType.RADIO) {
                    AutoexecParamConfigVo config = autoexecCombopParamVo.getConfig();
                    if (config != null) {
                        String matrixUuid = config.getMatrixUuid();
                        if (StringUtils.isNotBlank(matrixUuid)) {
                            JSONArray callers = new JSONArray();
                            callers.add(combopId);
                            callers.add(key);
                            DependencyManager.insert(MatrixAutoexecCombopParamDependencyHandler.class, matrixUuid, callers);
                        }
                    }
                }
                autoexecCombopParamVo.setCombopId(combopId);
                autoexecCombopParamVo.setSort(i);
                autoexecCombopParamVoList.add(autoexecCombopParamVo);
                if (autoexecCombopParamVoList.size() == 1000) {
//                    autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
                    autoexecCombopParamVoList.clear();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(autoexecCombopParamVoList)) {
//            autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
        }
        return null;
    }

}
