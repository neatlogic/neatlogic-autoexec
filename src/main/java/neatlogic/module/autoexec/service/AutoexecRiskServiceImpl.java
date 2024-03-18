/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.service;

import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dto.AutoexecRiskVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AutoexecRiskServiceImpl implements AutoexecRiskService{

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Override
    public Long saveRisk(AutoexecRiskVo autoexecRiskVo) {
        AutoexecRiskVo risk = autoexecRiskMapper.getAutoexecRiskById(autoexecRiskVo.getId());
        if (risk != null) {
            autoexecRiskVo.setSort(risk.getSort());
            autoexecRiskMapper.updateRisk(autoexecRiskVo);
        } else {
            Integer sort = autoexecRiskMapper.getMaxSort();
            autoexecRiskVo.setSort(sort != null ? sort + 1 : 1);
            autoexecRiskMapper.insertRisk(autoexecRiskVo);
        }
        return autoexecRiskVo.getId();
    }
}
