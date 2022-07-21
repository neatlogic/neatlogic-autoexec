/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecCombopService;
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
@Service
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
                } else if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
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
                        } else if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
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
                        } else if (Objects.equals(operationType, CombopOperationType.SCRIPT.getValue())) {
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
        autoexecCombopService.deleteDependency(autoexecCombopVo);
        autoexecCombopMapper.updateAutoexecCombopConfigById(autoexecCombopVo);
        autoexecCombopService.saveDependency(autoexecCombopVo);
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
