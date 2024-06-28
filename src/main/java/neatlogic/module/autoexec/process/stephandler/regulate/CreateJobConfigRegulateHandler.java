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

package neatlogic.module.autoexec.process.stephandler.regulate;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.process.stephandler.core.IProcessStepInternalHandler;
import neatlogic.framework.process.stephandler.core.IRegulateHandler;
import neatlogic.module.autoexec.process.dto.CreateJobConfigVo;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

@Component
public class CreateJobConfigRegulateHandler implements IRegulateHandler {
    @Override
    public String getName() {
        return "createJobConfig";
    }

    @Override
    public void regulateConfig(IProcessStepInternalHandler processStepInternalHandler, JSONObject oldConfigObj, JSONObject newConfigObj) {
        JSONObject createJobConfig = oldConfigObj.getJSONObject("createJobConfig");
        if (MapUtils.isEmpty(createJobConfig)) {
            newConfigObj.put("createJobConfig", new CreateJobConfigVo());
        } else {
            newConfigObj.put("createJobConfig", createJobConfig.toJavaObject(CreateJobConfigVo.class));
        }
    }
}
