/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

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
