/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.customtemplate.CustomTemplateVo;

import java.util.List;

public interface AutoexecCustomTemplateMapper {
    List<CustomTemplateVo> searchCustomTemplate(CustomTemplateVo customTemplateVo);

    int searchCustomTemplateCount(CustomTemplateVo customTemplateVo);

    CustomTemplateVo getCustomTemplateById(Long id);

    void updateCustomTemplate(CustomTemplateVo customTemplateVo);

    void insertCustomTemplate(CustomTemplateVo customTemplateVo);

    void deleteCustomTemplateById(Long id);
}
