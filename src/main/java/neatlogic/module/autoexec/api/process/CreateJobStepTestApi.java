/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.autoexec.api.process;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopVersionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.process.crossover.IProcessTaskCrossoverMapper;
import neatlogic.framework.process.crossover.ISelectContentByHashCrossoverMapper;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.process.dto.AutoexecJobBuilder;
import neatlogic.module.autoexec.process.dto.CreateJobConfigConfigVo;
import neatlogic.module.autoexec.process.dto.CreateJobConfigVo;
import neatlogic.module.autoexec.process.util.CreateJobConfigUtil;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class CreateJobStepTestApi extends PrivateApiComponentBase {
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Override
    public String getName() {
        return "nmaap.createjobsteptestapi.getname";
    }

    @Input({
            @Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "term.itsm.processtaskid"),
            @Param(name = "processTaskStepId", type = ApiParamType.LONG, desc = "term.itsm.processtaskstepid"),
            @Param(name = "createJobConfig", type = ApiParamType.JSONOBJECT, desc = "common.config")
    })
    @Output({
            @Param(name = "tbodyList", explode = AutoexecJobBuilder[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmaap.createjobsteptestapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ProcessTaskStepVo processTaskStep = new ProcessTaskStepVo();
        Long processTaskId = paramObj.getLong("processTaskId");
        processTaskStep.setProcessTaskId(processTaskId);
        processTaskStep.setId(1L);
        JSONObject createJobConfig = paramObj.getJSONObject("createJobConfig");
        if (MapUtils.isEmpty(createJobConfig)) {
            Long processTaskStepId = paramObj.getLong("processTaskStepId");
            if (processTaskStepId == null) {
                return null;
            }
            processTaskStep.setId(processTaskStepId);
            IProcessTaskCrossoverMapper processTaskCrossoverMapper = CrossoverServiceFactory.getApi(IProcessTaskCrossoverMapper.class);
            ISelectContentByHashCrossoverMapper selectContentByHashCrossoverMapper = CrossoverServiceFactory.getApi(ISelectContentByHashCrossoverMapper.class);
            ProcessTaskStepVo processTaskStepVo = processTaskCrossoverMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
            // 获取工单当前步骤配置信息
            String config = selectContentByHashCrossoverMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
            if (StringUtils.isBlank(config)) {
                return null;
            }
            createJobConfig = (JSONObject) JSONPath.read(config, "createJobConfig");
        }
        CreateJobConfigVo createJobConfigVo = createJobConfig.toJavaObject(CreateJobConfigVo.class);
        List<CreateJobConfigConfigVo> configList = createJobConfigVo.getConfigList();
        if (CollectionUtils.isEmpty(configList)) {
            return null;
        }
        List<AutoexecJobBuilder> tbodyList = new ArrayList<>();
        for (CreateJobConfigConfigVo createJobConfigConfigVo : configList) {
            Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(createJobConfigConfigVo.getCombopId());
            if (activeVersionId == null) {
                throw new AutoexecCombopActiveVersionNotFoundException(createJobConfigConfigVo.getCombopId());
            }
            AutoexecCombopVersionVo autoexecCombopVersionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
            if (autoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(activeVersionId);
            }
            List<AutoexecJobBuilder> builderList = CreateJobConfigUtil.createAutoexecJobBuilderList(processTaskStep, createJobConfigConfigVo, autoexecCombopVersionVo);
            if (CollectionUtils.isNotEmpty(builderList)) {
                tbodyList.addAll(builderList);
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("tbodyList", tbodyList);
        return resultObj;
    }

    @Override
    public String getToken() {
        return "create/job/step/test";
    }
}
