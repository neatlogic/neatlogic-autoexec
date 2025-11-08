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

package neatlogic.module.autoexec.api.job.action.node;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author longrf
 * @date 2022/4/25 5:46 下午
 */

@Service
@AuthUser(SystemUser.AUTOEXEC)
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class CheckinAutoexecJobSqlApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "检查作业执行sql文件状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/sql/checkin";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sqlInfoList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "sql文件列表"),
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业id"),
            @Param(name = "phaseName", type = ApiParamType.STRING, isRequired = true, desc = "作业剧本名（导入sql）"),
            @Param(name = "targetPhaseName", type = ApiParamType.STRING, isRequired = true, desc = "目标作业剧本名（执行sql）"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "version", type = ApiParamType.STRING, desc = "版本"),
            @Param(name = "operType", type = ApiParamType.ENUM, rule = "auto,deploy", isRequired = true, desc = "来源类型")
    })
    @Output({
    })
    @Description(desc = "检查作业执行sql文件状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        IAutoexecJobSourceTypeHandler handler = AutoexecJobSourceTypeHandlerFactory.getAction(paramObj.getString("operType"));
        if(handler != null) {
            handler.checkinSqlList(paramObj);
        }
        return null;
    }
}
