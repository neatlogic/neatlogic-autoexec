/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.JobSource;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.combop.ParamMappingVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopActiveVersionNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecRunnerGroupTagInvalidException;
import neatlogic.framework.autoexec.exception.combop.AutoexecCombopVersionNotFoundEditTargetException;
import neatlogic.framework.autoexec.exception.job.AutoexecJobTargetInvalidException;
import neatlogic.framework.autoexec.script.paramtype.IScriptParamType;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.common.util.IpUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.user.UserNotFoundException;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dto.job.AutoexecCombopJobBuildResultVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 组合工具公共创建接口的用户上下文和作业参数构建能力。
 */
@Service
public class AutoexecCombopJobCreateServiceImpl implements AutoexecCombopJobCreateService {

    private static final Logger logger = LoggerFactory.getLogger(AutoexecCombopJobCreateServiceImpl.class);

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Override
    public String initExecUserContext(String execUserUuid) {
        if (StringUtils.isBlank(execUserUuid)) {
            execUserUuid = UserContext.get().getUserUuid();
        }
        UserVo execUser;
        AuthenticationInfoVo authenticationInfoVo;
        if (Objects.equals(SystemUser.SYSTEM.getUserUuid(), execUserUuid)) {
            execUser = SystemUser.SYSTEM.getUserVo();
            authenticationInfoVo = SystemUser.SYSTEM.getAuthenticationInfoVo();
        } else if (Objects.equals(SystemUser.AUTOEXEC.getUserUuid(), execUserUuid)) {
            // autoexec脚本使用autoexec虚拟用户。
            execUser = SystemUser.AUTOEXEC.getUserVo();
            authenticationInfoVo = SystemUser.AUTOEXEC.getAuthenticationInfoVo();
        } else {
            execUser = userMapper.getUserByUser(execUserUuid);
            if (execUser == null) {
                throw new UserNotFoundException(execUserUuid);
            }
            authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(execUserUuid);
        }
        UserContext.init(execUser, authenticationInfoVo, SystemUser.SYSTEM.getTimezone());
        return execUserUuid;
    }

    @Override
    public AutoexecCombopJobBuildResultVo buildJob(JSONObject paramObj, String execUserUuid) {
        String combopName = paramObj.getString("combopName");
        AutoexecCombopVo combopVo = autoexecCombopMapper.getAutoexecCombopByName(combopName);
        if (combopVo == null) {
            throw new AutoexecCombopNotFoundException(combopName);
        }
        Long activeVersionId = autoexecCombopVersionMapper.getAutoexecCombopActiveVersionIdByCombopId(combopVo.getId());
        if (activeVersionId == null) {
            throw new AutoexecCombopActiveVersionNotFoundException(combopName);
        }
        AutoexecCombopVersionVo versionVo = autoexecCombopService.getAutoexecCombopVersionById(activeVersionId);
        if (versionVo == null) {
            throw new AutoexecCombopVersionNotFoundEditTargetException(activeVersionId);
        }
        AutoexecCombopVersionConfigVo versionConfig = versionVo.getConfig();

        JSONObject param = paramObj.getJSONObject("param");
        paramObj.put("param", initParam(param, versionConfig));
        paramObj.put("execUser", execUserUuid);
        paramObj.put("operationType", CombopOperationType.COMBOP.getValue());
        paramObj.put("source", JobSource.COMBOP.getValue());
        paramObj.put("operationId", combopVo.getId());
        initExecuteConfig(paramObj);
        String runnerGroup = paramObj.getString("runnerGroup");
        String runnerGroupTag = normalizeRunnerGroupTag(paramObj.getString("runnerGroupTag"));
        paramObj.remove("runnerGroup");
        paramObj.remove("runnerGroupTag");
        AutoexecJobVo jobVo = JSON.toJavaObject(paramObj, AutoexecJobVo.class);
        if (StringUtils.isNotBlank(runnerGroup)) {
            ParamMappingVo runnerGroupMappingVo = new ParamMappingVo();
            runnerGroupMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
            runnerGroupMappingVo.setValue(runnerGroup);
            jobVo.setRunnerGroup(runnerGroupMappingVo);
        }
        if (StringUtils.isNotBlank(runnerGroupTag)) {
            ParamMappingVo runnerGroupTagMappingVo = new ParamMappingVo();
            runnerGroupTagMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
            runnerGroupTagMappingVo.setValue(runnerGroupTag);
            jobVo.setRunnerGroupTag(runnerGroupTagMappingVo);
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = jobVo.getExecuteConfig();
        if (executeConfigVo != null && StringUtils.isNotBlank(executeConfigVo.getProtocol())) {
            IResourceAccountCrossoverMapper accountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
            AccountProtocolVo accountProtocolVo = accountCrossoverMapper.getAccountProtocolVoByProtocolName(executeConfigVo.getProtocol());
            if (accountProtocolVo == null) {
                throw new ResourceCenterAccountProtocolNotFoundException(executeConfigVo.getProtocol());
            }
            executeConfigVo.setProtocolId(accountProtocolVo.getId());
        }
        if (versionConfig != null) {
            AutoexecCombopExecuteConfigVo executeConfig = versionConfig.getExecuteConfig();
            if (executeConfig != null) {
                jobVo.setPreCondition(executeConfig.getPreCondition());
                jobVo.setWhenToSpecify(executeConfig.getWhenToSpecify());
            }
        }
        return new AutoexecCombopJobBuildResultVo(jobVo, activeVersionId);
    }

    /**
     * 保持公共创建接口原有规则：仅在未传executeConfig时将扁平字段转换成结构化配置。
     */
    private void initExecuteConfig(JSONObject paramObj) {
        if (!paramObj.containsKey("executeConfig")) {
            JSONObject executeConfig = new JSONObject();
            paramObj.put("executeConfig", executeConfig);
            executeConfig.put("protocol", paramObj.getString("protocol"));
            if (StringUtils.isNotBlank(paramObj.getString("executeUser"))) {
                JSONObject executeUser = new JSONObject();
                executeUser.put("mappingMode", ParamMappingMode.CONSTANT.getValue());
                executeUser.put("value", paramObj.getString("executeUser"));
                executeConfig.put("executeUser", executeUser);
            }
            JSONObject executeNodeConfig = new JSONObject();
            executeNodeConfig.put("inputNodeList", parseIpPortList(paramObj.getJSONArray("ipPortList")));
            executeConfig.put("executeNodeConfig", executeNodeConfig);
        }
    }

    /**
     * 将扁平目标参数转换成作业节点。空数组表示不覆盖组合工具的目标配置。
     */
    private List<AutoexecNodeVo> parseIpPortList(JSONArray ipPortList) {
        if (CollectionUtils.isEmpty(ipPortList)) {
            return Collections.emptyList();
        }
        List<AutoexecNodeVo> nodeList = new ArrayList<>();
        for (Object targetObject : ipPortList) {
            if (!(targetObject instanceof String) || StringUtils.isBlank((String) targetObject)) {
                throw new AutoexecJobTargetInvalidException(String.valueOf(targetObject));
            }
            String target = ((String) targetObject).trim();
            if (!target.matches("[^:/\\s]+(?::(?:[1-9]\\d{0,4})(?:/[^/\\s]+)?)?")) {
                throw new AutoexecJobTargetInvalidException(target);
            }
            try {
                AutoexecNodeVo nodeVo = new AutoexecNodeVo(target);
                if (nodeVo.getIp().contains("*") || !IpUtil.checkIp(nodeVo.getIp())
                        || (nodeVo.getPort() != null && nodeVo.getPort() > 65535)) {
                    throw new AutoexecJobTargetInvalidException(target);
                }
                nodeList.add(nodeVo);
            } catch (NumberFormatException ex) {
                logger.error("Invalid autoexec job target: {}", target, ex);
                throw new AutoexecJobTargetInvalidException(target);
            }
        }
        return nodeList;
    }

    /**
     * 将单个Runner执行组标签或JSON数组字符串统一转换成合法的JSON数组字符串。
     */
    private String normalizeRunnerGroupTag(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmedValue = value.trim();
        List<String> tagList;
        if (trimmedValue.startsWith("[")) {
            JSONArray tagArray;
            try {
                tagArray = JSON.parseArray(trimmedValue);
            } catch (Exception ex) {
                logger.error("Invalid runner group tag JSON array: {}", value, ex);
                throw new AutoexecRunnerGroupTagInvalidException(value);
            }
            if (CollectionUtils.isEmpty(tagArray) || tagArray.stream()
                    .anyMatch(tag -> !(tag instanceof String) || StringUtils.isBlank((String) tag))) {
                throw new AutoexecRunnerGroupTagInvalidException(value);
            }
            tagList = tagArray.toJavaList(String.class);
        } else {
            tagList = Collections.singletonList(trimmedValue);
        }
        if (CollectionUtils.isEmpty(tagList) || tagList.stream().anyMatch(StringUtils::isBlank)) {
            throw new AutoexecRunnerGroupTagInvalidException(value);
        }
        return JSON.toJSONString(tagList);
    }

    /**
     * 保持公共创建接口原有运行参数默认值及类型转换规则。
     */
    private JSONObject initParam(JSONObject param, AutoexecCombopVersionConfigVo versionConfig) {
        JSONObject newParam = new JSONObject();
        List<AutoexecParamVo> paramList = versionConfig.getRuntimeParamList().stream()
                .filter(o -> !Objects.equals(ParamType.FILE.getValue(), o.getType()))
                .collect(Collectors.toList());
        for (AutoexecParamVo paramVo : paramList) {
            Object value;
            IScriptParamType paramType = ScriptParamTypeFactory.getHandler(paramVo.getType());
            if (param.containsKey(paramVo.getKey()) && param.get(paramVo.getKey()) != null) {
                value = param.get(paramVo.getKey());
            } else {
                value = paramVo.getDefaultValue();
            }
            if (paramType != null) {
                value = paramType.getExchangeParamByValue(value);
            }
            newParam.put(paramVo.getKey(), value);
        }
        return newParam;
    }
}
