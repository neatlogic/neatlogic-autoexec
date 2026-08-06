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

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_JOB_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_JOB_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class CleanAutoexecJobApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private RunnerMapper runnerMapper;

    @Resource
    private AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "nmaa.cleanautoexecjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "dayBefore", type = ApiParamType.INTEGER, desc = "nmaa.cleanautoexecjobapi.input.param.desc.daybefore", isRequired = true),
            @Param(name = "runnerNameList", type = ApiParamType.JSONARRAY, desc = "nmaa.cleanautoexecjobapi.input.param.desc.runnernamelist")
    })
    @Output({
    })
    @Description(desc = "nmaa.cleanautoexecjobapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray runnerNameArray = jsonObj.getJSONArray("runnerNameList");
        List<RunnerMapVo> runnerMapVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(runnerNameArray)) {
            runnerMapVoList = runnerMapper.getRunnerMapByRunnerNameList(runnerNameArray.toJavaList(String.class));
        }
        if (CollectionUtils.isEmpty(runnerMapVoList)) {
            runnerMapVoList = runnerMapper.getAllRunnerMapList();
        }
        autoexecJobService.cleanHistoryJobAutoexecData(runnerMapVoList.stream().map(RunnerMapVo::getRunnerMapId).collect(Collectors.toList()), jsonObj.getInteger("dayBefore"));
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/data/clean";
    }
}
