package neatlogic.module.autoexec.dao.mapper;

import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AutoexecCombopVersionMapper {

    Long checkAutoexecCombopVersionNameIsRepeat(AutoexecCombopVersionVo autoexecCombopVersionVo);

    AutoexecCombopVersionVo getAutoexecCombopVersionById(Long id);

    List<AutoexecCombopVersionVo> getAutoexecCombopVersionListByCombopId(Long id);

    List<AutoexecCombopVersionVo> getAutoexecCombopVersionListByIdList(List<Long> idList);

    List<AutoexecCombopVersionVo> getAutoexecCombopVersionList(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int getAutoexecCombopVersionCount(AutoexecCombopVersionVo autoexecCombopVersionVo);

    List<AutoexecParamVo> getAutoexecCombopVersionParamListByCombopVersionId(Long combopVersionId);

    Long getAutoexecCombopActiveVersionIdByCombopId(Long combopId);

    AutoexecCombopVersionVo getAutoexecCombopActiveVersionByCombopId(Long combopId);

    Integer getAutoexecCombopMaxVersionByCombopId(Long combopId);

    Long getAutoexecCombopMaxVersionIdByCombopIdAndStatus(@Param("combopId") Long combopId, @Param("status") String versionStatus);

    int getAutoexecCombopVersionCountForUpdateConfig();

    List<Map<String, Object>> getAutoexecCombopVersionListForUpdateConfig(BasePageVo searchVo);

    int insertAutoexecCombopVersion(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int updateAutoexecCombopVersionById(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int updateAutoexecCombopVersionStatusById(AutoexecCombopVersionVo autoexecCombopVersionVo);

    int updateAutoexecCombopVersionConfigById(@Param("id") Long id, @Param("configStr") String configStr);

    int disableAutoexecCombopVersionByCombopId(Long combopId);

    int enableAutoexecCombopVersionById(Long id);

    int deleteAutoexecCombopVersionByCombopId(Long id);

    int deleteAutoexecCombopVersionById(Long id);

    /** 统计当前执行权限范围内符合查询条件的激活版本。 */
    int countExecutableVersions(
            @Param("auth") AuthenticationInfoVo auth,
            @Param("all") boolean all,
            @Param("keyword") String keyword,
            @Param("ids") List<Long> ids);

    /** 有界游标批量读取摘要与配置，授权多行不会重复返回工具。 */
    List<AutoexecCombopVersionVo> searchExecutableVersions(
            @Param("auth") AuthenticationInfoVo auth,
            @Param("all") boolean all,
            @Param("keyword") String keyword,
            @Param("ids") List<Long> ids,
            @Param("beforeId") Long beforeId,
            @Param("size") int size);
}
