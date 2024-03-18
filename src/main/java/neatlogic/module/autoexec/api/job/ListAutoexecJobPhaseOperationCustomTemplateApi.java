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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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
