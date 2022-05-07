package codedriver.module.autoexec.dao.mapper;


import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import org.apache.ibatis.annotations.Param;

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

    AutoexecProfileVo getProfileVoById(Long id);

    void insertAutoexecProfileOperation(@Param("profileId") Long profileId, @Param("operationIdList") List<Long> operationIdList, @Param("type") String type);

    void insertProfile(AutoexecProfileVo profileVo);

    void insertAutoexecProfileParam(List<AutoexecProfileParamVo> paramList);

    List<AutoexecProfileParamVo> getProfileParamListByProfileId(Long id);


    void deleteProfileById(Long id);

    void deleteProfileOperationByProfileId(Long id);

    void deleteProfileOperationByOperationId(Long id);

}
