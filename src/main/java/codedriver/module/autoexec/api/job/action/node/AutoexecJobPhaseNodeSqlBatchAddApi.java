package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeSqlVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/4/25 5:46 下午
 */

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeSqlBatchAddApi extends PublicApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "批量插入作业阶段节点执行sql状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/sql/batch/add";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "autoexecJobPhaseNodeSqlVoList", type = ApiParamType.JSONARRAY, desc = "作业剧本节点sql状态列表"),
    })
    @Output({
    })
    @Description(desc = "批量插入作业阶段节点执行sql状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray phaseNodeSqlVoArray = paramObj.getJSONArray("autoexecJobPhaseNodeSqlVoList");

        if (CollectionUtils.isNotEmpty(phaseNodeSqlVoArray)) {
            List<AutoexecJobPhaseNodeSqlVo>  phaseNodeSqlVoList = phaseNodeSqlVoArray.toJavaList(AutoexecJobPhaseNodeSqlVo.class);
            autoexecJobMapper.insertJobPhaseNodeSql(phaseNodeSqlVoList);
        }
        return null;
    }
}
