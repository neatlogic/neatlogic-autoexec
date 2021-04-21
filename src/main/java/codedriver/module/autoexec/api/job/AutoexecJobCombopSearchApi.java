/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/13 10:20
 **/

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobCombopSearchApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Override
    public String getName() {
        return "作业搜索（组合工具视图）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "作业来源"),
            @Param(name = "combopOperationTypeList", type = ApiParamType.JSONARRAY, desc = "组合工具类型"),
            @Param(name = "combopName", type = ApiParamType.STRING, desc = "组合工具"),
            @Param(name = "startTime", type = ApiParamType.JSONOBJECT, desc = "时间过滤"),
            @Param(name = "execUser", type = ApiParamType.STRING, desc = "操作人"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "作业搜索（组合工具视图）")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecJobVo jobVo = new AutoexecJobVo(jsonObj);
        List<AutoexecCombopVo> combopVoList = autoexecJobMapper.searchJobWithCombopView(jobVo);
        result.put("tbodyList", combopVoList);
        if (jobVo.getNeedPage()) {
            int rowNum = autoexecJobMapper.searchJobCount(jobVo);
            jobVo.setRowNum(rowNum);
            result.put("currentPage", jobVo.getCurrentPage());
            result.put("pageSize", jobVo.getPageSize());
            result.put("pageCount", jobVo.getPageCount());
            result.put("rowNum", jobVo.getRowNum());
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/combop/search";
    }
}
