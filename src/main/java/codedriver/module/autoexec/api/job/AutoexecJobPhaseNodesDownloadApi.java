/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CacheControlType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicBinaryStreamApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service

@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodesDownloadApi extends PublicBinaryStreamApiComponentBase {

    @Autowired
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getToken() {
        return "autoexec/job/phase/nodes/download";
    }

    @Override
    public String getName() {
        return "下载作业剧本节点";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @CacheControl(cacheControlType = CacheControlType.MAXAGE, maxAge = 30000)
    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "剧本", isRequired = true)
    })
    @Description(desc = "下载作业剧本节点")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        String phaseName = paramObj.getString("phase");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobDetailByJobIdAndPhaseName(jobId,phaseName);
        if(jobVo == null){
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeVoList =  autoexecJobMapper.searchJobPhaseNode(new AutoexecJobPhaseNodeVo(paramObj.getLong("jobId"),paramObj.getString("phase")));
        ServletOutputStream os = response.getOutputStream();
        for (AutoexecJobPhaseNodeVo nodeVo : autoexecJobPhaseNodeVoList){
            JSONObject nodeJson = new JSONObject(){{
                //TODO 待资源中心完善，需补充
                put("nodeId",nodeVo.getNodeId());
                put("nodeType","ssh");
                put("host",nodeVo.getHost());
                put("port",nodeVo.getPort());
                put("username","root");
                put("password","techsure");
            }};
            response.setContentType("application/json");
            response.setHeader("Content-Disposition", "attachment;fileName=nodes.json");
            IOUtils.copyLarge(IOUtils.toInputStream(nodeJson.toString(), StandardCharsets.UTF_8), os);
            if (os != null) {
                os.flush();
            }
        }
        if(os != null) {
            os.close();
        }
        return null;
    }
}
