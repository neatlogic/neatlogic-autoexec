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

package neatlogic.module.autoexec.notify.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.AutoexecNotifyParam;
import neatlogic.framework.autoexec.constvalue.AutoexecNotifyTriggerType;
import neatlogic.framework.dto.ConditionParamVo;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
import neatlogic.framework.process.constvalue.ProcessTaskGroupSearch;
import neatlogic.framework.process.constvalue.ProcessUserType;
import neatlogic.framework.process.notify.core.ProcessTaskNotifyHandlerBase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AutoexecNotifyPolicyHandler extends ProcessTaskNotifyHandlerBase {
    @Override
    public String getName() {
        return "term.autoexec.groupname";
    }

    /**
     * 绑定权限，每种handler对应不同的权限
     */
    @Override
    public String getAuthName() {
        return AUTOEXEC_COMBOP_ADD.class.getSimpleName();
    }

    @Override
    protected List<NotifyTriggerVo> myCustomNotifyTriggerList() {
        List<NotifyTriggerVo> returnList = new ArrayList<>();
        for (AutoexecNotifyTriggerType triggerType : AutoexecNotifyTriggerType.values()) {
            returnList.add(new NotifyTriggerVo(triggerType));
        }
        return returnList;
    }

    @Override
    protected List<ConditionParamVo> myCustomSystemParamList() {
        List<ConditionParamVo> notifyPolicyParamList = new ArrayList<>();
        for (AutoexecNotifyParam param : AutoexecNotifyParam.values()) {
            notifyPolicyParamList.add(createConditionParam(param));
        }
        return notifyPolicyParamList;
    }

    @Override
    protected void myCustomAuthorityConfig(JSONObject config) {
        List<String> excludeList = config.getJSONArray("excludeList").toJavaList(String.class);
        excludeList.add(ProcessTaskGroupSearch.PROCESSUSERTYPE.getValue() + "#" + ProcessUserType.MINOR.getValue());
        config.put("excludeList", excludeList);
    }

    @Override
    public String getModuleGroup() {
        return "process";
    }
}
