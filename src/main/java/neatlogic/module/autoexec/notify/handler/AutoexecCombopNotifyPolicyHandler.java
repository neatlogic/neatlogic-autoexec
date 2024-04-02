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

package neatlogic.module.autoexec.notify.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.AutoexecJobNotifyParam;
import neatlogic.framework.autoexec.constvalue.AutoexecJobNotifyTriggerType;
import neatlogic.framework.dto.ConditionParamVo;
import neatlogic.framework.notify.core.NotifyPolicyHandlerBase;
import neatlogic.framework.notify.dto.NotifyTriggerVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: linbq
 * @since: 2021/4/15 9:47
 **/
@Component
public class AutoexecCombopNotifyPolicyHandler extends NotifyPolicyHandlerBase {
    @Override
    public String getName() {
        return "term.autoexec.combop";
    }

    /**
     * 绑定权限，每种handler对应不同的权限
     */
    @Override
    public String getAuthName() {
        return AUTOEXEC_COMBOP_ADD.class.getSimpleName();
    }

    @Override
    protected List<NotifyTriggerVo> myNotifyTriggerList() {
        List<NotifyTriggerVo> returnList = new ArrayList<>();
        for (AutoexecJobNotifyTriggerType triggerType : AutoexecJobNotifyTriggerType.values()) {
            returnList.add(new NotifyTriggerVo(triggerType));
        }
        return returnList;
    }

    @Override
    protected List<ConditionParamVo> mySystemParamList() {
        List<ConditionParamVo> notifyPolicyParamList = new ArrayList<>();
        for (AutoexecJobNotifyParam param : AutoexecJobNotifyParam.values()) {
            ConditionParamVo paramVo = new ConditionParamVo();
            paramVo.setName(param.getValue());
            paramVo.setLabel(param.getText());
            paramVo.setParamType(param.getParamType().getName());
            paramVo.setParamTypeName(param.getParamType().getText());
            paramVo.setFreemarkerTemplate(param.getFreemarkerTemplate());
            paramVo.setIsEditable(0);
            notifyPolicyParamList.add(paramVo);
        }
        return notifyPolicyParamList;
    }

    @Override
    protected List<ConditionParamVo> mySystemConditionOptionList() {
        return new ArrayList<>();
    }

    @Override
    protected void myAuthorityConfig(JSONObject config) {

    }
}
