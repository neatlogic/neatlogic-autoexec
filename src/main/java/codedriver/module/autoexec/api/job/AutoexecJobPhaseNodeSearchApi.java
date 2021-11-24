/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.autoexec.service.AutoexecJobService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/13 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseNodeSearchApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Resource
    ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "status", type = ApiParamType.STRING, isRequired = true, desc = "当前阶段状态"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobPhaseNodeVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class),
            @Param(name = "isRefresh", type = ApiParamType.INTEGER, isRequired = true, desc = "是否需要继续定时刷新，1:继续 0:停止")
    })
    @Description(desc = "作业剧本节点搜索")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecJobPhaseNodeVo jobPhaseNodeVo = JSONObject.toJavaObject(jsonObj, AutoexecJobPhaseNodeVo.class);
        AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseNodeVo.getJobPhaseId());
        if(phaseVo == null){
            throw new AutoexecJobPhaseNotFoundException(jobPhaseNodeVo.getJobPhaseId().toString());
        }
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(phaseVo.getJobId());
        if(jobVo == null){
            throw new AutoexecJobNotFoundException(phaseVo.getJobId().toString());
        }
        List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList = new ArrayList<>();
        /*String protocol = jobPhaseNodeVoList.get(0).getProtocol();
        String userName = jobPhaseNodeVoList.get(0).getUserName();
        List<Long> resourceIdList = jobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).collect(Collectors.toList());
            List<AccountVo> accountVoList = resourceCenterMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(resourceIdList, protocol,userName);
            jobPhaseNodeVoList.forEach(o->{
                List<AccountVo> accountVoTmpList = accountVoList.stream().filter(a-> Objects.equals(a.getResourceId(),o.getResourceId())).collect(Collectors.toList());
                if(CollectionUtils.isNotEmpty(accountVoTmpList)) {
                    o.setUserName(accountVoTmpList.get(0).getAccount());
                    o.setPassword(accountVoTmpList.get(0).getPasswordPlain());
                }
            });*/
        if (jobPhaseNodeVo.getNeedPage()) {
            int rowNum = autoexecJobMapper.searchJobPhaseNodeCount(jobPhaseNodeVo);
            jobPhaseNodeVo.setRowNum(rowNum);
            if(rowNum >0){
                jobPhaseNodeVoList =  autoexecJobMapper.searchJobPhaseNode(jobPhaseNodeVo);
            }
            result = TableResultUtil.getResult(jobPhaseNodeVoList,jobPhaseNodeVo);
        }
        //判断是否停止刷新作业详细
        autoexecJobService.setIsRefresh(result, jobVo,jsonObj.getString("status"));
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/node/search";
    }


}
