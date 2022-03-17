/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateAuthorityVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateParamVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/13 11:05
 **/
public interface AutoexecCombopTemplateMapper {
    int checkAutoexecCombopIsExists(Long id);

    Long checkAutoexecCombopNameIsRepeat(AutoexecCombopTemplateVo autoexecCombopVo);

//    Long checkAutoexecCombopUkIsRepeat(AutoexecCombopTemplateVo autoexecCombopVo);

    Integer getAutoexecCombopIsActiveByIdForUpdate(Long id);

    AutoexecCombopTemplateVo getAutoexecCombopById(Long id);

//    AutoexecCombopTemplateVo getAutoexecCombopByName(String name);

//    List<AutoexecCombopTemplateVo> getAutoexecCombopByIdList(ArrayList<Long> idList);

    int getAutoexecCombopCount(AutoexecCombopTemplateVo searchVo);

    List<AutoexecCombopTemplateVo> getAutoexecCombopList(AutoexecCombopTemplateVo searchVo);

//    List<AutoexecCombopTemplateVo> getAutoexecCombopListByIdList(List<Long> idList);

    List<AutoexecCombopTemplateAuthorityVo> getAutoexecCombopAuthorityListByCombopIdAndAction(@Param("combopTemplateId") Long combopTemplateId, @Param("action") String action);

    List<AutoexecCombopTemplateParamVo> getAutoexecCombopParamListByCombopId(Long combopId);

//    AutoexecCombopTemplateParamVo getAutoexecCombopParamByCombopIdAndKey(@Param("combopId") Long combopId, @Param("key") String key);

//    List<String> getAutoexecCombopAuthorityListByCombopIdAndUserUuidAndTeamUuidListAndRoleUuidList(
//            @Param("combopId") Long combopId,
//            @Param("userUuid") String userUuid,
//            @Param("teamUuidList") List<String> teamUuidList,
//            @Param("roleUuidList") List<String> roleUuidList
//    );

//    Set<Long> getExecutableAutoexecCombopIdListByKeywordAndAuthenticationInfo(
//            @Param("keyword")String keyword,
//            @Param("authenticationInfoVo") AuthenticationInfoVo authenticationInfoVo
//    );

//    List<Long> getCombopPhaseIdListByCombopId(Long combopId);

    List<Long> checkAutoexecCombopIdListIsExists(List<Long> idList);

//    Long checkItHasBeenGeneratedToCombopByOperationId(Long operationId);

    int insertAutoexecCombop(AutoexecCombopTemplateVo autoexecCombopVo);

    int insertAutoexecCombopAuthorityVoList(List<AutoexecCombopTemplateAuthorityVo> autoexecCombopAuthorityVoList);

    int insertAutoexecCombopParamVoList(List<AutoexecCombopTemplateParamVo> autoexecCombopParamVoList);

//    int insertAutoexecCombopPhase(AutoexecCombopPhaseVo autoexecCombopPhaseVo);

//    int insertAutoexecCombopPhaseOperation(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo);

    int updateAutoexecCombopById(AutoexecCombopTemplateVo autoexecCombopVo);

    int updateAutoexecCombopIsActiveById(AutoexecCombopTemplateVo autoexecCombopVo);

    int updateAutoexecCombopConfigById(AutoexecCombopTemplateVo autoexecCombopVo);

    int deleteAutoexecCombopById(Long id);

    int deleteAutoexecCombopAuthorityByCombopId(Long combopId);

    int deleteAutoexecCombopParamByCombopId(Long combopId);

//    int deleteAutoexecCombopPhaseByCombopId(Long combopId);

//    int deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(List<Long> combopPhaseIdList);

}
