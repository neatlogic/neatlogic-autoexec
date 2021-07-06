/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.action;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecCombopCannotExecuteException;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobThreadCountException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import codedriver.module.autoexec.service.AutoexecJobActionService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Transactional
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecJobFromCombopCreateApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

    @Resource
    AutoexecJobActionService autoexecJobActionService;

    @Resource
    TeamMapper teamMapper;

    @Override
    public String getName() {
        return "作业创建（来自组合工具）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具ID"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "source", type = ApiParamType.STRING, isRequired = true, desc = "来源 itsm|human   ITSM|人工发起的等，不传默认是人工发起的"),
            @Param(name = "threadCount", type = ApiParamType.LONG, isRequired = true, desc = "并发线程,2的n次方 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT,  desc = "执行目标"),
    })
    @Output({
    })
    @Description(desc = "作业创建（来自组合工具）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        Integer threadCount = jsonObj.getInteger("threadCount");
        JSONObject paramJson = jsonObj.getJSONObject("param");
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        //作业执行权限校验
        autoexecCombopService.setOperableButtonList(combopVo);
        if(combopVo.getExecutable() != 1){
            throw new AutoexecCombopCannotExecuteException(combopVo.getName());
        }
        //设置作业执行节点
        if(combopVo.getConfig() != null && jsonObj.containsKey("executeConfig")){
            AutoexecCombopExecuteConfigVo executeConfigVo = JSON.toJavaObject(jsonObj.getJSONObject("executeConfig"), AutoexecCombopExecuteConfigVo.class);
            combopVo.getConfig().setExecuteConfig(executeConfigVo);
        }
        autoexecCombopService.verifyAutoexecCombopConfig(combopVo);
        //TODO 校验执行参数

        //并发数必须是2的n次方
        if ((threadCount & (threadCount - 1)) != 0) {
            throw new AutoexecJobThreadCountException();
        }
        AutoexecJobVo jobVo = autoexecJobService.saveAutoexecCombopJob(combopVo, jsonObj.getString("source"), threadCount, paramJson);
        jobVo.setAction(JobAction.FIRE.getValue());
        autoexecJobActionService.fire(jobVo);
        return new JSONObject(){{
            put("jobId",jobVo.getId());
        }};
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/create";
    }
}
