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

package neatlogic.module.autoexec.api.tool;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_MANAGE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecToolArgumentUpdateApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/argument/update";
    }

    @Override
    public String getName() {
        return "nmaa.autoexectoolargumentupdateapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
    })
    @Description(desc = "nmaa.autoexectoolargumentupdateapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AutoexecToolVo> allTool = autoexecToolMapper.getAllTool();
        for (AutoexecToolVo toolVo : allTool) {
            JSONObject config = toolVo.getConfig();
            if (config != null) {
                JSONObject argument = config.getJSONObject("argument");
                if (argument != null) {
                    config.put("argument", new AutoexecParamVo(argument));
                    toolVo.setConfigStr(config.toJSONString());
                    autoexecToolMapper.updateConfig(toolVo);
                }
            }
        }
        return null;
    }


}
