/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.FileUtil;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseOperationScriptBinaryForAutoexecApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "autoexec/job/phase/operation/script/get/forautoexec";
    }

    @Override
    public String getName() {
        return "获取作业剧本操作脚本内容流";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "operationId", type = ApiParamType.STRING, desc = "作业操作id（opName_opId）"),
            @Param(name = "scriptId", type = ApiParamType.LONG, desc = "工具id"),
            @Param(name = "lastModified", type = ApiParamType.DOUBLE, desc = "最后修改时间（秒，支持小数位）"),
            @Param(name = "acceptStream", type = ApiParamType.BOOLEAN, desc = "True: 返回流,false 返回json，默认True")
    })
    @Output({
            @Param(name = "script", type = ApiParamType.STRING, desc = "脚本内容")
    })
    @Description(desc = "获取操作当前激活版本脚本内容流")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject result = new JSONObject();
        String operationId = jsonObj.getString("operationId");
        Long scriptId = jsonObj.getLong("scriptId");
        Long jobId = jsonObj.getLong("jobId");
        boolean acceptStream = jsonObj.getBoolean("acceptStream") == null || jsonObj.getBoolean("acceptStream");

        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecScriptVo scriptVo = null;
        AutoexecScriptVersionVo scriptVersionVo = null;
        if (StringUtils.isNotBlank(operationId) && !Objects.equals(operationId, "None")) {
            Long opId = Long.valueOf(operationId.substring(operationId.lastIndexOf("_") + 1));
            AutoexecJobPhaseOperationVo jobPhaseOperationVo = autoexecJobMapper.getJobPhaseOperationByOperationId(opId);
            if (jobPhaseOperationVo == null) {
                throw new AutoexecJobPhaseOperationNotFoundException(opId.toString());
            }
            scriptVo = autoexecScriptMapper.getScriptByVersionId(jobPhaseOperationVo.getVersionId());
            if (scriptVo == null) {
                throw new AutoexecScriptNotFoundException(scriptId);
            }
            //如果不是测试作业 则获取最新版本的脚本
            if (!Objects.equals(JobSource.TEST.getValue(), jobVo.getSource()) && !Objects.equals(JobSource.SCRIPT_TEST.getValue(), jobVo.getSource())) {
                scriptVersionVo = autoexecScriptMapper.getActiveVersionWithUseLibsByScriptId(scriptVo.getId());
                if (scriptVersionVo == null) {
                    throw new AutoexecScriptVersionHasNoActivedException(scriptVo.getName());
                }
                //update job 对应operation version_id
                if (!Objects.equals(jobPhaseOperationVo.getVersionId(), scriptVersionVo.getId())) {
                    autoexecJobMapper.updateJobPhaseOperationVersionIdByJobIdAndOperationId(scriptVersionVo.getId(), jobId, scriptVersionVo.getScriptId());
                }
            } else {
                scriptVersionVo = autoexecScriptMapper.getVersionWithUseLibByVersionId(jobPhaseOperationVo.getVersionId());
                if (scriptVersionVo == null) {
                    throw new AutoexecScriptVersionNotFoundException(jobPhaseOperationVo.getName() + ":" + jobPhaseOperationVo.getVersionId());
                }
            }
        } else if (scriptId != null) {
            scriptVo = autoexecScriptMapper.getScriptBaseInfoById(scriptId);
            if (scriptVo == null) {
                throw new AutoexecScriptNotFoundException(scriptId);
            }
            scriptVersionVo = autoexecScriptMapper.getActiveVersionWithUseLibsByScriptId(scriptId);
            if (scriptVersionVo == null) {
                throw new AutoexecScriptHasNoActiveVersionException(scriptVo.getName());
            }
        } else {
            throw new ParamIrregularException("operationId | scriptId");
        }

        BigDecimal lastModified = null;
        if (jsonObj.getDouble("lastModified") != null) {
            lastModified = new BigDecimal(Double.toString(jsonObj.getDouble("lastModified")));
        }
        //获取脚本目录
        String scriptCatalog = "";
        AutoexecCatalogVo scriptCatalogVo = autoexecCatalogMapper.getAutoexecCatalogByScriptId(scriptVo.getId());
        if (scriptCatalogVo != null) {
            List<AutoexecCatalogVo> catalogVoList = autoexecCatalogMapper.getParentListAndSelfByLR(scriptCatalogVo.getLft(), scriptCatalogVo.getRht());
            if (CollectionUtils.isNotEmpty(catalogVoList)) {
                scriptCatalog = catalogVoList.stream().map(AutoexecCatalogVo::getName).collect(Collectors.joining(File.separator));
            }
        }
        //查询是否被依赖
        List<Long> libScriptIdList = autoexecScriptMapper.getScriptVersionIdListByLibScriptId(scriptVo.getId());
        if (response != null) {
            response.setHeader("ScriptCatalog", scriptCatalog);
            response.setHeader("ScriptId", scriptVo.getId().toString());
            response.setHeader("ScriptName", scriptVo.getName());
            response.setHeader("ScriptVersionId", scriptVersionVo.getId().toString());
            response.setHeader("ScriptInterpreter", scriptVersionVo.getParser());
            response.setHeader("ScriptIsLib", CollectionUtils.isEmpty(libScriptIdList) ? "0" : "1");
            response.setHeader("ScriptUseLibs", scriptVersionVo.getUseLib().toString());
            if (lastModified != null && lastModified.multiply(new BigDecimal("1000")).longValue() >= scriptVersionVo.getLcd().getTime()) {
                response.setStatus(205);
                response.getWriter().print(StringUtils.EMPTY);
            } else {
                //获取脚本内容
                if (acceptStream || scriptVersionVo.getPackageFileId() != null) {
                    InputStream in;
                    if (scriptVersionVo.getPackageFileId() != null) {
                        FileVo fileVo = fileMapper.getFileById(scriptVersionVo.getPackageFileId());
                        if (fileVo == null) {
                            throw new FileNotFoundException(scriptVersionVo.getPackageFileId().toString());
                        }
                        in = FileUtil.getData(fileVo.getPath());
                    } else {
                        String script = autoexecCombopService.getScriptVersionContent(scriptVersionVo);
                        in = IOUtils.toInputStream(script, StandardCharsets.UTF_8);
                    }
                    response.setContentType("application/octet-stream");
                    response.setHeader("Content-Disposition", " attachment; filename=\"" + neatlogic.framework.util.FileUtil.getEncodedFileName(scriptVo.getName()) + "\"");

                    OutputStream os = response.getOutputStream();
                    IOUtils.copyLarge(in, os);
                    os.flush();
                    os.close();
                    in.close();
                } else {
                    String script = autoexecCombopService.getScriptVersionContent(scriptVersionVo);
                    result.put("script", script);
                    result.put("scriptCatalog", scriptCatalog);
                    result.put("scriptId", scriptVersionVo.getScriptId());
                    result.put("scriptName", scriptVo.getName());
                    result.put("scriptVersionId", scriptVersionVo.getId());
                    result.put("scriptInterpreter", scriptVersionVo.getParser());
                    result.put("scriptIsLib", CollectionUtils.isEmpty(libScriptIdList) ? "0" : "1");
                    result.put("scriptUseLibs", scriptVersionVo.getUseLib());
                }
            }
        }
        return result;
    }
}
