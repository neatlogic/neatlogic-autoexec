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

package neatlogic.module.autoexec.dependency;

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DefaultDependencyHandlerBase;
import neatlogic.framework.dependency.core.IFromType;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dependency.dto.DependencyVo;
import neatlogic.framework.process.crossover.IProcessCrossoverMapper;
import neatlogic.framework.process.dto.ProcessVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigConfigVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AutoexecCombop2ProcessStepDependencyHandler extends DefaultDependencyHandlerBase {

    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {
        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        String stepUuid = dependencyVo.getTo();
        String processUuid = config.getString("processUuid");
        IProcessCrossoverMapper processCrossoverMapper = CrossoverServiceFactory.getApi(IProcessCrossoverMapper.class);
        ProcessVo processVo = processCrossoverMapper.getProcessBaseInfoByUuid(processUuid);
        if (processVo == null) {
            return null;
        }
        JSONObject processConfig = processVo.getConfig();
        if (MapUtils.isEmpty(processConfig)) {
            return null;
        }
        JSONObject processObj = processConfig.getJSONObject("process");
        if (MapUtils.isEmpty(processObj)) {
            return null;
        }
        JSONArray stepList = processObj.getJSONArray("stepList");
        if (CollectionUtils.isEmpty(stepList)) {
            return null;
        }
        for (int i = 0; i < stepList.size(); i++) {
            JSONObject stepObj = stepList.getJSONObject(i);
            if (MapUtils.isEmpty(stepObj)) {
                continue;
            }
            String uuid = stepObj.getString("uuid");
            if (!Objects.equals(uuid, stepUuid)) {
                continue;
            }
            JSONObject stepConfig = stepObj.getJSONObject("stepConfig");
            if (MapUtils.isEmpty(stepConfig)) {
                return null;
            }
            JSONObject createJobConfig = stepConfig.getJSONObject("createJobConfig");
            if (MapUtils.isEmpty(createJobConfig)) {
                return null;
            }
            boolean flag = false;
            CreateJobConfigVo createJobConfigVo = createJobConfig.toJavaObject(CreateJobConfigVo.class);
            List<CreateJobConfigConfigVo> configList = createJobConfigVo.getConfigList();
            if (CollectionUtils.isNotEmpty(configList)) {
                for (CreateJobConfigConfigVo createJobConfigConfigVo : configList) {
                    if (createJobConfigConfigVo.getCombopId() != null && Objects.equals(createJobConfigConfigVo.getCombopId().toString(), dependencyVo.getFrom())) {
                        flag = true;
                        break;
                    }
                }
            }
            if (!flag) {
                return null;
            }
            JSONObject dependencyInfoConfig = new JSONObject();
            dependencyInfoConfig.put("processUuid", processUuid);
            dependencyInfoConfig.put("stepUuid", stepUuid);
            List<String> pathList = new ArrayList<>();
            pathList.add($.t("nmar.dependency.workflowmanagement"));
            pathList.add(processVo.getName());
            String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/process.html#/flow-edit?uuid=${DATA.processUuid}&stepUuid=${DATA.stepUuid}";
            return new DependencyInfoVo(stepUuid, dependencyInfoConfig, stepObj.getString("name"), pathList, urlFormat, this.getGroupName());
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.COMBOP;
    }
}
