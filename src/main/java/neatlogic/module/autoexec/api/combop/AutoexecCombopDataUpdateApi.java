package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.annotation.Input;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AutoexecCombopDataUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

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

    @Input({})
    @Output({})
    @Description(desc = "更新组合工具数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        int rowNum = autoexecCombopVersionMapper.getAutoexecCombopVersionCountForUpdateConfig();
        if (rowNum == 0) {
            return null;
        }
        BasePageVo searchVo = new BasePageVo();
//        searchVo.setPageSize(100);
        searchVo.setRowNum(rowNum);
        for (int currentPage = 1; currentPage <= searchVo.getPageCount(); currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<Map<String, Object>> list = autoexecCombopVersionMapper.getAutoexecCombopVersionListForUpdateConfig(searchVo);
            for (Map<String, Object> version : list) {
                System.out.println(version.get("id"));
                System.out.println(version.get("name"));
//                System.out.println(JSONObject.toJSONString(version));
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
                System.out.println(config);
                JSONObject executeConfig = config.getJSONObject("executeConfig");
                if (MapUtils.isNotEmpty(executeConfig)) {
                    Object executeUser = executeConfig.get("executeUser");
                    if (executeUser == null) {
                        ParamMappingVo paramMappingVo = new ParamMappingVo();
                        paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                        config.put("executeUser", paramMappingVo);
                    } else {
                        if (executeUser instanceof String) {
                            ParamMappingVo paramMappingVo = new ParamMappingVo();
                            paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                            paramMappingVo.setValue(executeUser);
                            config.put("executeUser", paramMappingVo);
                        }
                    }
                }
                JSONArray combopGroupArray = config.getJSONArray("combopGroupList");
            }
            break;
        }

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
        return null;
    }
}
