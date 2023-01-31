/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

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
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/6/6 14:49
 **/
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TailAutoexecJobPhaseNodeLogApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "获取剧本节点执行日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "status", type = ApiParamType.STRING, isRequired = true, desc = "node status ,用于判断刷新状态"),
            @Param(name = "logPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取位置,-1:获取最新的数据。如果是向上读'up'，则每次向上滚动加载传startPos。如果是向下读 'down'，则每次向下加载传endPos"),
            @Param(name = "direction", type = ApiParamType.ENUM, rule = "up,down", isRequired = true, desc = "读取方向，up:向上读，down:向下读"),
            @Param(name = "encoding", type = ApiParamType.STRING, desc = "字符编码", defaultValue = "UTF-8")
    })
    @Output({
            @Param(name = "tailContent", type = ApiParamType.LONG, isRequired = true, desc = "内容"),
            @Param(name = "startPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取开始位置"),
            @Param(name = "endPos", type = ApiParamType.LONG, isRequired = true, desc = "日志读取结束位置"),
            @Param(name = "logPos", type = ApiParamType.LONG, isRequired = true, desc = "读取到的位置"),
            @Param(name = "last", type = ApiParamType.LONG, isRequired = true, desc = "日志读取内容"),
            @Param(name = "interact", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "作业节点等待输入渲染json"),
            @Param(name = "isRefresh", type = ApiParamType.INTEGER, isRequired = true, desc = "是否需要继续定时刷新，1:继续 0:停止")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        autoexecJobService.validateAutoexecJobLogEncoding(paramObj.getString("encoding"));
        AutoexecJobVo jobVo = new AutoexecJobVo();
        jobVo.setActionParam(paramObj);
        jobVo.setAction(JobAction.TAIL_NODE_LOG.getValue());
        IAutoexecJobActionHandler tailNodeLogAction = AutoexecJobActionHandlerFactory.getAction(JobAction.TAIL_NODE_LOG.getValue());
        return tailNodeLogAction.doService(jobVo);
    }

    @Override
    public String getToken() {
        return "/autoexec/job/phase/node/log/tail";
    }
}
