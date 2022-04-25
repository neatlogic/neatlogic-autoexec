package codedriver.module.autoexec.dao.mapper;


import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileOperationVo;
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

    List<Long> getAutoexecProfileIdList(AutoexecProfileVo profileVo);

    List<AutoexecProfileVo> getProfileListByIdList(@Param("idList") List<Long> idList);

    List<AutoexecProfileOperationVo> getProfileOperationVoListByProfileId(Long id);

    AutoexecProfileVo getProfileVoById(Long id);

    void insertAutoexecProfileOperation(@Param("profileId") Long profileId, @Param("operationIdList") List<Long> operationIdList, @Param("type") String type);

    void insertProfile(AutoexecProfileVo profileVo);

    void updateProfile(AutoexecProfileVo profileVo);

    void deleteProfileById(Long id);

    void deleteProfileOperationByProfileId(Long id);

    void deleteProfileOperationByOperationId(Long id);

    List<AutoexecOperationVo> getAutoexecOperationVoByProfileId(Long id);

}
