/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.AutoexecRiskVo;
import codedriver.framework.common.dto.ValueTextVo;

import java.util.List;

public interface AutoexecRiskMapper {

    int checkRiskIsExistsById(Long id);

    Long getRiskIdByName(String name);

    List<ValueTextVo> getAllActiveRisk();

    AutoexecRiskVo getAutoexecRiskById(Long riskId);

    int searchRiskCount(AutoexecRiskVo vo);

    List<AutoexecRiskVo> searchRisk(AutoexecRiskVo vo);
}
