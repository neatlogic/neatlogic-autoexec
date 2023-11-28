/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobContentVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/6/8 16:00
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobRunTimeParamGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取作业运行参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
    })
    @Output({

    })
    @Description(desc = "获取作业运行参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        //运行变量
        AutoexecJobContentVo paramContentVo = autoexecJobMapper.getJobContent(jobVo.getParamHash());
        if(paramContentVo != null) {
            JSONArray runTimeParam = JSONObject.parseArray(paramContentVo.getContent());
            //集成数据特殊处理，截取text
            for (int i = 0; i < runTimeParam.size(); i++) {
                Object value = runTimeParam.getJSONObject(i).get("value");
                Object defaultValue = runTimeParam.getJSONObject(i).get("defaultValue");
                String type = runTimeParam.getJSONObject(i).getString("type");
                if (StringUtils.isNotBlank(type) && value != null) {
                    IScriptParamType paramType = ScriptParamTypeFactory.getHandler(type);
                    if (paramType != null) {
                        JSONObject config = runTimeParam.getJSONObject(i).getJSONObject("config");
                        runTimeParam.getJSONObject(i).put("value", paramType.getTextByValue(value, config));
                        if (defaultValue != null) {
                            runTimeParam.getJSONObject(i).put("defaultValue", paramType.getTextByValue(defaultValue, config));
                        }
                    }
                }
            }
            result.put("runTimeParamList", runTimeParam);
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/runtime/param/get";
    }
}
