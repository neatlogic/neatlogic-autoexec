/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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
        return "清除runner历史作业文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "dayBefore", type = ApiParamType.INTEGER, desc = "保留天数", isRequired = true),
            @Param(name = "runnerNameList", type = ApiParamType.JSONARRAY, desc = "执行器名列表")
    })
    @Output({
    })
    @Description(desc = "清除runner历史作业文件")
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
