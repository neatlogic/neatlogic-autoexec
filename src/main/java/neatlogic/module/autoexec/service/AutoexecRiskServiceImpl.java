/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
