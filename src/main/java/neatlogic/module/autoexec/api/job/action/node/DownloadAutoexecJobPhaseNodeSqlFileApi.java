package neatlogic.module.autoexec.api.job.action.node;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author longrf
 * @date 2022/5/30 10:49 上午
 */
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadAutoexecJobPhaseNodeSqlFileApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getName() {
        return "下载节点sql文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/sql/file/download";
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源Id"),
            @Param(name = "sqlId", type = ApiParamType.LONG, isRequired = true, desc = "sql id"),
            @Param(name = "sqlName", type = ApiParamType.STRING, isRequired = true, desc = "sql名")
    })
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.DOWNLOAD_NODE_SQL_FILE.getValue());
        IAutoexecJobActionHandler getNodeSqlContentAction = AutoexecJobActionHandlerFactory.getAction(JobAction.DOWNLOAD_NODE_SQL_FILE.getValue());
        return getNodeSqlContentAction.doService(jobVo);
    }
}
