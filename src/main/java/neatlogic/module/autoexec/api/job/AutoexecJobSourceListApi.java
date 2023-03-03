/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

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

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author lvzk
 * @since 2021/4/21 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobSourceListApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取作业来源";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({

    })
    @Output({
    })
    @Description(desc = "获取作业来源")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray sourceArray = new JSONArray();
        Map<String, String> sourceMap = AutoexecJobSourceFactory.getSourceValueMap();
        for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
            sourceArray.add(new JSONObject() {
                {
                    put("value", entry.getKey());
                    put("text", entry.getValue());
                }
            });
        }
        return sourceArray;
    }

    @Override
    public String getToken() {
        return "autoexec/job/source/list";
    }
}
