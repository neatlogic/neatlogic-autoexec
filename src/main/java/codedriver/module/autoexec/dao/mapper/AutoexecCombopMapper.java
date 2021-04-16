/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.combop.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/13 11:05
 **/
public interface AutoexecCombopMapper {
    public int checkAutoexecCombopIsExists(Long id);

    public Long checkAutoexecCombopNameIsRepeat(AutoexecCombopVo autoexecCombopVo);

    public Long checkAutoexecCombopUkIsRepeat(AutoexecCombopVo autoexecCombopVo);

    public Integer getAutoexecCombopIsActiveByIdForUpdate(Long id);

    public AutoexecCombopVo getAutoexecCombopById(Long id);

    public int getAutoexecCombopCount(AutoexecCombopVo searchVo);

    public List<AutoexecCombopVo> getAutoexecCombopList(AutoexecCombopVo searchVo);

    public List<AutoexecCombopAuthorityVo> getAutoexecCombopAuthorityListByCombopIdAndAction(@Param("combopId") Long combopId, @Param("action") String action);

    public int getAutoexecCombopParamCountByCombopId(AutoexecCombopParamVo searchVo);

    public List<AutoexecCombopParamVo> getAutoexecCombopParamListByCombopId(AutoexecCombopParamVo searchVo);

    public List<String> getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(
            @Param("combopId") Long combopId,
            @Param("userUuid") String userUuid,
            @Param("teamUuidList") List<String> teamUuidList,
            @Param("roleUuidList") List<String> roleUuidList
    );

    public List<Long> getCombopPhaseIdListByCombopId(Long combopId);

    public int insertAutoexecCombop(AutoexecCombopVo autoexecCombopVo);

    public int insertAutoexecCombopAuthorityVoList(List<AutoexecCombopAuthorityVo> autoexecCombopAuthorityVoList);

    public int insertAutoexecCombopParamVoList(List<AutoexecCombopParamVo> autoexecCombopParamVoList);

    public int insertAutoexecCombopPhase(AutoexecCombopPhaseVo autoexecCombopPhaseVo);

    public int insertAutoexecCombopPhaseOperation(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo);

    public int updateAutoexecCombopById(AutoexecCombopVo autoexecCombopVo);

    public int updateAutoexecCombopIsActiveById(AutoexecCombopVo autoexecCombopVo);

    public int deleteAutoexecCombopById(Long id);

    public int deleteAutoexecCombopAuthorityByCombopId(Long combopId);

    public int deleteAutoexecCombopParamByCombopId(Long combopId);

    public int deleteAutoexecCombopPhaseByCombopId(Long combopId);

    public int deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(List<Long> combopPhaseIdList);
}
