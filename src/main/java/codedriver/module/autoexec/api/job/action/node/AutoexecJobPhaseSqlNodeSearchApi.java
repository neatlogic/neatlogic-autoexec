package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/4/28 10:14 上午
 */
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseSqlNodeSearchApi extends PrivateApiComponentBase {


    @Override
    public String getName() {
        return "作业剧本sql文件节点搜索";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return null;
    }

    @Override
    public String getToken() {
        return  "autoexec/job/phase/node/sql/search";
    }
}
