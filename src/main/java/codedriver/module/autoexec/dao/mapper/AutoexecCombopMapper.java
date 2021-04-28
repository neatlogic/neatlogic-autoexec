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
    int checkAutoexecCombopIsExists(Long id);

    Long checkAutoexecCombopNameIsRepeat(AutoexecCombopVo autoexecCombopVo);

    Long checkAutoexecCombopUkIsRepeat(AutoexecCombopVo autoexecCombopVo);

    Integer getAutoexecCombopIsActiveByIdForUpdate(Long id);

    AutoexecCombopVo getAutoexecCombopById(Long id);

    int getAutoexecCombopCount(AutoexecCombopVo searchVo);

    List<AutoexecCombopVo> getAutoexecCombopList(AutoexecCombopVo searchVo);

    List<AutoexecCombopAuthorityVo> getAutoexecCombopAuthorityListByCombopIdAndAction(@Param("combopId") Long combopId, @Param("action") String action);

    List<AutoexecCombopParamVo> getAutoexecCombopParamListByCombopId(Long combopId);

    List<String> getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(
            @Param("combopId") Long combopId,
            @Param("userUuid") String userUuid,
            @Param("teamUuidList") List<String> teamUuidList,
            @Param("roleUuidList") List<String> roleUuidList
    );

    List<Long> getCombopPhaseIdListByCombopId(Long combopId);

    List<Long> checkAutoexecCombopIdListIsExists(List<Long> idList);

    int insertAutoexecCombop(AutoexecCombopVo autoexecCombopVo);

    int insertAutoexecCombopAuthorityVoList(List<AutoexecCombopAuthorityVo> autoexecCombopAuthorityVoList);

    int insertAutoexecCombopParamVoList(List<AutoexecCombopParamVo> autoexecCombopParamVoList);

    int insertAutoexecCombopPhase(AutoexecCombopPhaseVo autoexecCombopPhaseVo);

    int insertAutoexecCombopPhaseOperation(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo);

    int updateAutoexecCombopById(AutoexecCombopVo autoexecCombopVo);

    int updateAutoexecCombopIsActiveById(AutoexecCombopVo autoexecCombopVo);

    int updateAutoexecCombopConfigById(AutoexecCombopVo autoexecCombopVo);

    int deleteAutoexecCombopById(Long id);

    int deleteAutoexecCombopAuthorityByCombopId(Long combopId);

    int deleteAutoexecCombopParamByCombopId(Long combopId);

    int deleteAutoexecCombopPhaseByCombopId(Long combopId);

    int deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(List<Long> combopPhaseIdList);
}
