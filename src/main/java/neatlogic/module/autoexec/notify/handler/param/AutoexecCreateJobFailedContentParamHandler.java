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

package neatlogic.module.autoexec.notify.handler.param;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.AutoexecNotifyParam;
import neatlogic.framework.autoexec.constvalue.AutoexecNotifyTriggerType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.notify.core.INotifyTriggerType;
import neatlogic.framework.process.crossover.IProcessTaskStepDataCrossoverMapper;
import neatlogic.framework.process.dto.ProcessTaskStepDataVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.notify.core.ProcessTaskNotifyParamHandlerBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

@Component
public class AutoexecCreateJobFailedContentParamHandler extends ProcessTaskNotifyParamHandlerBase {
    @Override
    public String getValue() {
        return AutoexecNotifyParam.CREATE_JOB_FAILED_CONTENT.getValue();
    }

    @Override
    public Object getMyText(ProcessTaskStepVo processTaskStepVo, INotifyTriggerType notifyTriggerType) {
        if (!(notifyTriggerType == AutoexecNotifyTriggerType.CREATE_JOB_FAILED)) {
            return null;
        }
        if (processTaskStepVo == null) {
            return null;
        }
        IProcessTaskStepDataCrossoverMapper processTaskStepDataCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskStepDataCrossoverMapper.class);
        ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo();
        searchVo.setProcessTaskId(processTaskStepVo.getProcessTaskId());
        searchVo.setProcessTaskStepId(processTaskStepVo.getId());
        searchVo.setType("autoexecCreateJobError");
        ProcessTaskStepDataVo processTaskStepDataVo = processTaskStepDataCrossoverMapper.getProcessTaskStepData(searchVo);
        if (processTaskStepDataVo != null) {
            JSONObject dataObj = processTaskStepDataVo.getData();
            if (MapUtils.isNotEmpty(dataObj)) {
                JSONArray errorList = dataObj.getJSONArray("errorList");
                if (CollectionUtils.isNotEmpty(errorList)) {
                    return errorList.toJSONString();
                }
            }
        }
        return null;
    }
}
