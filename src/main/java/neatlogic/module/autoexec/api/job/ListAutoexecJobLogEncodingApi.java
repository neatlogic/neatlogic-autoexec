/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.AutoexecTenantConfig;
import neatlogic.framework.autoexec.constvalue.JobLogEncoding;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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
