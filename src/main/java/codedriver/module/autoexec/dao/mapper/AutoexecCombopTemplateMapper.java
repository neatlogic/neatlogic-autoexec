/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;

import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/13 11:05
 **/
public interface AutoexecCombopTemplateMapper {
    int checkAutoexecCombopIsExists(Long id);

    Long checkAutoexecCombopNameIsRepeat(AutoexecCombopTemplateVo autoexecCombopVo);

    Integer getAutoexecCombopIsActiveByIdForUpdate(Long id);

    AutoexecCombopTemplateVo getAutoexecCombopById(Long id);

    int getAutoexecCombopCount(AutoexecCombopTemplateVo searchVo);

    List<AutoexecCombopTemplateVo> getAutoexecCombopList(AutoexecCombopTemplateVo searchVo);

    List<Long> checkAutoexecCombopIdListIsExists(List<Long> idList);

    int insertAutoexecCombop(AutoexecCombopTemplateVo autoexecCombopVo);

    int updateAutoexecCombopById(AutoexecCombopTemplateVo autoexecCombopVo);

    int updateAutoexecCombopIsActiveById(AutoexecCombopTemplateVo autoexecCombopVo);

    int updateAutoexecCombopConfigById(AutoexecCombopTemplateVo autoexecCombopVo);

    int deleteAutoexecCombopById(Long id);

}
