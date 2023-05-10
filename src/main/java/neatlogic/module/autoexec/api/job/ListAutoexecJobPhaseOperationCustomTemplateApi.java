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
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAutoexecJobPhaseOperationCustomTemplateApi extends PrivateApiComponentBase {

    final static Logger logger = LoggerFactory.getLogger(ListAutoexecJobPhaseOperationCustomTemplateApi.class);

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取阶段工具引用的自定义模版列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "作业阶段id", isRequired = true),
    })
    @Output({})
    @Description(desc = "获取自动化作业阶段工具引用的自定义模版列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        /*
            模版要按工具在阶段的顺序排序，一个模版对应一个工具
            如果同一阶段下，多个工具引用同一个模版，那么只取最下游的那个工具作为该模版的数据来源
         */
        Long jobPhaseId = jsonObj.getLong("jobPhaseId");
        List<CustomTemplateVo> customTemplateList = autoexecJobMapper.getJobPhaseOperationCustomTemplateListByJobPhaseId(jobPhaseId);
        List<Long> operationIdList = autoexecJobMapper.getJobPhaseOpertionIdListByJobPhaseId(jobPhaseId);
        if (customTemplateList.size() > 0 && operationIdList.size() > 0) {
            Map<Long, CustomTemplateVo> map = new LinkedHashMap<>();
            for (Long operationId : operationIdList) {
                Optional<CustomTemplateVo> opt = customTemplateList.stream().filter(o -> Objects.equals(o.getOperationId(), operationId)).findFirst();
                opt.ifPresent(customTemplateVo -> map.put(customTemplateVo.getId(), customTemplateVo));
            }
            return map.values();
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/operation/customtemplate/list";
    }
}
