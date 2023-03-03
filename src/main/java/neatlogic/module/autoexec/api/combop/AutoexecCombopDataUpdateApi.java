package neatlogic.module.autoexec.api.combop;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.annotation.Input;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
public class AutoexecCombopDataUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

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
        AutoexecCombopVo search = new AutoexecCombopVo();
        int rowNum = autoexecCombopMapper.getAutoexecCombopCount(search);
        if (rowNum == 0) {
            return null;
        }
        search.setRowNum(rowNum);
        for (int currentPage = 1; currentPage <= search.getPageCount(); currentPage++) {
            search.setCurrentPage(currentPage);
            List<AutoexecCombopVo> list = autoexecCombopMapper.getAutoexecCombopList(search);
            for (AutoexecCombopVo autoexecCombopVo : list) {
                List<AutoexecCombopVersionVo> versionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(autoexecCombopVo.getId());
                if (CollectionUtils.isNotEmpty(versionList)) {
                    continue;
                }
                List<AutoexecParamVo> paramList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(autoexecCombopVo.getId());
                AutoexecCombopVersionVo versionVo = new AutoexecCombopVersionVo();
                versionVo.setCombopId(autoexecCombopVo.getId());
                versionVo.setName(autoexecCombopVo.getName());
                versionVo.setVersion(1);
                versionVo.setIsActive(1);
                versionVo.setStatus("passed");
                versionVo.setLcu(UserContext.get().getUserUuid());
                AutoexecCombopVersionConfigVo configVo = new AutoexecCombopVersionConfigVo();
                AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
                configVo.setExecuteConfig(config.getExecuteConfig());
                configVo.setCombopGroupList(config.getCombopGroupList());
                configVo.setCombopPhaseList(config.getCombopPhaseList());
                configVo.setScenarioList(config.getScenarioList());
                configVo.setDefaultScenarioId(config.getDefaultScenarioId());
                configVo.setRuntimeParamList(paramList);
                versionVo.setConfig(configVo);
                autoexecCombopVersionMapper.insertAutoexecCombopVersion(versionVo);
            }
        }
        return null;
    }
}
