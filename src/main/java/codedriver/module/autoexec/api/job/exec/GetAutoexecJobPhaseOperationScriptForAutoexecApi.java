/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobSource;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseOperationNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseOperationScriptForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Override
    public String getToken() {
        return "autoexec/job/phase/operation/script/get/forautoexec";
    }

    @Override
    public String getName() {
        return "获取作业剧本操作脚本内容";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "operationId", type = ApiParamType.STRING, desc = "作业操作id（opName_opId）", isRequired = true),
            @Param(name = "lastModified", type = ApiParamType.DOUBLE, desc = "最后修改时间（秒，支持小数位）")
    })
    @Output({
            @Param(name = "script", type = ApiParamType.STRING, desc = "脚本内容")
    })
    @Description(desc = "获取操作当前激活版本脚本内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        String operationId = jsonObj.getString("operationId");
        Long jobId = jsonObj.getLong("jobId");

        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        Long opId = Long.valueOf(operationId.substring(operationId.lastIndexOf("_") + 1));
        AutoexecJobPhaseOperationVo jobPhaseOperationVo = autoexecJobMapper.getJobPhaseOperationByOperationId(opId);
        if (jobPhaseOperationVo == null) {
            throw new AutoexecJobPhaseOperationNotFoundException(opId.toString());
        }


        AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getVersionByVersionId(jobPhaseOperationVo.getVersionId());
        if (scriptVersionVo == null) {
            throw new AutoexecScriptNotFoundException(jobPhaseOperationVo.getName() + ":" + jobPhaseOperationVo.getVersionId());
        }
        //如果不是测试作业 则获取最新版本的脚本
        if (!Objects.equals(JobSource.TEST.getValue(), jobVo.getSource())) {
            scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(scriptVersionVo.getScriptId());
            if (scriptVersionVo == null) {
                throw new AutoexecScriptVersionHasNoActivedException(jobPhaseOperationVo.getName());
            }
        }
        BigDecimal lastModified = null;
        if (jsonObj.getDouble("lastModified") != null) {
            lastModified = new BigDecimal(Double.toString(jsonObj.getDouble("lastModified")));
        }
        //获取脚本目录
        String scriptCatalog = "";
        AutoexecCatalogVo scriptCatalogVo = autoexecCatalogMapper.getAutoexecCatalogByScriptId(scriptVersionVo.getScriptId());
        if (scriptCatalogVo != null) {
            List<AutoexecCatalogVo> catalogVoList = autoexecCatalogMapper.getParentListAndSelfByLR(scriptCatalogVo.getLft(), scriptCatalogVo.getRht());
            if (CollectionUtils.isNotEmpty(catalogVoList)) {
                scriptCatalog = catalogVoList.stream().map(AutoexecCatalogVo::getName).collect(Collectors.joining(File.separator));
            }
        }
        HttpServletResponse resp = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
        if (resp != null) {
            resp.setHeader("ScriptCatalog", scriptCatalog);
            resp.setHeader("ScriptId", scriptVersionVo.getScriptId().toString());
            resp.setHeader("ScriptVersionId", scriptVersionVo.getId().toString());
        }

        if (lastModified != null && lastModified.multiply(new BigDecimal("1000")).longValue() >= scriptVersionVo.getLcd().getTime()) {
            if (resp != null) {
                resp.setStatus(205);
                resp.getWriter().print(StringUtils.EMPTY);
            }
        } else {
            //获取脚本内容
            String script = autoexecCombopService.getScriptVersionContent(scriptVersionVo);
            result.put("script", script);
            result.put("scriptCatalog", scriptCatalog);
            result.put("scriptId", scriptVersionVo.getScriptId());
            result.put("scriptVersionId", scriptVersionVo.getId());
            //update job 对应operation version_id
            if (!Objects.equals(jobPhaseOperationVo.getVersionId(), scriptVersionVo.getId())) {
                autoexecJobMapper.updateJobPhaseOperationVersionIdByJobIdAndOperationId(scriptVersionVo.getId(), jobId, scriptVersionVo.getScriptId());
            }
        }
        return result;


    }


}
