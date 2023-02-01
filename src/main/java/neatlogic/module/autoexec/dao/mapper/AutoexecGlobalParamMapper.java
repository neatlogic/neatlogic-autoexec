package neatlogic.module.autoexec.dao.mapper;


import neatlogic.framework.autoexec.crossover.IAutoexecGlobalParamCrossoverMapper;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/18 7:02 下午
 */
public interface AutoexecGlobalParamMapper extends IAutoexecGlobalParamCrossoverMapper {

    int checkGlobalParamIsExistsById(Long paramId);

    AutoexecGlobalParamVo getGlobalParamById(Long paramId);

    AutoexecGlobalParamVo getGlobalParamByKey(String key);

    List<AutoexecGlobalParamVo> getGlobalParamByKeyList(List<String> keyList);

    int getGlobalParamCount(AutoexecGlobalParamVo globalParamVo);

    List<Long> getGlobalParamIdList(AutoexecGlobalParamVo globalParamVo);

    List<AutoexecGlobalParamVo> getGlobalParamListByIdList(@Param("idList") List<Long> idList);

    List<AutoexecGlobalParamVo> searchGlobalParam(AutoexecGlobalParamVo globalParamVo);

    List<AutoexecGlobalParamVo> getAllPasswordGlobalParam();

    int checkGlobalParamKeyIsRepeat(AutoexecGlobalParamVo globalParamVo);

    int checkGlobalParamNameIsRepeat(AutoexecGlobalParamVo globalParamVo);

    void insertGlobalParam(AutoexecGlobalParamVo paramVo);

    void deleteGlobalParamById(Long paramId);

    int updateGlobalParamPasswordById(@Param("id") Long id, @Param("password") String password);
}
