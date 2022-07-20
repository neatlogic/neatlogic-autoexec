/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job.exec;

import codedriver.framework.autoexec.constvalue.AutoexecJobPhaseNodeFrom;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobGroupVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobGroupNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobNotFoundException;
import codedriver.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.enums.resourcecenter.Protocol;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CacheControlType;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadAutoexecJobPhaseNodesApi extends PrivateBinaryStreamApiComponentBase {
    @Autowired
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "autoexec/job/phase/nodes/download";
    }

    @Override
    public String getName() {
        return "下载作业剧本节点";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @CacheControl(cacheControlType = CacheControlType.MAXAGE, maxAge = 30000)
    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "阶段"),
            @Param(name = "nodeFrom", type = ApiParamType.STRING, desc = "节点来源 job｜group｜phase", isRequired = true),
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "runner id", isRequired = true),
            @Param(name = "groupNo", type = ApiParamType.INTEGER, desc = "组No"),
            @Param(name = "lastModified", type = ApiParamType.DOUBLE, desc = "最后修改时间（秒，支持小数位）", isRequired = true),
            @Param(name = "passThroughEnv", type = ApiParamType.JSONOBJECT, desc = "返回参数")
    })
    @Description(desc = "下载作业剧本节点")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        Long localRunnerId = paramObj.getLong("runnerId");
        Integer groupSort = paramObj.getInteger("groupNo");
        String phaseName = paramObj.getString("phase");
        String nodeFrom = paramObj.getString("nodeFrom");
        boolean isNeedDownLoad = false;
        long lastModifiedLong = 0L;
        Date lncd = null;
        AutoexecJobPhaseNodeVo nodeParamVo = new AutoexecJobPhaseNodeVo(jobId, phaseName, 0, nodeFrom);
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        if (Objects.equals(AutoexecJobPhaseNodeFrom.JOB.getValue(), nodeFrom)) {
            lncd = jobVo.getLncd();
        } else if (Objects.equals(AutoexecJobPhaseNodeFrom.GROUP.getValue(), nodeFrom)) {
            if (groupSort == null) {
                throw new ParamIrregularException("groupNo");
            }
            nodeParamVo.setGroupSort(groupSort);
            AutoexecJobGroupVo jobGroupVo = autoexecJobMapper.getJobGroupByJobIdAndSort(jobId, groupSort);
            if (jobGroupVo == null) {
                throw new AutoexecJobGroupNotFoundException(jobId, groupSort);
            }
            lncd = jobGroupVo.getLncd();
        } else if (Objects.equals(AutoexecJobPhaseNodeFrom.PHASE.getValue(), nodeFrom)) {
            AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phaseName);
            if ((StringUtils.isBlank(phaseName) || jobPhaseVo == null)) {
                throw new ParamIrregularException("phase");
            }
            lncd = jobPhaseVo.getLncd();
            nodeParamVo.setJobPhaseName(jobPhaseVo.getName());
        }

        if (paramObj.getDouble("lastModified") != null) {
            BigDecimal lastModifiedDec = new BigDecimal(Double.toString(paramObj.getDouble("lastModified")));
            lastModifiedLong = lastModifiedDec.multiply(new BigDecimal("1000")).longValue();
        }
        nodeParamVo.setStatusBlackList(Collections.singletonList(JobNodeStatus.IGNORED.getValue()));
        nodeParamVo.setNodeFrom(nodeFrom);

        /*
         * 以下场景会下载，否则无须重新下载
         * 1、lastModified 为 null
         * 2、lastModified小于最近一次节点变动时间(lncd)
         */
        if (lastModifiedLong == 0L || lncd == null || lastModifiedLong < lncd.getTime()) {
            int count = autoexecJobMapper.searchJobPhaseNodeByDistinctResourceIdCount(nodeParamVo);
            int pageCount = PageUtil.getPageCount(count, nodeParamVo.getPageSize());
            nodeParamVo.setPageCount(pageCount);
            List<AccountProtocolVo> protocolVoList = resourceCenterMapper.searchAccountProtocolListByProtocolName(new AccountProtocolVo());
            IResourceCenterAccountCrossoverService accountService = CrossoverServiceFactory.getApi(IResourceCenterAccountCrossoverService.class);
            //补充第一行数据 {"totalCount":14, "localRunnerId":1, "jobRunnerIds":[1]}
            if(count != 0) {
                ServletOutputStream os = response.getOutputStream();
                isNeedDownLoad = true;
                List<Long> runnerMapIdList = autoexecJobMapper.getJobPhaseNodeRunnerMapIdListByNodeVo(nodeParamVo);
                JSONObject firstRow = new JSONObject();
                firstRow.put("totalCount", count);
                firstRow.put("localRunnerId", localRunnerId);
                firstRow.put("jobRunnerIds", runnerMapIdList);
                IOUtils.copyLarge(IOUtils.toInputStream(firstRow.toJSONString() + System.lineSeparator(), StandardCharsets.UTF_8), os);
                if (os != null) {
                    os.flush();
                }
                //循环分页输出节点流
                for (int i = 1; i <= pageCount; i++) {
                    nodeParamVo.setCurrentPage(i);
                    nodeParamVo.setStartNum(nodeParamVo.getStartNum());
                    List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeVoList = autoexecJobMapper.searchJobPhaseNodeByDistinct(nodeParamVo);
                    if (CollectionUtils.isNotEmpty(autoexecJobPhaseNodeVoList)) {
                        //补充执行用户和账号信息（用户、密码）
                        Long protocolId = autoexecJobPhaseNodeVoList.get(0).getProtocolId();
                        String userName = autoexecJobPhaseNodeVoList.get(0).getUserName();
                        List<Long> resourceIdList = autoexecJobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).collect(Collectors.toList());
                        //List<ResourceVo> resourceVoList = resourceCenterMapper.getResourceListByIdList(resourceIdList, TenantContext.get().getDataDbName());
                        if (protocolVoList.stream().anyMatch(o -> Objects.equals(o.getId(), protocolId) && Objects.equals(o.getName(), Protocol.TAGENT.getValue()))) {
                            userName = null;
                        }
                        List<AccountVo> accountVoList = resourceCenterMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(resourceIdList, protocolId, userName);
                        List<AccountVo> allAccountVoList = resourceCenterMapper.getAccountListByIpList(autoexecJobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getHost).collect(Collectors.toList()));
                        String finalUserName = autoexecJobPhaseNodeVoList.get(0).getUserName();
                        for (int j = 0; j < autoexecJobPhaseNodeVoList.size(); j++) {
                            AutoexecJobPhaseNodeVo nodeVo = autoexecJobPhaseNodeVoList.get(j);
                            JSONObject nodeJson = new JSONObject();
                            Optional<AccountVo> accountOp = accountService.filterAccountByRules(accountVoList, allAccountVoList, protocolVoList, nodeVo.getResourceId(), nodeVo.getProtocolId(), nodeVo.getHost(), nodeVo.getPort());
                            if (accountOp.isPresent()) {
                                AccountVo accountVoTmp = accountOp.get();
                                nodeJson.put("protocol", accountVoTmp.getProtocol());
                                nodeJson.put("password", RC4Util.encrypt(accountVoTmp.getPasswordPlain()));
                                nodeJson.put("protocolPort", accountVoTmp.getProtocolPort());
                            } else {
                                Optional<AccountProtocolVo> protocolVo = protocolVoList.stream().filter(o -> Objects.equals(o.getId(), nodeVo.getProtocolId())).findFirst();
                                if (protocolVo.isPresent()) {
                                    nodeJson.put("protocol", protocolVo.get().getName());
                                    nodeJson.put("protocolPort", protocolVo.get().getPort());
                                } else {
                                    nodeJson.put("protocol", "protocolNotExist");
                                }
                            }
                            nodeJson.put("username", finalUserName);
                            nodeJson.put("nodeName", nodeVo.getNodeName());
                            nodeJson.put("nodeType", nodeVo.getNodeType());
                            nodeJson.put("resourceId", nodeVo.getResourceId());
                            nodeJson.put("host", nodeVo.getHost());
                            nodeJson.put("port", nodeVo.getPort());
                            nodeJson.put("runnerId", nodeVo.getRunnerMapId());
                            response.setContentType("application/json");
                            response.setHeader("Content-Disposition", " attachment; filename=nodes.json");
                            IOUtils.copyLarge(IOUtils.toInputStream(nodeJson.toJSONString() + System.lineSeparator(), StandardCharsets.UTF_8), os);
                            if (os != null) {
                                os.flush();
                            }
                        }
                    }
                }
                if (os != null) {
                    os.close();
                }
            }
        }

        if (!isNeedDownLoad) {
            if (response != null) {
                response.setStatus(204);
                response.getWriter().print(StringUtils.EMPTY);
            }
        }
        return null;
    }

}
