/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.job.exec;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.*;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobGroupVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.AutoexecJobGroupNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.SoftwareServiceOSVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Protocol;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.CacheControlType;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.common.util.RC4Util;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.inspect.constvalue.AutoexecType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

import static java.util.stream.Collectors.*;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadAutoexecJobPhaseNodesApi extends PrivateBinaryStreamApiComponentBase {
    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AutoexecCombopMapper combopMapper;

    @Resource
    private AutoexecScriptMapper scriptMapper;

    @Resource
    private AutoexecToolMapper toolMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

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

        AutoexecJobPhaseNodeVo nodeParamVo = new AutoexecJobPhaseNodeVo(jobId, phaseName, 0);
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId.toString());
        }
        if (Objects.equals(AutoexecJobPhaseNodeFrom.JOB.getValue(), nodeFrom)) {
            lncd = jobVo.getLncd();
            nodeParamVo.setUserNameFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            nodeParamVo.setProtocolFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
            nodeParamVo.setNodeFrom(AutoexecJobPhaseNodeFrom.JOB.getValue());
        } else if (Objects.equals(AutoexecJobPhaseNodeFrom.GROUP.getValue(), nodeFrom)) {
            if (groupSort == null) {
                throw new ParamIrregularException("groupNo");
            }
            nodeParamVo.setGroupSort(groupSort);
            AutoexecJobGroupVo jobGroupVo = autoexecJobMapper.getJobGroupByJobIdAndSort(jobId, groupSort);
            if (jobGroupVo == null) {
                throw new AutoexecJobGroupNotFoundException(jobId, groupSort);
            }
            List<AutoexecJobPhaseVo> jobPhaseVoList = autoexecJobMapper.getJobPhaseListByJobIdAndGroupSort(jobId, groupSort);
            if (CollectionUtils.isEmpty(jobPhaseVoList)) {
                throw new AutoexecJobPhaseNotFoundException(jobId, groupSort);
            }
            Optional<AutoexecJobPhaseVo> jobPhaseVoOptional = jobPhaseVoList.stream().filter(o -> !Arrays.asList(ExecMode.SQL.getValue(), ExecMode.RUNNER.getValue()).contains(o.getExecMode())).findFirst();
            //如果需要执行目标的阶段全部引用全局，则无需下载
            if (!jobPhaseVoOptional.isPresent() || Objects.equals(jobPhaseVoOptional.get().getNodeFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())
                    && Objects.equals(jobPhaseVoOptional.get().getProtocolFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())
                    && Objects.equals(jobPhaseVoOptional.get().getUserNameFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())
            ) {
                if (response != null) {
                    response.setStatus(204);
                    response.getWriter().print(StringUtils.EMPTY);
                }
                return null;
            }
            lncd = jobGroupVo.getLncd();
            jobVo.setExecuteJobGroupVo(jobGroupVo);
            nodeParamVo.setIsDownloadGroup(1);
        } else if (Objects.equals(AutoexecJobPhaseNodeFrom.PHASE.getValue(), nodeFrom)) {
            AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phaseName);
            if ((StringUtils.isBlank(phaseName) || jobPhaseVo == null)) {
                throw new ParamIrregularException("phase");
            }
            //如果全部引用全局,则无需下载
            if ((Objects.equals(jobPhaseVo.getNodeFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())
                    && Objects.equals(jobPhaseVo.getProtocolFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue())
                    && Objects.equals(jobPhaseVo.getUserNameFrom(), AutoexecJobPhaseNodeFrom.JOB.getValue()))
            ) {
                if (response != null) {
                    response.setStatus(204);
                    response.getWriter().print(StringUtils.EMPTY);
                }
                return null;
            }
            lncd = jobPhaseVo.getLncd();
            nodeParamVo.setJobPhaseName(jobPhaseVo.getName());
            jobVo.setExecuteJobPhaseList(Collections.singletonList(jobPhaseVo));
        }

        if (paramObj.getDouble("lastModified") != null) {
            BigDecimal lastModifiedDec = new BigDecimal(Double.toString(paramObj.getDouble("lastModified")));
            lastModifiedLong = lastModifiedDec.multiply(new BigDecimal("1000")).longValue();
        }
        nodeParamVo.setStatusBlackList(Collections.singletonList(JobNodeStatus.IGNORED.getValue()));

        //获取是不是巡检类型的作业
        boolean isInspect = isInspect(jobVo);

        /*
         * 以下场景会下载，否则无须重新下载
         * 1、lastModified 为 null
         * 2、lastModified小于最近一次节点变动时间(lncd)
         */
        if (lastModifiedLong == 0L || lncd == null || lastModifiedLong < lncd.getTime()) {
            IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            List<AccountProtocolVo> allProtocolList = resourceAccountCrossoverMapper.getAllAccountProtocolList();
            int count = autoexecJobMapper.searchJobPhaseNodeByDistinctResourceIdCount(nodeParamVo);
            int pageCount = PageUtil.getPageCount(count, nodeParamVo.getPageSize());
            nodeParamVo.setPageCount(pageCount);
            IResourceCenterAccountCrossoverService accountService = CrossoverServiceFactory.getApi(IResourceCenterAccountCrossoverService.class);
            //补充第一行数据 {"totalCount":14, "localRunnerId":1, "jobRunnerIds":[1]}
            //nodes.json 每个作业必须下载
            if (count != 0 || Objects.equals(AutoexecJobPhaseNodeFrom.JOB.getValue(), nodeFrom)) {
                ServletOutputStream os = response.getOutputStream();
                isNeedDownLoad = true;
                List<Long> runnerMapIdList = new ArrayList<>();
                if (Objects.equals(AutoexecJobPhaseNodeFrom.JOB.getValue(), nodeFrom)) {
                    runnerMapIdList = autoexecJobMapper.getJobRunnerMapIdListByJobId(nodeParamVo.getJobId());
                } else {
                    runnerMapIdList = autoexecJobMapper.getJobPhaseNodeRunnerMapIdListByNodeVo(nodeParamVo);
                }
                JSONObject firstRow = new JSONObject();
                firstRow.put("totalCount", count);
                firstRow.put("localRunnerId", jobVo.getRunnerMapId());
                firstRow.put("jobRunnerIds", runnerMapIdList);
                IOUtils.copyLarge(IOUtils.toInputStream(firstRow.toJSONString() + System.lineSeparator(), StandardCharsets.UTF_8), os);
                if (os != null) {
                    os.flush();
                }
                //循环分页输出节点流
                for (int i = 1; i <= pageCount; i++) {
                    Map<Long, JSONObject> resourceServicePortsMap = new HashMap<>();
                    Map<Long, List<Long>> resourceAppSystemMap = new HashMap<>();
                    List<AccountVo> accountByResourceList = new ArrayList<>();
                    Map<String, AccountVo> tagentIpAccountMap = new HashMap<>();
                    Map<Long, Long> resourceOSResourceMap = new HashMap<>();//节点resourceId->对应操作系统resourceId
                    nodeParamVo.setCurrentPage(i);
                    nodeParamVo.setStartNum(nodeParamVo.getStartNum());
                    List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeVoList = autoexecJobMapper.searchJobPhaseNodeByDistinct(nodeParamVo);
                    Long protocolId = autoexecJobPhaseNodeVoList.get(0).getProtocolId();
                    Optional<AccountProtocolVo> protocolVoOptional = allProtocolList.stream().filter(o -> Objects.equals(o.getId(), protocolId)).findFirst();
                    String protocol = null;
                    if (protocolVoOptional.isPresent()) {
                        protocol = protocolVoOptional.get().getName();
                    }
                    String account = autoexecJobPhaseNodeVoList.get(0).getUserName();
                    //批量根据协议查询默认端口
                    List<AccountVo> defaultAccountList;
                    Map<Long, AccountVo> protocolDefaultAccountMap = new HashMap<>();
                    if (CollectionUtils.isNotEmpty(allProtocolList)) {
                        defaultAccountList = resourceAccountCrossoverMapper.getDefaultAccountListByProtocolIdListAndAccount(allProtocolList.stream().map(AccountProtocolVo::getId).collect(Collectors.toList()), account);
                        if (CollectionUtils.isNotEmpty(defaultAccountList)) {
                            protocolDefaultAccountMap = defaultAccountList.stream().collect(toMap(AccountVo::getProtocolId, o -> o));
                        }
                    }
                    if (CollectionUtils.isNotEmpty(autoexecJobPhaseNodeVoList)) {
                        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
                        List<Long> resourceIdList = autoexecJobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).filter(Objects::nonNull).collect(Collectors.toList());
                        List<Long> resourceIncludeOsIdList = new ArrayList<>(resourceIdList);
                        //针对巡检 批量补充对应资产的appSystemId
                        if (CollectionUtils.isNotEmpty(resourceIdList)) {
                            if (isInspect) {
                                List<ResourceVo> ipObjectResourceList = resourceCrossoverMapper.getResourceListByIdList(resourceIdList);
                                if (CollectionUtils.isNotEmpty(ipObjectResourceList)) {
                                    resourceAppSystemMap.putAll(ipObjectResourceList.stream().filter(o -> o.getAppSystemId() != null).collect(Collectors.toMap(ResourceVo::getId, o -> {
                                        List<Long> appSystemIdList = new ArrayList<>();
                                        appSystemIdList.add(o.getAppSystemId());
                                        return appSystemIdList;
                                    }, (k1, k2) -> {
                                        k1.addAll(k2);
                                        return k1;
                                    })));
                                }
                                List<ResourceVo> osResourceList = resourceCrossoverMapper.getResourceAppSystemListByResourceIdList(resourceIdList);
                                if (CollectionUtils.isNotEmpty(osResourceList)) {
                                    osResourceList = osResourceList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getId() + ":" + o.getAppSystemId()))), ArrayList::new));
                                    resourceAppSystemMap.putAll(osResourceList.stream().filter(o -> o.getAppSystemId() != null).collect(Collectors.toMap(ResourceVo::getId, o -> {
                                        List<Long> appSystemIdList = new ArrayList<>();
                                        appSystemIdList.add(o.getAppSystemId());
                                        return appSystemIdList;
                                    }, (k1, k2) -> {
                                        k1.addAll(k2);
                                        return k1;
                                    })));
                                }

                            }
                            //查询target 对应的os
                            List<SoftwareServiceOSVo> targetOsList = resourceCrossoverMapper.getOsResourceListByResourceIdList(resourceIdList);
                            if (CollectionUtils.isNotEmpty(targetOsList)) {
                                resourceIncludeOsIdList.addAll(targetOsList.stream().map(SoftwareServiceOSVo::getOsId).collect(toList()));
                                resourceOSResourceMap = targetOsList.stream().collect(Collectors.toMap(SoftwareServiceOSVo::getResourceId, SoftwareServiceOSVo::getOsId));
                            }

                            //os、software补充listen_port
                            List<ResourceVo> osResourceList = resourceCrossoverMapper.getOsResourceListenPortListByResourceIdList(resourceIncludeOsIdList);
                            if (CollectionUtils.isNotEmpty(osResourceList)) {
                                resourceServicePortsMap.putAll(osResourceList.stream().filter(o -> o.getListenPort() != null).collect(Collectors.toMap(ResourceVo::getId, o -> {
                                    JSONObject servicePorts = new JSONObject();
                                    servicePorts.put(o.getName(), o.getListenPort());
                                    return servicePorts;
                                }, (k1, k2) -> {
                                    k1.putAll(k2);
                                    return k1;
                                })));
                            }
                            List<ResourceVo> softwareResourceList = resourceCrossoverMapper.getSoftwareResourceListenPortListByResourceIdList(resourceIdList);
                            if (CollectionUtils.isNotEmpty(softwareResourceList)) {
                                resourceServicePortsMap.putAll(softwareResourceList.stream().filter(o -> o.getListenPort() != null).collect(Collectors.toMap(ResourceVo::getId, o -> {
                                    JSONObject servicePorts = new JSONObject();
                                    servicePorts.put(o.getName(), o.getListenPort());
                                    return servicePorts;
                                }, (k1, k2) -> {
                                    k1.putAll(k2);
                                    return k1;
                                })));
                            }
                            if (!Objects.equals(protocol, Protocol.TAGENT.getValue())) {
                                accountByResourceList = resourceAccountCrossoverMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(resourceIncludeOsIdList, protocolId, account);
                            } else {
                                List<AccountVo> tagentAccountByIpList = resourceAccountCrossoverMapper.getAccountListByIpList(autoexecJobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getHost).collect(Collectors.toList()));
                                if (CollectionUtils.isNotEmpty(tagentAccountByIpList)) {
                                    tagentIpAccountMap = tagentAccountByIpList.stream().collect(Collectors.toMap(AccountVo::getIp, o -> o));
                                }
                            }
                        }
                        for (AutoexecJobPhaseNodeVo nodeVo : autoexecJobPhaseNodeVoList) {
                            JSONObject nodeJson = new JSONObject();
                            AccountProtocolVo protocolVo = new AccountProtocolVo(protocolId, protocol);
                            AccountVo accountVoTmp = accountService.filterAccountByRules(accountByResourceList, tagentIpAccountMap, nodeVo.getResourceId(), protocolVo, nodeVo.getHost(), resourceOSResourceMap, protocolDefaultAccountMap);
                            if (accountVoTmp != null) {
                                nodeJson.put("protocol", accountVoTmp.getProtocol());
                                nodeJson.put("password", RC4Util.encrypt(accountVoTmp.getPasswordPlain()));
                                nodeJson.put("protocolPort", accountVoTmp.getProtocolPort());
                            } else {
                                if (StringUtils.isNotBlank(protocolVo.getName())) {
                                    nodeJson.put("protocol", protocolVo.getName());
                                    nodeJson.put("protocolPort", protocolVo.getPort());
                                } else {
                                    nodeJson.put("protocol", "protocolNotExist");
                                }
                            }
                            nodeJson.put("username", account);
                            nodeJson.put("nodeName", nodeVo.getNodeName());
                            nodeJson.put("nodeType", nodeVo.getNodeType());
                            nodeJson.put("resourceId", nodeVo.getResourceId());
                            JSONObject servicePorts = resourceServicePortsMap.get(nodeVo.getResourceId());
                            Long osResourceId = resourceOSResourceMap.get(nodeVo.getResourceId());
                            if (osResourceId != null) {
                                JSONObject osServicePorts = resourceServicePortsMap.get(osResourceId);
                                if (MapUtils.isNotEmpty(osServicePorts)) {
                                    if (MapUtils.isEmpty(servicePorts)) {
                                        servicePorts = new JSONObject();
                                    }
                                    servicePorts.putAll(osServicePorts);
                                }
                            }
                            nodeJson.put("servicePorts", servicePorts);
                            nodeJson.put("host", nodeVo.getHost());
                            nodeJson.put("port", nodeVo.getPort());
                            nodeJson.put("runnerId", nodeVo.getRunnerMapId());
                            nodeJson.put("appSystemId", resourceAppSystemMap.get(nodeVo.getResourceId()));
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

    /**
     * 根据作业operationId 判读是不是巡检类型的作业
     *
     * @param jobVo 作业
     * @return 是｜否
     */
    private boolean isInspect(AutoexecJobVo jobVo) {
        boolean isInspect = false;
        Long typeId = null;
        if (Objects.equals(CombopOperationType.COMBOP.getValue(), jobVo.getOperationType())) {
            AutoexecCombopVo combopVo = combopMapper.getAutoexecCombopById(jobVo.getOperationId());
            if (combopVo != null) {
                typeId = combopVo.getTypeId();
            }
        } else if (Objects.equals(CombopOperationType.SCRIPT.getValue(), jobVo.getOperationType())) {
            AutoexecScriptVersionVo scriptVersionVo = scriptMapper.getVersionByVersionId(jobVo.getOperationId());
            if (scriptVersionVo != null) {
                AutoexecScriptVo scriptVo = scriptMapper.getScriptBaseInfoById(scriptVersionVo.getScriptId());
                if (scriptVo != null) {
                    typeId = scriptVo.getTypeId();
                }
            }
        } else if (Objects.equals(CombopOperationType.TOOL.getValue(), jobVo.getOperationType())) {
            AutoexecToolVo toolVo = toolMapper.getToolById(jobVo.getOperationId());
            if (toolVo != null) {
                typeId = toolVo.getTypeId();
            }
        }
        if (typeId != null) {
            AutoexecTypeVo typeVo = autoexecTypeMapper.getTypeById(typeId);
            if (typeVo != null && Objects.equals(AutoexecType.INSPECT.getValue(), typeVo.getName())) {
                isInspect = true;
            }
        }
        return isInspect;
    }
}
