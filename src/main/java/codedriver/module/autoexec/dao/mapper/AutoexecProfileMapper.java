package codedriver.module.autoexec.dao.mapper;


import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamValueVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author longrf
 * @date 2022/3/16 11:42 上午
 */
public interface AutoexecProfileMapper {

    int searchAutoexecProfileCount(AutoexecProfileVo profileVo);

    int checkProfileIsExists(Long id);

    int checkProfileNameIsRepeats(AutoexecProfileVo vo);

    List<AutoexecOperationVo> getAutoexecOperationVoByProfileId(Long id);

    List<AutoexecProfileVo> searchAutoexecProfile(AutoexecProfileVo paramProfileVo);

    List<AutoexecProfileParamVo> getProfileParamListByProfileId(Long id);

    List<AutoexecProfileVo> getProfileInfoListByIdList(@Param("idList") List<Long> idList);

    List<Long> getNeedDeleteProfileParamIdListByProfileIdAndLcd(@Param("profileId") Long profileId, @Param("lcd") Date lcd);

    List<Long> getNeedDeleteProfileParamValueIdListByParamIdAndLcd(@Param("paramId") Long paramId, @Param("lcd") Date lcd);

    AutoexecProfileVo getProfileVoById(Long id);

    void insertAutoexecProfileOperation(@Param("profileId") Long profileId, @Param("operationIdList") List<Long> operationIdList, @Param("type") String type, @Param("lcd") Date lcd);

    void insertProfile(AutoexecProfileVo profileVo);

    void insertAutoexecProfileParamList(@Param("paramList") List<AutoexecProfileParamVo> paramList, @Param("profileId") Long profileId, @Param("lcd") Date lcd);

    void insertProfileParamValueInvokeList(@Param("paramValueInvokeVoList") List<AutoexecProfileParamValueVo> paramValueInvokeVoList, @Param("lcd") Date lcd);

    void deleteProfileById(Long id);

    void deleteProfileOperationByProfileId(Long id);

    void deleteProfileOperationByOperationId(Long id);

    void deleteProfileParamValueInvokeByProfileId(Long paramProfileId);

    void deleteProfileParamByProfileId(Long paramProfileId);

    void deleteProfileParamByIdList(@Param("idList") List<Long> idList);

    void deleteProfileParamValueInvokeByValueIdList(@Param("idList") List<Long> idList);

    void deleteProfileOperationByProfileIdAndLcd(@Param("profileId") Long profileId, @Param("lcd") Date lcd);
}
