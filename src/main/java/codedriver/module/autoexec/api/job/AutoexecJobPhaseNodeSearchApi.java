/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeSearchApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "作业剧本节点搜索";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobPhaseId", type = ApiParamType.LONG, desc = "作业剧本id", isRequired = true),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词(节点名称或ip)", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobPhaseNodeVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "作业剧本节点搜索")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        /*AutoexecJobPhaseNodeVo jobPhaseNodeVo = JSONObject.toJavaObject(jsonObj, AutoexecJobPhaseNodeVo.class);
        List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = autoexecJobMapper.searchJobPhaseNode(jobPhaseNodeVo);
        result.put("tbodyList", jobPhaseNodeVoList);
        if (jobPhaseNodeVo.getNeedPage()) {
            int rowNum = autoexecJobMapper.searchJobPhaseNodeCount(jobPhaseNodeVo);
            jobPhaseNodeVo.setRowNum(rowNum);
            result.put("currentPage", jobPhaseNodeVo.getCurrentPage());
            result.put("pageSize", jobPhaseNodeVo.getPageSize());
            result.put("pageCount", PageUtil.getPageCount(rowNum, jobPhaseNodeVo.getPageSize()));
            result.put("rowNum", jobPhaseNodeVo.getRowNum());
        }*/
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/search";
    }


}
