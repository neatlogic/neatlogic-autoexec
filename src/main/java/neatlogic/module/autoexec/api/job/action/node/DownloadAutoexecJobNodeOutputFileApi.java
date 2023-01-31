/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.job.action.node;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNodeNotFoundException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author lvzk
 * @since 2021/4/21 15:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class DownloadAutoexecJobNodeOutputFileApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "下载作业节点输出文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, isRequired = true, desc = "作业剧本Id"),
            @Param(name = "path", type = ApiParamType.STRING, isRequired = true, desc = "附件出参相对路径"),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源Id")
    })
    @Output({
    })
    @Description(desc = "下载作业节点输出文件")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Long jobPhaseId = jsonObj.getLong("jobPhaseId");
        Long resourceId = jsonObj.getLong("resourceId");
        AutoexecJobVo jobInfo = autoexecJobMapper.getJobInfo(jobId);
        if (jobInfo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        AutoexecJobPhaseNodeVo nodeVo = autoexecJobMapper.getJobPhaseNodeInfoByJobPhaseIdAndResourceId(jobPhaseId, resourceId);
        if (nodeVo == null) {
            throw new AutoexecJobPhaseNodeNotFoundException(jobPhaseId.toString(), resourceId);
        }
            String url = String.format("%s/api/binary/job/output/file/download", nodeVo.getRunnerUrl());
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.download(url, "POST", response.getOutputStream()).setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
            String error = httpRequestUtil.getError();
            if (StringUtils.isNotBlank(error)) {
                throw new RunnerHttpRequestException(url + ":" + error);
            }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/node/output/file/download";
    }
}
