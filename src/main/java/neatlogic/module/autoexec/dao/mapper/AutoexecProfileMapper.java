package neatlogic.module.autoexec.dao.mapper;


import neatlogic.framework.autoexec.crossover.IAutoexecProfileCrossoverMapper;
import neatlogic.framework.autoexec.dto.AutoexecOperationVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileOperationVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/16 11:42 上午
 */
public interface AutoexecProfileMapper extends IAutoexecProfileCrossoverMapper {

    int searchAutoexecProfileCount(AutoexecProfileVo profileVo);

    int checkProfileIsExists(Long id);

    int checkProfileNameIsRepeats(AutoexecProfileVo vo);

    List<AutoexecOperationVo> getAutoexecOperationVoByProfileId(Long id);

    List<AutoexecProfileVo> searchAutoexecProfile(AutoexecProfileVo paramProfileVo);

    List<AutoexecProfileParamVo> getProfileParamListByProfileId(Long id);

    List<AutoexecProfileParamVo> getAllProfileParamList();

    List<AutoexecProfileVo> getProfileInfoListByIdList(@Param("idList") List<Long> idList);

    List<AutoexecProfileVo> getProfileListInvokeParamListByIdList(@Param("idList") List<Long> idList);

    List<Long> getNeedDeleteProfileParamIdListByProfileIdAndLcd(@Param("profileId") Long profileId, @Param("updateTag") Long updateTag);

    AutoexecProfileVo getProfileVoById(Long id);

    AutoexecProfileVo getProfileVoByName(String name);

    List<AutoexecProfileOperationVo> getAutoexecProfileOperationListByProfileId(Long profileId);

    int updateProfileParamPassword(@Param("param") AutoexecProfileParamVo autoexecProfileParamVo,@Param("password") String newPassword);

    Long getProfileIdByProfileIdAndOperationId(@Param("profileId") Long profileId, @Param("operationId") Long operationId);

    void insertAutoexecProfileOperation(@Param("profileId") Long profileId, @Param("operationIdList") List<Long> operationIdList, @Param("type") String type, @Param("updateTag") Long updateTag);

    void insertProfile(AutoexecProfileVo profileVo);

    void insertAutoexecProfileParamList(@Param("paramList") List<AutoexecProfileParamVo> paramList, @Param("profileId") Long profileId, @Param("updateTag") Long updateTag);

    void deleteProfileById(Long id);

    void deleteProfileOperationByProfileId(Long id);

    void deleteProfileOperationByOperationId(Long id);

    void deleteProfileParamByProfileId(Long paramProfileId);

    void deleteProfileParamByIdList(@Param("idList") List<Long> idList);

    void deleteProfileOperationByProfileIdAndLcd(@Param("profileId") Long profileId, @Param("updateTag") Long updateTag);
}
