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
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.annotation.Prop;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

//@Service
@Deprecated
@OperationType(type = OperationTypeEnum.UPDATE)
@AuthAction(action = AUTOEXEC_MODIFY.class)
@Transactional
public class AutoexecCombopDataUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/data/update";
    }

    @Override
    public String getName() {
        return "更新组合工具数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Prop({})
    @Output({})
    @Description(desc = "更新组合工具数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        int count = autoexecCombopMapper.getAutoexecCombopCountForUpdateConfig();
        if (count > 0) {
            BasePageVo searchVo = new BasePageVo();
            searchVo.setRowNum(count);
            for (int currentPage = 1; currentPage <= searchVo.getPageCount(); currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<Map<String, Object>> list = autoexecCombopMapper.getAutoexecCombopListForUpdateConfig(searchVo);
                for (Map<String, Object> combop : list) {
                    Long id = (Long) combop.get("id");
                    String configStr = (String) combop.get("config");
                    if (StringUtils.isBlank(configStr)) {
                        continue;
                    }
                    JSONObject config = null;
                    try {
                        config = JSONObject.parseObject(configStr);
                    } catch (JSONException e) {
                        //System.out.println("格式不对");
                    }
                    if (MapUtils.isEmpty(config)) {
                        continue;
                    }
                    boolean flag = false;
                    if (config.containsKey("executeConfig")) {
                        config.remove("executeConfig");
                        flag = true;
                    }
                    if (config.containsKey("combopGroupList")) {
                        config.remove("combopGroupList");
                        flag = true;
                    }
                    if (config.containsKey("combopPhaseList")) {
                        config.remove("combopPhaseList");
                        flag = true;
                    }
                    if (config.containsKey("runtimeParamList")) {
                        config.remove("runtimeParamList");
                        flag = true;
                    }
                    if (config.containsKey("defaultScenarioId")) {
                        config.remove("defaultScenarioId");
                        flag = true;
                    }
                    if (config.containsKey("scenarioList")) {
                        config.remove("scenarioList");
                        flag = true;
                    }
                    if (flag) {
                        System.out.println(id);
                        System.out.println(combop.get("name"));
                        System.out.println(config.toJSONString());
                        autoexecCombopMapper.updateAutoexecCombopConfigByIdAndConfigStr(id, config.toJSONString());
                    }
                }
            }
        }
        int rowNum = autoexecCombopVersionMapper.getAutoexecCombopVersionCountForUpdateConfig();
        resultObj.put("总数", rowNum);
        if (rowNum == 0) {
            return resultObj;
        }
        int needUpdateCount = 0;
        int updatedCount = 0;
        BasePageVo searchVo = new BasePageVo();
        searchVo.setPageSize(100);
        searchVo.setRowNum(rowNum);
        for (int currentPage = 1; currentPage <= searchVo.getPageCount(); currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<Map<String, Object>> list = autoexecCombopVersionMapper.getAutoexecCombopVersionListForUpdateConfig(searchVo);
            for (Map<String, Object> version : list) {
                Long id = (Long) version.get("id");
                String configStr = (String) version.get("config");
                if (StringUtils.isBlank(configStr)) {
                    continue;
                }
                JSONObject config = null;
                try {
                    config = JSONObject.parseObject(configStr);
                } catch (JSONException e) {
                    System.out.println("格式不对");
                }
                if (MapUtils.isEmpty(config)) {
                    continue;
                }
                try {
                    AutoexecCombopVersionConfigVo configVo = config.toJavaObject(AutoexecCombopVersionConfigVo.class);
                    continue;
                } catch (JSONException e) {
                }
                needUpdateCount++;
//                if (Objects.equals(id, 818075860795538L)) {
//                    System.out.println("");
//                }
                boolean flag = false;
                {
                    JSONObject executeConfig = config.getJSONObject("executeConfig");
                    if (updateExecuteConfig(executeConfig)) {
                        flag = true;
                    }
                }
                JSONArray combopGroupList = config.getJSONArray("combopGroupList");
                if (CollectionUtils.isNotEmpty(combopGroupList)) {
                    for (int i = 0; i < combopGroupList.size(); i++) {
                        JSONObject group = combopGroupList.getJSONObject(i);
                        JSONObject groupConfig = group.getJSONObject("config");
                        if (MapUtils.isEmpty(groupConfig)) {
                            continue;
                        }
                        JSONObject executeConfig = groupConfig.getJSONObject("executeConfig");
                        if (updateExecuteConfig(executeConfig)) {
                            flag = true;
                        }
                    }
                }
                JSONArray combopPhaseList = config.getJSONArray("combopPhaseList");
                if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                    for (int i = 0; i < combopPhaseList.size(); i++) {
                        JSONObject phase = combopPhaseList.getJSONObject(i);
                        JSONObject phaseConfig = phase.getJSONObject("config");
                        if (MapUtils.isEmpty(phaseConfig)) {
                            continue;
                        }
                        JSONObject executeConfig = phaseConfig.getJSONObject("executeConfig");
                        if (updateExecuteConfig(executeConfig)) {
                            flag = true;
                        }
                    }
                }
                if (flag) {
                    try {
                        AutoexecCombopVersionConfigVo configVo = config.toJavaObject(AutoexecCombopVersionConfigVo.class);
                        autoexecCombopVersionMapper.updateAutoexecCombopVersionConfigById(id, config.toJSONString());
                        updatedCount++;
                    } catch (JSONException e) {
                    }
                }
            }
//            break;
        }
        resultObj.put("已更新个数", updatedCount);
        resultObj.put("需要更新个数", needUpdateCount);
//        AutoexecCombopVo search = new AutoexecCombopVo();
//        int rowNum = autoexecCombopMapper.getAutoexecCombopCount(search);
//        if (rowNum == 0) {
//            return null;
//        }
//        search.setRowNum(rowNum);
//        for (int currentPage = 1; currentPage <= search.getPageCount(); currentPage++) {
//            search.setCurrentPage(currentPage);
//            List<AutoexecCombopVo> list = autoexecCombopMapper.getAutoexecCombopList(search);
//            for (AutoexecCombopVo autoexecCombopVo : list) {
//                List<AutoexecCombopVersionVo> versionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(autoexecCombopVo.getId());
//                if (CollectionUtils.isNotEmpty(versionList)) {
//                    continue;
//                }
//                List<AutoexecParamVo> paramList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(autoexecCombopVo.getId());
//                AutoexecCombopVersionVo versionVo = new AutoexecCombopVersionVo();
//                versionVo.setCombopId(autoexecCombopVo.getId());
//                versionVo.setName(autoexecCombopVo.getName());
//                versionVo.setVersion(1);
//                versionVo.setIsActive(1);
//                versionVo.setStatus("passed");
//                versionVo.setLcu(UserContext.get().getUserUuid());
//                AutoexecCombopVersionConfigVo configVo = new AutoexecCombopVersionConfigVo();
//                AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
//                configVo.setExecuteConfig(config.getExecuteConfig());
//                configVo.setCombopGroupList(config.getCombopGroupList());
//                configVo.setCombopPhaseList(config.getCombopPhaseList());
//                configVo.setScenarioList(config.getScenarioList());
//                configVo.setDefaultScenarioId(config.getDefaultScenarioId());
//                configVo.setRuntimeParamList(paramList);
//                versionVo.setConfig(configVo);
//                autoexecCombopVersionMapper.insertAutoexecCombopVersion(versionVo);
//            }
//        }
        return resultObj;
    }

    /**
     * 更新执行目标配置executeConfig
     * @param executeConfig
     * @return 返回标识更新前后是否发生变化
     */
    private boolean updateExecuteConfig(JSONObject executeConfig) {
        if (MapUtils.isNotEmpty(executeConfig)) {
            Object executeUser = executeConfig.get("executeUser");
            if (executeUser == null) {
                ParamMappingVo paramMappingVo = new ParamMappingVo();
                paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                paramMappingVo.setValue("");
                executeConfig.put("executeUser", paramMappingVo);
                return true;
            } else {
                if (executeUser instanceof String) {
                    ParamMappingVo paramMappingVo = new ParamMappingVo();
                    paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                    paramMappingVo.setValue(executeUser);
                    executeConfig.put("executeUser", paramMappingVo);
                    return true;
                }
            }
        }
        return false;
    }
}
