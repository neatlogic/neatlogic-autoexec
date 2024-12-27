/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
