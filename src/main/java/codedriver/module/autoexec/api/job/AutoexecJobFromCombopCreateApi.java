/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopCannotExecuteException;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecCombopOperationNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobThreadCountException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Transactional
@Service
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecJobFromCombopCreateApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecCombopService autoexecCombopService;

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
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "运行参数"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "来源 itsm|human   ITSM|人工发起的等，不传默认是人工发起的"),
            @Param(name = "threadCount", type = ApiParamType.LONG, isRequired = true, desc = "并发线程,2的n次方 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行目标"),
    })
    @Output({
    })
    @Description(desc = "作业创建（来自组合工具）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        String operationType = jsonObj.getString("operationType");
        Integer threadCount = jsonObj.getInteger("threadCount");
        if(autoexecCombopMapper.checkAutoexecCombopIsExists(combopId) == 0 ){
            throw new AutoexecCombopNotFoundException(combopId);
        }
        if (StringUtils.isBlank(CombopOperationType.getText(operationType))) {
            throw new AutoexecCombopOperationNotFoundException(operationType);
        }
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        autoexecCombopService.setOperableButtonList(combopVo);
        if(combopVo.getEditable() == 0){
            throw new AutoexecCombopCannotExecuteException(combopVo.getName());
        }
        //并发数必须是2的n次方
        if ((threadCount & (threadCount - 1)) != 0) {
            throw new AutoexecJobThreadCountException();
        }
        autoexecJobService.saveAutoexecCombopJob(combopVo, jsonObj.getString("source"), threadCount, jsonObj.getJSONObject("jobParam"));
        return null;
    }

    @Override
    public String getToken() {
        return "/autoexec/job/from/combop/create";
    }
}
