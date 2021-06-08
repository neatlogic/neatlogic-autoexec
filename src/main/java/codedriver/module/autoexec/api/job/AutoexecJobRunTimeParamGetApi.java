/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobParamContentVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.core.AutoexecJobAuthActionManager;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/6/8 16:00
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobRunTimeParamGetApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    AutoexecJobAuthActionManager autoexecJobAuthActionManager;

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "获取作业运行参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
    })
    @Output({

    })
    @Description(desc = "获取作业运行参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if(jobVo == null){
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        AutoexecJobParamContentVo paramContentVo = autoexecJobMapper.getJobParamContent(jobVo.getParamHash());
        JSONObject paramJson = new JSONObject();
        if(StringUtils.isNotBlank(paramContentVo.getContent())){
            paramJson = JSONObject.parseObject(paramContentVo.getContent());
        }
        //运行变量
        JSONArray runTimeParamArray = new JSONArray();
        if(Objects.equals(jobVo.getOperationType(), CombopOperationType.COMBOP.getValue())) {
            List<AutoexecCombopParamVo> combopParamVos = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(jobVo.getOperationId());
            for(AutoexecCombopParamVo combopParamVo : combopParamVos){
                JSONObject combopParamJson = JSONObject.parseObject(JSONObject.toJSON(combopParamVo).toString());
                String value = paramJson.getString(combopParamJson.getString("key"));
                combopParamJson.put("value", value);
                runTimeParamArray.add(combopParamJson);
            }
        }else{
            //TODO 测试生成作业场景
        }
        result.put("runTimeParamList",runTimeParamArray);
        //TODO 环境变量
        JSONArray environmentParamArray = new JSONArray();
        result.put("runTimeParamList", CollectionUtils.EMPTY_COLLECTION);
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/runtime/param/get";
    }
}
