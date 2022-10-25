/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.customtemplate.CustomTemplateVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
        return "获取自动化作业阶段工具引用的自定义模版列表";
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
