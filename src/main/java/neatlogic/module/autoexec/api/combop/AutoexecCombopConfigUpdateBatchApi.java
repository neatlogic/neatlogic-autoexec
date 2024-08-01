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

package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
                                /* System.out.println("insertAutoexecOperationGenerateCombop:" + id + "," + autoexecCombopVo.getOperationType() + "," + operationId);*/
                                autoexecCombopMapper.insertAutoexecOperationGenerateCombop(id, autoexecCombopVo.getOperationType(), operationId);
                            }
                        }
                    }
                    if (Objects.equals(oldConfigStr, newConfigStr)) {
                        continue;
                    }
                    //System.out.println(oldConfigStr);
                    //System.out.println(newConfigStr);
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
            getOperationData(phaseOperationList);
        }
    }

    /**
     * 补充工具 id/name
     * @param operationVos 工具列表
     */
    private void getOperationData(List<AutoexecCombopPhaseOperationVo> operationVos){
        if (CollectionUtils.isNotEmpty(operationVos)) {
            for (AutoexecCombopPhaseOperationVo operationVo : operationVos) {
                if (operationVo == null) {
                    continue;
                }
                //对于旧数据，id代表自定义工具id或工具id，operationId代表新增的唯一标识，现在就是要把两者互换
                Long id = operationVo.getId();
                Long operationId = operationVo.getOperationId();
                String operationType = operationVo.getOperationType();
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

                AutoexecCombopPhaseOperationConfigVo operationConfig = operationVo.getConfig();
                getOperationData(operationConfig.getIfList());
                getOperationData(operationConfig.getElseList());
                getOperationData(operationConfig.getOperations());
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
