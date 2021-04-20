/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.CombopAuthorityAction;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: linbq
 * @since: 2021/4/15 16:13
 **/
@Service
public class AutoexecCombopServiceImpl implements AutoexecCombopService {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private TeamMapper teamMapper;

    /**
     * 设置当前用户可操作按钮权限列表
     *
     * @param autoexecCombopVo 组合工具Vo对象
     */
    @Override
    public void setOperableButtonList(AutoexecCombopVo autoexecCombopVo) {
        String userUuid = UserContext.get().getUserUuid(true);
        if (Objects.equals(autoexecCombopVo.getFcu(), userUuid)) {
            autoexecCombopVo.setEditable(1);
            autoexecCombopVo.setDeletable(1);
            if (Objects.equals(autoexecCombopVo.getIsActive(), 1)) {
                autoexecCombopVo.setExecutable(1);
            }
        } else {
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
            List<String> authorityList = autoexecCombopMapper.getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(autoexecCombopVo.getId(), userUuid, teamUuidList, roleUuidList);
            if (authorityList.contains(CombopAuthorityAction.EDIT.getValue())) {
                autoexecCombopVo.setEditable(1);
                autoexecCombopVo.setDeletable(1);
            } else {
                autoexecCombopVo.setEditable(0);
                autoexecCombopVo.setDeletable(0);
            }
            if (Objects.equals(autoexecCombopVo.getIsActive(), 1) && authorityList.contains(CombopAuthorityAction.EXECUTE.getValue())) {
                autoexecCombopVo.setExecutable(1);
            } else {
                autoexecCombopVo.setExecutable(0);
            }
        }
    }

    /**
     * 校验组合工具每个阶段是否配置正确
     * 校验规则
     * 1.每个阶段至少选择了一个工具
     * 2.引用上游出参或顶层参数，能找到来源（防止修改顶层参数或插件排序、或修改顶层参数带来的影响）
     *
     * @param autoexecCombopVo 组合工具Vo对象
     * @return
     */
    @Override
    public boolean verifyAutoexecCombopConfig(AutoexecCombopVo autoexecCombopVo) {
        JSONObject config = autoexecCombopVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }
        JSONArray combopPhaseList = config.getJSONArray("combopPhaseList");
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            throw new AutoexecCombopAtLeastOnePhaseException();
        }

        List<String> localPreNodeOutputParamList = new ArrayList<>();
        List<String> remotePreNodeOutputParamList = new ArrayList<>();
        List<String> topLevelParamList = new ArrayList<>();
        List<AutoexecCombopParamVo> autoexecCombopParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(autoexecCombopVo.getId());
        if(CollectionUtils.isNotEmpty(autoexecCombopParamVoList)){
            topLevelParamList = autoexecCombopParamVoList.stream().map(AutoexecCombopParamVo::getKey).collect(Collectors.toList());
        }
        for (int i = 0; i < combopPhaseList.size(); i++) {
            AutoexecCombopPhaseVo autoexecCombopPhaseVo = combopPhaseList.getObject(i, AutoexecCombopPhaseVo.class);
            if (autoexecCombopPhaseVo != null) {
                JSONObject phaseConfig = autoexecCombopPhaseVo.getConfig();
                if (MapUtils.isEmpty(phaseConfig)) {
                    throw new AutoexecCombopPhaseAtLeastOneOperationException();
                }
                JSONArray phaseOperationList = phaseConfig.getJSONArray("phaseOperationList");
                if (CollectionUtils.isEmpty(phaseOperationList)) {
                    throw new AutoexecCombopPhaseAtLeastOneOperationException();
                }
                String uuid = combopPhaseList.getJSONObject(i).getString("uuid");
                String execMode = autoexecCombopPhaseVo.getExecMode();
                for (int j = 0; j < phaseOperationList.size(); j++) {
                    AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo = phaseOperationList.getObject(j, AutoexecCombopPhaseOperationVo.class);
                    if (autoexecCombopPhaseOperationVo != null) {
                        List<String> inputParamList = new ArrayList<>();
                        Long operationId = autoexecCombopPhaseOperationVo.getOperationId();
                        if(Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())){
                            if(autoexecScriptMapper.checkScriptIsExistsById(operationId) == 0){
                                throw new AutoexecScriptNotFoundException(operationId);
                            }
                            List<AutoexecScriptVersionParamVo> autoexecScriptVersionParamVoList = autoexecScriptMapper.getParamListByScriptId(operationId);
                            for(AutoexecScriptVersionParamVo paramVo : autoexecScriptVersionParamVoList){
                                if(Objects.equals(paramVo.getType(), "input")){
                                    inputParamList.add(paramVo.getKey());
                                }else if(Objects.equals(paramVo.getType(), "output")){
                                    if(Objects.equals(ExecMode.LOCAL.getValue(), execMode)){
                                        localPreNodeOutputParamList.add(uuid + "." + operationId + "." + paramVo.getKey());
                                    }else if(Objects.equals(ExecMode.REMOTE.getValue(), execMode)){
                                        remotePreNodeOutputParamList.add(uuid + "." + operationId + "." + paramVo.getKey());
                                    }
                                }
                            }
                        }else{
                            //TODO linbq 工具暂时不实现
                        }
                        JSONObject operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                        if(MapUtils.isNotEmpty(operationConfig)){
                            JSONArray paramList = operationConfig.getJSONArray("paramList");
                            if(CollectionUtils.isNotEmpty(paramList)){
                                for(int k = 0; k < paramList.size(); k++){
                                    JSONObject paramObj = paramList.getJSONObject(i);
                                    if(MapUtils.isNotEmpty(paramObj)){
                                        String key = paramObj.getString("key");
                                        if(!inputParamList.contains(key)){
                                            throw new AutoexecParamNotFoundException(key);
                                        }
                                        String value = paramObj.getString("value");
                                        if(StringUtils.isEmpty(value)){
                                            throw new AutoexecParamMappingIncorrectException(key);
                                        }
                                        String mappingMode = paramObj.getString("mappingMode");
                                        if(Objects.equals(mappingMode, ParamMappingMode.TOP_LEVEL_PARAM)){
                                            if(!topLevelParamList.contains(value)){
                                                throw new AutoexecParamMappingIncorrectException(key);
                                            }
                                        }else if(Objects.equals(mappingMode, ParamMappingMode.PRE_NODE_OUTPUT_PARAM)){
                                            if(Objects.equals(ExecMode.LOCAL.getValue(), execMode)){
                                                if(!localPreNodeOutputParamList.contains(value)){
                                                    throw new AutoexecParamMappingIncorrectException(key);
                                                }
                                            }else if(Objects.equals(ExecMode.REMOTE.getValue(), execMode)){
                                                if(!localPreNodeOutputParamList.contains(value) && !remotePreNodeOutputParamList.contains(value)){
                                                    throw new AutoexecParamMappingIncorrectException(key);
                                                }
                                            }
                                        }else if(Objects.equals(mappingMode, ParamMappingMode.CONSTANT)) {

                                        }else {
                                            throw new AutoexecParamMappingIncorrectException(key);
                                        }
                                        inputParamList.remove(key);
                                    }
                                }
                                if(CollectionUtils.isNotEmpty(inputParamList)){
                                    throw new AutoexecParamMappingNotMappedException(String.join("、", inputParamList));
                                }
                            }else if(CollectionUtils.isNotEmpty(inputParamList)){
                                throw new AutoexecParamMappingNotMappedException(String.join("、", inputParamList));
                            }
                        }
                    }
                }
//                JSONArray phaseNodeList = phaseConfig.getJSONArray("phaseNodeList");
//                if (CollectionUtils.isNotEmpty(phaseNodeList)) {
//                }
            }
        }
//        JSONArray combopNodeList = config.getJSONArray("combopNodeList");
//        if (CollectionUtils.isNotEmpty(combopNodeList)) {
//        }
        return true;
    }
}
