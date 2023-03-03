/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author linbq
 * @since 2022/3/23 15:47
 **/
@Deprecated
//@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class AutoexecCombopConfigUpdateBatchApi extends PrivateApiComponentBase {

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getName() {
        return "批量更新组合工具配置信息";
    }

    @Override
    public String getToken() {
        return "autoexec/combop/config/update/batch";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表")
    })
    @Output({})
    @Description(desc = "批量更新组合工具配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<Long> combopIdList = new ArrayList<>();
        JSONArray idArray = paramObj.getJSONArray("idList");
        if (CollectionUtils.isNotEmpty(idArray)) {
            combopIdList = idArray.toJavaList(Long.class);
        }
        AutoexecCombopVo searchVo = new AutoexecCombopVo();
        int rowNum = autoexecCombopMapper.getAutoexecCombopCount(searchVo);
        if (rowNum > 0) {
            searchVo.setPageSize(100);
            searchVo.setRowNum(rowNum);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<Long> idList = autoexecCombopMapper.getAutoexecCombopIdList(searchVo);
                for (Long id : idList) {
                    if (CollectionUtils.isNotEmpty(combopIdList) && !combopIdList.contains(id)) {
                        continue;
                    }
                    AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
                    AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
                    String oldConfigStr = JSONObject.toJSONString(config);
                    updateConfig(config);
                    String newConfigStr = JSONObject.toJSONString(config);
                    if (!Objects.equals(CombopOperationType.COMBOP.getValue(), autoexecCombopVo.getOperationType())) {
                        Long operationId = getOperationId(config);
                        if (operationId != null) {
                            Long combopId = autoexecCombopMapper.checkItHasBeenGeneratedToCombopByOperationId(operationId);
                            if (combopId == null || !Objects.equals(combopId, id)) {
                                System.out.println("insertAutoexecOperationGenerateCombop:" + id + "," + autoexecCombopVo.getOperationType() + "," + operationId);
                                autoexecCombopMapper.insertAutoexecOperationGenerateCombop(id, autoexecCombopVo.getOperationType(), operationId);
                            }
                        }
                    }
                    if (Objects.equals(oldConfigStr, newConfigStr)) {
                        continue;
                    }
                    System.out.println(oldConfigStr);
                    System.out.println(newConfigStr);
                    autoexecCombopVo.setConfig(config);
                    updateDBdata(autoexecCombopVo);
                }
            }
        }
        return null;
    }

    private void updateConfig(AutoexecCombopConfigVo config) {
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }

        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo == null) {
                continue;
            }
            AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
            if (autoexecCombopPhaseConfigVo == null) {
                continue;
            }
            List<AutoexecCombopPhaseOperationVo> phaseOperationList = autoexecCombopPhaseConfigVo.getPhaseOperationList();
            if (CollectionUtils.isEmpty(phaseOperationList)) {
                continue;
            }
            for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                if (phaseOperationVo == null) {
                    continue;
                }
                //对于旧数据，id代表自定义工具id或工具id，operationId代表新增的唯一标识，现在就是要把两者互换
                Long id = phaseOperationVo.getId();
                Long operationId = phaseOperationVo.getOperationId();
                String operationType = phaseOperationVo.getOperationType();
                if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
                    AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
                    if (autoexecScriptVo != null) {
                        if (Objects.equals(id, operationId)) {
                            phaseOperationVo.setId(null);
                        } else {
                            phaseOperationVo.setId(operationId);
                        }
                        phaseOperationVo.setOperationId(autoexecScriptVo.getId());
                        phaseOperationVo.setOperationName(autoexecScriptVo.getName());
                    }
                } else if (Objects.equals(operationType, CombopOperationType.TOOL.getValue())) {
                    AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(id);
                    if (autoexecToolVo != null) {
                        if (Objects.equals(id, operationId)) {
                            phaseOperationVo.setId(null);
                        } else {
                            phaseOperationVo.setId(operationId);
                        }
                        phaseOperationVo.setOperationId(autoexecToolVo.getId());
                        phaseOperationVo.setOperationName(autoexecToolVo.getName());
                    }
                }
                AutoexecCombopPhaseOperationConfigVo operationConfig = phaseOperationVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                if (CollectionUtils.isNotEmpty(ifList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                        if (operationVo == null) {
                            continue;
                        }
                        //对于旧数据，id代表自定义工具id或工具id，operationId代表新增的唯一标识，现在就是要把两者互换
                        id = operationVo.getId();
                        operationId = operationVo.getOperationId();
                        operationType = operationVo.getOperationType();
                        if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
                            AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
                            if (autoexecScriptVo != null) {
                                if (Objects.equals(id, operationId)) {
                                    operationVo.setId(null);
                                } else {
                                    operationVo.setId(operationId);
                                }
                                operationVo.setOperationId(autoexecScriptVo.getId());
                                operationVo.setOperationName(autoexecScriptVo.getName());
                            }
                        } else if (Objects.equals(operationType, CombopOperationType.TOOL.getValue())) {
                            AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(id);
                            if (autoexecToolVo != null) {
                                if (Objects.equals(id, operationId)) {
                                    operationVo.setId(null);
                                } else {
                                    operationVo.setId(operationId);
                                }
                                operationVo.setOperationId(autoexecToolVo.getId());
                                operationVo.setOperationName(autoexecToolVo.getName());
                            }
                        }
                    }
                }
                List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                if (CollectionUtils.isNotEmpty(elseList)) {
                    for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                        if (operationVo == null) {
                            continue;
                        }
                        //对于旧数据，id代表自定义工具id或工具id，operationId代表新增的唯一标识，现在就是要把两者互换
                        id = operationVo.getId();
                        operationId = operationVo.getOperationId();
                        operationType = operationVo.getOperationType();
                        if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
                            AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
                            if (autoexecScriptVo != null) {
                                if (Objects.equals(id, operationId)) {
                                    operationVo.setId(null);
                                } else {
                                    operationVo.setId(operationId);
                                }
                                operationVo.setOperationId(autoexecScriptVo.getId());
                                operationVo.setOperationName(autoexecScriptVo.getName());
                            }
                        } else if (Objects.equals(operationType, CombopOperationType.TOOL.getValue())) {
                            AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(id);
                            if (autoexecToolVo != null) {
                                if (Objects.equals(id, operationId)) {
                                    operationVo.setId(null);
                                } else {
                                    operationVo.setId(operationId);
                                }
                                operationVo.setOperationId(autoexecToolVo.getId());
                                operationVo.setOperationName(autoexecToolVo.getName());
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateDBdata(AutoexecCombopVo autoexecCombopVo) {
//        autoexecCombopService.deleteDependency(autoexecCombopVo);
        autoexecCombopMapper.updateAutoexecCombopConfigById(autoexecCombopVo);
//        autoexecCombopService.saveDependency(autoexecCombopVo);
    }

    private Long getOperationId(AutoexecCombopConfigVo config) {
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return null;
        }
        if (combopPhaseList.size() > 1) {
            return null;
        }
        AutoexecCombopPhaseVo autoexecCombopPhaseVo = combopPhaseList.get(0);
        if (autoexecCombopPhaseVo == null) {
            return null;
        }
        AutoexecCombopPhaseConfigVo autoexecCombopPhaseConfigVo = autoexecCombopPhaseVo.getConfig();
        if (autoexecCombopPhaseConfigVo == null) {
            return null;
        }
        List<AutoexecCombopPhaseOperationVo> phaseOperationList = autoexecCombopPhaseConfigVo.getPhaseOperationList();
        if (CollectionUtils.isEmpty(phaseOperationList)) {
            return null;
        }
        if (phaseOperationList.size() > 1) {
            return null;
        }
        AutoexecCombopPhaseOperationVo phaseOperationVo = phaseOperationList.get(0);
        if (phaseOperationVo == null) {
            return null;
        }
        return phaseOperationVo.getOperationId();
    }
}
