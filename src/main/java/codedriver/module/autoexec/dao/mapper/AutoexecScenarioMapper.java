package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/15 12:13 下午
 */
public interface AutoexecScenarioMapper {

    int checkScenarioIsExistsById(Long paramId);

    int getScenarioCount(AutoexecScenarioVo paramScenarioVo);

    AutoexecScenarioVo getScenarioById(Long paramId);

    AutoexecScenarioVo getScenarioByName(String scenarioName);

    List<Long> getScenarioIdList(AutoexecScenarioVo paramScenarioVo);

    List<AutoexecScenarioVo> getScenarioListByIdList(@Param("idList") List<Long> idList);

    void insertScenario(AutoexecScenarioVo paramScenarioVo);

    int checkScenarioNameIsRepeat(AutoexecScenarioVo paramScenarioVo);

    void deleteScenarioById(Long paramId);

    List<AutoexecScenarioVo> searchScenario(AutoexecScenarioVo paramScenarioVo);

}
