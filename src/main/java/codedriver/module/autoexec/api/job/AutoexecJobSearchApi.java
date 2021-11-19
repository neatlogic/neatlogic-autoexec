/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lvzk
 * @since 2021/4/12 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobSearchApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    AutoexecScriptMapper autoexecScriptMapper;
    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getName() {
        return "作业搜索（作业执行列表）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "作业来源"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "组合工具类型"),
            @Param(name = "combopName", type = ApiParamType.STRING, desc = "组合工具"),
            @Param(name = "combopId", type = ApiParamType.LONG, desc = "组合工具Id"),
            @Param(name = "scheduleId", type = ApiParamType.LONG, desc = "组合工具定时作业Id"),
            @Param(name = "startTime", type = ApiParamType.JSONOBJECT, desc = "时间过滤"),
            @Param(name = "execUserList", type = ApiParamType.JSONARRAY, desc = "操作人"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "作业搜索（作业执行视图）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecJobVo jobVo = new AutoexecJobVo(jsonObj);
        List<AutoexecJobVo> jobVoList = new ArrayList<>();
        int rowNum = autoexecJobMapper.searchJobCount(jobVo);
        if (rowNum > 0) {
            jobVo.setRowNum(rowNum);
            List<Long> jobIdList = autoexecJobMapper.searchJobId(jobVo);
            if (CollectionUtils.isNotEmpty(jobIdList)) {
                Map<String,ArrayList<Long>> operationIdMap = new HashMap<>();
                jobVoList = autoexecJobMapper.searchJob(jobIdList);
                //补充来源operation信息
                Map<Long,String> operationIdNameMap = new HashMap<>();
                List<AutoexecCombopVo> combopVoList;
                List<AutoexecScriptVersionVo> scriptVoList;
                List<AutoexecToolAndScriptVo> toolVoList;
                operationIdMap.put(CombopOperationType.COMBOP.getValue(),new ArrayList<>());
                operationIdMap.put(CombopOperationType.SCRIPT.getValue(),new ArrayList<>());
                operationIdMap.put(CombopOperationType.TOOL.getValue(),new ArrayList<>());
                jobVoList.forEach(o->{
                    operationIdMap.get(o.getOperationType()).add(o.getOperationId());
                });
                if(CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.COMBOP.getValue()))){
                    combopVoList = autoexecCombopMapper.getAutoexecCombopByIdList(operationIdMap.get(CombopOperationType.COMBOP.getValue()));
                    combopVoList.forEach(o-> operationIdNameMap.put(o.getId(),o.getName()));
                }
                if(CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.SCRIPT.getValue()))){
                    scriptVoList = autoexecScriptMapper.getVersionByVersionIdList(operationIdMap.get(CombopOperationType.SCRIPT.getValue()));
                    scriptVoList.forEach(o-> operationIdNameMap.put(o.getId(),o.getTitle()));
                }
                if(CollectionUtils.isNotEmpty(operationIdMap.get(CombopOperationType.TOOL.getValue()))){
                    toolVoList = autoexecToolMapper.getToolListByIdList(operationIdMap.get(CombopOperationType.TOOL.getValue()));
                    toolVoList.forEach(o-> operationIdNameMap.put(o.getId(),o.getName()));
                }
                jobVoList.forEach(o->{
                    o.setOperationName(operationIdNameMap.get(o.getOperationId()));
                });
                /*  jobVoList.forEach(j -> {
            //判断是否有编辑权限
            if(Objects.equals(j.getOperationType(), CombopOperationType.COMBOP.getValue())) {
                AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopById(j.getOperationId());
                if (combopVo == null) {
                    throw new AutoexecCombopNotFoundException(j.getOperationId());
                }
                autoexecCombopService.setOperableButtonList(combopVo);
                if (combopVo.getEditable() == 1) {
                    jobVo.setIsCanEdit(1);
                }
            }
        });*/
            }
        }
        return TableResultUtil.getResult(jobVoList, jobVo);
    }

    @Override
    public String getToken() {
        return "autoexec/job/search";
    }


}
