package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVersionVo;

import java.util.List;

public interface AutoexecCombopVersionMapper {

    Long checkAutoexecCombopVersionNameIsRepeat(AutoexecCombopVersionVo autoexecCombopVersionVo);

    AutoexecCombopVersionVo getAutoexecCombopVersionById(Long id);

    List<AutoexecCombopVersionVo> getAutoexecCombopVersionListByCombopId(Long id);

    List<AutoexecCombopVersionVo> getAutoexecCombopVersionList(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int getAutoexecCombopVersionCount(AutoexecCombopVersionVo autoexecCombopVersionVo);

    List<AutoexecParamVo> getAutoexecCombopVersionParamListByCombopVersionId(Long combopVersionId);

    Long getAutoexecCombopActiveVersionIdByCombopId(Long combopId);

    Integer getAutoexecCombopMaxVersionByCombopId(Long combopId);

    int insertAutoexecCombopVersion(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int updateAutoexecCombopVersionById(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int updateAutoexecCombopVersionStatusById(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int disableAutoexecCombopVersionByCombopId(Long combopId);

    int enableAutoexecCombopVersionById(Long id);

    int deleteAutoexecCombopVersionByCombopId(Long id);

    int deleteAutoexecCombopVersionById(Long id);
}
