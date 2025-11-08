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

import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobContentVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class AutoexecJobNotSupportedServiceImpl implements  AutoexecJobNotSupportedService{
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    //共享表避免竞争无需事务
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void insertIntoJobContent(String hash, String configStr){
        autoexecJobMapper.insertJobContent(new AutoexecJobContentVo(hash, configStr));
    }
}
