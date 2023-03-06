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
import neatlogic.framework.autoexec.constvalue.AutoexecJobPhaseNodeFrom;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dao.mapper.*;
import neatlogic.framework.autoexec.dto.AutoexecJobSourceVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopGroupVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.*;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerFactory;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;
import neatlogic.framework.autoexec.source.AutoexecJobSourceFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCenterAccountCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.SoftwareServiceOSVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Protocol;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
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
            jobVo.setExecuteJobGroupVo(jobGroupVo);
        } else if (Objects.equals(AutoexecJobPhaseNodeFrom.PHASE.getValue(), nodeFrom)) {
            AutoexecJobPhaseVo jobPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(jobId, phaseName);
            if ((StringUtils.isBlank(phaseName) || jobPhaseVo == null)) {
                throw new ParamIrregularException("phase");
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
        nodeParamVo.setNodeFrom(nodeFrom);
        //获取是不是巡检类型的作业
        boolean isInspect = isInspect(jobVo);

        /*
         * 以下场景会下载，否则无须重新下载
         * 1、lastModified 为 null
         * 2、lastModified小于最近一次节点变动时间(lncd)
         */
        if (lastModifiedLong == 0L || lncd == null || lastModifiedLong < lncd.getTime()) {
            AccountVo executeAccount = getProtocolAndUserName(jobVo, nodeFrom);
            if (executeAccount == null) {
                response.setStatus(204);
                response.getWriter().print(StringUtils.EMPTY);
                return null;
            }
            IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            List<AccountProtocolVo> allProtocolList = resourceAccountCrossoverMapper.getAllAccountProtocolList();
            if (allProtocolList.stream().noneMatch(o -> Objects.equals(o.getId(), executeAccount.getProtocolId()))) {
                throw new ResourceCenterAccountProtocolNotFoundException(executeAccount.getProtocolId());
            }
            //批量根据协议查询默认端口
            List<AccountVo> defaultAccountList;
            Map<Long, AccountVo> protocolDefaultAccountMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(allProtocolList)) {
                defaultAccountList = resourceAccountCrossoverMapper.getDefaultAccountListByProtocolIdListAndAccount(allProtocolList.stream().map(AccountProtocolVo::getId).collect(Collectors.toList()), executeAccount.getAccount());
                if (CollectionUtils.isNotEmpty(defaultAccountList)) {
                    protocolDefaultAccountMap = defaultAccountList.stream().collect(toMap(AccountVo::getProtocolId, o -> o));
                }
            }
            int count = autoexecJobMapper.searchJobPhaseNodeByDistinctResourceIdCount(nodeParamVo);
            int pageCount = PageUtil.getPageCount(count, nodeParamVo.getPageSize());
            nodeParamVo.setPageCount(pageCount);

            IResourceCenterAccountCrossoverService accountService = CrossoverServiceFactory.getApi(IResourceCenterAccountCrossoverService.class);
            //补充第一行数据 {"totalCount":14, "localRunnerId":1, "jobRunnerIds":[1]}
            if (count != 0) {
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
                    Map<Long, JSONObject> resourceServicePortsMap = new HashMap<>();
                    Map<Long, List<Long>> resourceAppSystemMap = new HashMap<>();
                    List<AccountVo> accountByResourceList = new ArrayList<>();
                    Map<String, AccountVo> tagentIpAccountMap = new HashMap<>();
                    Map<Long, Long> resourceOSResourceMap = new HashMap<>();//节点resourceId->对应操作系统resourceId
                    nodeParamVo.setCurrentPage(i);
                    nodeParamVo.setStartNum(nodeParamVo.getStartNum());
                    List<AutoexecJobPhaseNodeVo> autoexecJobPhaseNodeVoList = autoexecJobMapper.searchJobPhaseNodeByDistinct(nodeParamVo);
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
                            if (!Objects.equals(executeAccount.getProtocol(), Protocol.TAGENT.getValue())) {
                                accountByResourceList = resourceAccountCrossoverMapper.getResourceAccountListByResourceIdAndProtocolAndAccount(resourceIncludeOsIdList, executeAccount.getProtocolId(), executeAccount.getAccount());
                            } else {
                                List<AccountVo> tagentAccountByIpList = resourceAccountCrossoverMapper.getAccountListByIpList(autoexecJobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getHost).collect(Collectors.toList()));
                                if (CollectionUtils.isNotEmpty(tagentAccountByIpList)) {
                                    tagentIpAccountMap = tagentAccountByIpList.stream().collect(Collectors.toMap(AccountVo::getIp, o -> o));
                                }
                            }
                        }
                        String finalUserName = autoexecJobPhaseNodeVoList.get(0).getUserName();
                        for (AutoexecJobPhaseNodeVo nodeVo : autoexecJobPhaseNodeVoList) {
                            JSONObject nodeJson = new JSONObject();
                            AccountVo accountVoTmp = accountService.filterAccountByRules(accountByResourceList, tagentIpAccountMap, nodeVo.getResourceId(), executeAccount, nodeVo.getHost(), resourceOSResourceMap, protocolDefaultAccountMap);
                            if (accountVoTmp != null) {
                                nodeJson.put("protocol", accountVoTmp.getProtocol());
                                nodeJson.put("password", RC4Util.encrypt(accountVoTmp.getPasswordPlain()));
                                nodeJson.put("protocolPort", accountVoTmp.getProtocolPort());
                            } else {
                                Optional<AccountProtocolVo> protocolVo = allProtocolList.stream().filter(o -> Objects.equals(o.getId(), nodeVo.getProtocolId())).findFirst();
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
     * 根据节点来源获取对应的执行用户和执行协议
     *
     * @param jobVo    作业
     * @param nodeFrom 节点来源
     * @return 执行账号
     */
    private AccountVo getProtocolAndUserName(AutoexecJobVo jobVo, String nodeFrom) {
        AccountVo accountVo = new AccountVo();
        String protocol;
        Long protocolId;
        String account;
        AutoexecJobContentVo jobContent = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
        if (jobContent == null) {
            throw new AutoexecJobConfigNotFoundException(jobVo.getId());
        }
        jobVo.setConfigStr(jobContent.getContent());
        AutoexecJobSourceVo jobSourceVo = AutoexecJobSourceFactory.getSourceMap().get(jobVo.getSource());
        if (jobSourceVo == null) {
            throw new AutoexecJobSourceInvalidException(jobVo.getSource());
        }
        IAutoexecJobSourceTypeHandler autoexecJobSourceActionHandler = AutoexecJobSourceTypeHandlerFactory.getAction(jobSourceVo.getType());
        AutoexecCombopVo combopVo = autoexecJobSourceActionHandler.getSnapshotAutoexecCombop(jobVo);
        AutoexecCombopExecuteConfigVo executeConfigVo;
        if (Objects.equals(AutoexecJobPhaseNodeFrom.JOB.getValue(), nodeFrom)) {
            executeConfigVo = combopVo.getConfig().getExecuteConfig();
            if (executeConfigVo == null) {
                return null;
            }
            protocol = executeConfigVo.getProtocol();
            protocolId = executeConfigVo.getProtocolId();
            account = executeConfigVo.getExecuteUser();
        } else if (Objects.equals(AutoexecJobPhaseNodeFrom.GROUP.getValue(), nodeFrom)) {
            AutoexecJobGroupVo groupVo = jobVo.getExecuteJobGroupVo();
            Optional<AutoexecCombopGroupVo> combopGroupVoOptional = combopVo.getConfig().getCombopGroupList().stream().filter(o -> Objects.equals(groupVo.getSort(), o.getSort())).findFirst();
            if (!combopGroupVoOptional.isPresent()) {
                throw new AutoexecJobGroupNotFoundException(jobVo.getId(), groupVo.getSort());
            }
            executeConfigVo = combopGroupVoOptional.get().getConfig().getExecuteConfig();
            if (executeConfigVo == null) {
                return null;
            }
            protocol = executeConfigVo.getProtocol();
            protocolId = executeConfigVo.getProtocolId();
            account = executeConfigVo.getExecuteUser();
            if (protocolId == null) {
                executeConfigVo = combopVo.getConfig().getExecuteConfig();
                protocol = executeConfigVo.getProtocol();
                protocolId = executeConfigVo.getProtocolId();
            }
            if (StringUtils.isBlank(account)) {
                executeConfigVo = combopVo.getConfig().getExecuteConfig();
                account = executeConfigVo.getExecuteUser();
            }
        } else {
            AutoexecJobPhaseVo jobPhaseVo = jobVo.getExecuteJobPhaseList().get(0);
            Optional<AutoexecCombopPhaseVo> combopPhaseVoOptional = combopVo.getConfig().getCombopPhaseList().stream().filter(o -> Objects.equals(jobPhaseVo.getName(), o.getName())).findFirst();
            if (!combopPhaseVoOptional.isPresent()) {
                throw new AutoexecJobPhaseNotFoundException(jobPhaseVo.getName());
            }
            executeConfigVo = combopPhaseVoOptional.get().getConfig().getExecuteConfig();
            if (executeConfigVo == null) {
                return null;
            }
            protocol = executeConfigVo.getProtocol();
            protocolId = executeConfigVo.getProtocolId();
            account = executeConfigVo.getExecuteUser();
            if (protocolId == null) {
                executeConfigVo = combopVo.getConfig().getExecuteConfig();
                protocol = executeConfigVo.getProtocol();
                protocolId = executeConfigVo.getProtocolId();
            }
            if (StringUtils.isBlank(account)) {
                executeConfigVo = combopVo.getConfig().getExecuteConfig();
                account = executeConfigVo.getExecuteUser();
            }
        }
        //tagent 没有account
        if (!Objects.equals(executeConfigVo.getProtocol(), Protocol.TAGENT.getValue())) {
            accountVo.setAccount(account);
        }
        accountVo.setProtocol(protocol);
        accountVo.setProtocolId(protocolId);
        return accountVo;
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
