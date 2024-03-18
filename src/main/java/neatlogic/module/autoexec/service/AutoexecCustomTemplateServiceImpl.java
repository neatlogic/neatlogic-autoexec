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

import neatlogic.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecCustomTemplateMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AutoexecCustomTemplateServiceImpl implements AutoexecCustomTemplateService {

    @Resource
    private AutoexecCustomTemplateMapper autoexecCustomTemplateMapper;

    @Override
    public Long saveCustomTemplate(CustomTemplateVo customTemplateVo) {
        if (autoexecCustomTemplateMapper.getCustomTemplateById(customTemplateVo.getId()) != null) {
            autoexecCustomTemplateMapper.updateCustomTemplate(customTemplateVo);
        } else {
            autoexecCustomTemplateMapper.insertCustomTemplate(customTemplateVo);
        }
        return customTemplateVo.getId();
    }
}
