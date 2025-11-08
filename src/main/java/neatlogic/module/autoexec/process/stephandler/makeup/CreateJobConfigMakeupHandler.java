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

package neatlogic.module.autoexec.process.stephandler.makeup;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.process.dto.ProcessStepVo;
import neatlogic.framework.process.stephandler.core.IProcessStepInternalHandler;
import neatlogic.framework.process.stephandler.core.IProcessStepMakeupHandler;
import neatlogic.module.autoexec.dependency.AutoexecCombop2ProcessStepDependencyHandler;
import neatlogic.module.autoexec.process.dto.CreateJobConfigConfigVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CreateJobConfigMakeupHandler implements IProcessStepMakeupHandler {
    @Override
    public String getName() {
        return "createJobConfig";
    }

    @Override
    public void makeup(IProcessStepInternalHandler processStepInternalHandler, ProcessStepVo processStepVo, JSONObject stepConfigObj, String action) {
        CreateJobConfigVo createJobConfigVo = stepConfigObj.getObject("createJobConfig", CreateJobConfigVo.class);
        if (createJobConfigVo != null) {
            List<CreateJobConfigConfigVo> configList = createJobConfigVo.getConfigList();
            if (CollectionUtils.isNotEmpty(configList)) {
                if (Objects.equals(action, "save")) {
                    for (CreateJobConfigConfigVo createJobConfigConfigVo : configList) {
                        if (createJobConfigConfigVo.getCombopId() != null) {
                            JSONObject config = new JSONObject();
                            config.put("processUuid", processStepVo.getProcessUuid());
                            config.put("stepUuid", processStepVo.getUuid());
                            config.put("stepName", processStepVo.getName());
                            DependencyManager.insert(AutoexecCombop2ProcessStepDependencyHandler.class, createJobConfigConfigVo.getCombopId(), processStepVo.getUuid(), config);
                        }
                    }
                } else if (Objects.equals(action, "delete")) {
                    DependencyManager.delete(AutoexecCombop2ProcessStepDependencyHandler.class, processStepVo.getUuid());
                }
            }
        }
    }
}
