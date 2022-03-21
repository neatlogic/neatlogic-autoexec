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
//    int checkAutoexecCombopTemplateIsExists(Long id);

    Long checkAutoexecCombopTemplateNameIsRepeat(AutoexecCombopTemplateVo autoexecCombopVo);

    Integer getAutoexecCombopTemplateIsActiveByIdForUpdate(Long id);

    AutoexecCombopTemplateVo getAutoexecCombopTemplateById(Long id);

    int getAutoexecCombopTemplateCount(AutoexecCombopTemplateVo searchVo);

    List<AutoexecCombopTemplateVo> getAutoexecCombopTemplateList(AutoexecCombopTemplateVo searchVo);

    List<Long> checkAutoexecCombopTemplateIdListIsExists(List<Long> idList);

    int insertAutoexecCombopTemplate(AutoexecCombopTemplateVo autoexecCombopVo);

    int updateAutoexecCombopTemplateById(AutoexecCombopTemplateVo autoexecCombopVo);

    int updateAutoexecCombopTemplateIsActiveById(AutoexecCombopTemplateVo autoexecCombopVo);

//    int updateAutoexecCombopTemplateConfigById(AutoexecCombopTemplateVo autoexecCombopVo);

    int deleteAutoexecCombopTemplateById(Long id);

}
