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

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobLogEncoding;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.AutoexecTenantConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author laiwt
 * @since 2022/6/23 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAutoexecJobLogEncodingApi extends PrivateApiComponentBase {

    final static Logger logger = LoggerFactory.getLogger(ListAutoexecJobLogEncodingApi.class);

    @Override
    public String getName() {
        return "nmaaj.listautoexecjoblogencodingapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({})
    @Description(desc = "nmaaj.listautoexecjoblogencodingapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<String> result = null;
        String encodingConfigValue = ConfigManager.getConfig(AutoexecTenantConfig.AUTOEXEC_JOB_LOG_ENCODING);
        if (StringUtils.isNotBlank(encodingConfigValue)) {
            try {
                result = JSONArray.parseArray(encodingConfigValue).toJavaList(String.class);
            } catch (Exception ex) {
                logger.error("nmaaj.listautoexecjoblogencodingapi.mydoservice.error");
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            result = JobLogEncoding.getJobLogEncodingValueList();
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/log/encoding/list";
    }
}
