package neatlogic.module.autoexec.dao.mapper;

import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.common.dto.BasePageVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AutoexecCombopVersionMapper {

    Long checkAutoexecCombopVersionNameIsRepeat(AutoexecCombopVersionVo autoexecCombopVersionVo);

    AutoexecCombopVersionVo getAutoexecCombopVersionById(Long id);

    List<AutoexecCombopVersionVo> getAutoexecCombopVersionListByCombopId(Long id);

    List<AutoexecCombopVersionVo> getAutoexecCombopVersionList(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int getAutoexecCombopVersionCount(AutoexecCombopVersionVo autoexecCombopVersionVo);

    List<AutoexecParamVo> getAutoexecCombopVersionParamListByCombopVersionId(Long combopVersionId);

    Long getAutoexecCombopActiveVersionIdByCombopId(Long combopId);

    AutoexecCombopVersionVo getAutoexecCombopActiveVersionByCombopId(Long combopId);

    Integer getAutoexecCombopMaxVersionByCombopId(Long combopId);

    List<Long> getAutoexecCombopIdListByStatus(String status);

    Long getAutoexecCombopMaxVersionIdByCombopIdAndStatus(@Param("combopId") Long combopId, @Param("status") String versionStatus);

    int getAutoexecCombopVersionCountForUpdateConfig();

    List<Map<String, Object>> getAutoexecCombopVersionListForUpdateConfig(BasePageVo searchVo);

    int insertAutoexecCombopVersion(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int updateAutoexecCombopVersionById(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int updateAutoexecCombopVersionStatusById(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int disableAutoexecCombopVersionByCombopId(Long combopId);

    int enableAutoexecCombopVersionById(Long id);

    int deleteAutoexecCombopVersionByCombopId(Long id);

    int deleteAutoexecCombopVersionById(Long id);
}
