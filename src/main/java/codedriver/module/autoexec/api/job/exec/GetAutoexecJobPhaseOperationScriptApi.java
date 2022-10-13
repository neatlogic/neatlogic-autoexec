/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseOperationVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseOperationNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionHasNoActivedException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAutoexecJobPhaseOperationScriptApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getToken() {
        return "autoexec/job/phase/operation/script/get";
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
            @Param(name = "operationId", type = ApiParamType.STRING, desc = "作业操作id（opName_opId）", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否获取最新版本的脚本（1：是，0：不是，默认是）"),
    })
    @Output({
            @Param(name = "lineList", type = ApiParamType.JSONARRAY, desc = "脚本内容行列表")
    })
    @Description(desc = "获取作业剧本操作脚本内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long opId = jsonObj.getLong("operationId");
        AutoexecJobPhaseOperationVo jobPhaseOperationVo = autoexecJobMapper.getJobPhaseOperationByOperationId(opId);
        if (jobPhaseOperationVo == null) {
            throw new AutoexecJobPhaseOperationNotFoundException(opId.toString());
        }

        if ((jsonObj.getInteger("isActive") != null ? jsonObj.getInteger("isActive") : 1) == 1) {

            //获取最新版本的脚本
            AutoexecScriptVersionVo scriptVersionVoOld = autoexecScriptMapper.getVersionByVersionId(jobPhaseOperationVo.getVersionId());
            if (scriptVersionVoOld == null) {
                throw new AutoexecScriptNotFoundException(jobPhaseOperationVo.getName() + ":" + jobPhaseOperationVo.getVersionId());
            }
            AutoexecScriptVersionVo scriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(scriptVersionVoOld.getScriptId());
            if (scriptVersionVo == null) {
                throw new AutoexecScriptVersionHasNoActivedException(jobPhaseOperationVo.getName());
            }
            return autoexecScriptMapper.getLineListByVersionId(scriptVersionVo.getId());
        } else {
            //获取对应的版本脚本
            return autoexecScriptMapper.getLineListByVersionId(jobPhaseOperationVo.getVersionId());
        }
    }
}
