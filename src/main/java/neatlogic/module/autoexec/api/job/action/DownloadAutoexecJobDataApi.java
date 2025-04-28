/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.job.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobRunnerNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.HttpRequestUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadAutoexecJobDataApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "/autoexec/job/data/download";
    }

    @Override
    public String getName() {
        return "下载作业data";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private RunnerMapper runnerMapper;

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业Id"),
            @Param(name = "runnerName", type = ApiParamType.STRING, desc = "执行器名")
    })
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        String runnerName = paramObj.getString("runnerName");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        List<RunnerMapVo> runnerMapVoList;
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        if (StringUtils.isNotBlank(runnerName)) {
            RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerName(runnerName);
            if (runnerMapVo != null) {
                runnerMapVoList = Collections.singletonList(runnerMapVo);
            } else {
                throw new AutoexecJobRunnerNotFoundException(runnerName);
            }
        } else {
            runnerMapVoList = autoexecJobMapper.getJobRunnerMapByJobId(jobId);
        }

        String fileName = FileUtil.getEncodedFileName("job-" + jobId + ".tar");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(response.getOutputStream())) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            for (int i = 0; i < runnerMapVoList.size(); i++) {
                RunnerMapVo runnerMapVo = runnerMapVoList.get(i);
                String url = String.format("%s/api/binary/job/data/download", runnerMapVo.getUrl());
                // 每次从一个runner下载数据
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                String result = HttpRequestUtil.download(url, "POST", UserContext.get().getResponse().getOutputStream()).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest().getError();
                if (StringUtils.isNotBlank(result)) {
                    throw new RunnerHttpRequestException(url + ":" + result);
                }

                // 将每个 runner 的内容加到 tar 里
                byte[] data = baos.toByteArray();
                TarArchiveEntry entry = new TarArchiveEntry(runnerMapVo.getHost() + (runnerMapVo.getPort() == null ? "" : "-" + runnerMapVo.getPort()) + ".tar");
                entry.setSize(data.length);
                tarOut.putArchiveEntry(entry);
                tarOut.write(data);
                tarOut.closeArchiveEntry();
            }
            tarOut.finish();
        }


        return null;
    }

}
