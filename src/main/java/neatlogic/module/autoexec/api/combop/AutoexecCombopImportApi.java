/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.dto.profile.AutoexecProfileVo;
import neatlogic.framework.autoexec.dto.scenario.AutoexecScenarioVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.util.ModuleUtil;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dto.module.ModuleGroupVo;
import neatlogic.framework.exception.file.FileExtNotAllowedException;
import neatlogic.framework.exception.file.FileNotUploadException;
import neatlogic.framework.notify.core.INotifyPolicyHandler;
import neatlogic.framework.notify.core.NotifyPolicyHandlerFactory;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.notify.dto.NotifyPolicyHandlerVo;
import neatlogic.framework.notify.dto.NotifyPolicyVo;
import neatlogic.framework.notify.exception.NotifyPolicyHandlerNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecProfileMapper;
import neatlogic.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 导入组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
@AuthAction(action = AUTOEXEC_COMBOP_ADD.class)
public class AutoexecCombopImportApi extends PrivateBinaryStreamApiComponentBase {

    private final Logger logger = LoggerFactory.getLogger(AutoexecCombopImportApi.class);
    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private NotifyMapper notifyMapper;

    @Resource
    private AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    private AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Resource
    private AutoexecScenarioMapper autoexecScenarioMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/import";
    }

    @Override
    public String getName() {
        return "导入组合工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nameList", type = ApiParamType.STRING, isRequired = true, minSize = 1, desc = "名称列表"),
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.JSONARRAY, desc = "导入结果")
    })
    @Description(desc = "导入组合工具")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //获取所有导入文件
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        //如果没有导入文件, 抛异常
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        String names = paramObj.getString("nameList");
        String[] split = names.split(",");
        List<String> nameList = new ArrayList<>();
        for (String str : split) {
            nameList.add(str);
        }
        int successCount = 0;
        int failureCount = 0;
        JSONArray failureReasonList = new JSONArray();
        byte[] buf = new byte[1024];
        //遍历导入文件
        for (Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            //反序列化获取对象
            try (ZipInputStream zipis = new ZipInputStream(multipartFile.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ZipEntry zipEntry;
                while ((zipEntry = zipis.getNextEntry()) != null) {
                    String name = zipEntry.getName();
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    if (name.endsWith(".json")) {
                        name = name.substring(0, name.length() - 5);
                    }
                    if (!nameList.contains(name)) {
                        continue;
                    }
                    int len;
                    while ((len = zipis.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    AutoexecCombopVo autoexecCombopVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), new TypeReference<AutoexecCombopVo>() {});
                    JSONObject resultObj = save(autoexecCombopVo);
                    int isSucceed = resultObj.getIntValue("isSucceed");
                    if (isSucceed == 0) {
                        failureCount++;
                    } else {
                        successCount++;
                    }
                    failureReasonList.add(resultObj);
                    out.reset();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureCount);
        resultObj.put("failureReasonList", failureReasonList);
        return resultObj;
    }

    /**
     * 保存一个组合工具信息
     * @param autoexecCombopVo
     * @return
     */
    private JSONObject save(AutoexecCombopVo autoexecCombopVo) {
        Long id = autoexecCombopVo.getId();
        String oldName = autoexecCombopVo.getName();
        if (StringUtils.isBlank(oldName)) {
            throw new ClassCastException();
        }
        if (autoexecCombopVo.getTypeId() == null){
            throw new ClassCastException();
        }
        if (autoexecCombopVo.getIsActive() == null){
            throw new ClassCastException();
        }
        if (StringUtils.isBlank(autoexecCombopVo.getOperationType())){
            throw new ClassCastException();
        }
        if (autoexecCombopVo.getConfig() == null){
            throw new ClassCastException();
        }
        AutoexecCombopVo oldAutoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (oldAutoexecCombopVo != null) {
            if (equals(oldAutoexecCombopVo, autoexecCombopVo)) {
                List<AutoexecCombopVersionVo> oldVersionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(id);
                List<AutoexecCombopVersionVo> versionList = autoexecCombopVo.getVersionList();
                if (Objects.equals(JSONObject.toJSONString(oldVersionList), JSONObject.toJSONString(versionList))) {
                    JSONObject resultObj = new JSONObject();
                    resultObj.put("item", oldName);
                    resultObj.put("isSucceed", 1);
                    return resultObj;
                }
            }
        }

        Set<String> failureReasonSet = new HashSet<>();
        Set<String> warnReasonSet = new HashSet<>();
        Long typeId = autoexecTypeMapper.getTypeIdByName(autoexecCombopVo.getTypeName());
        if (typeId == null) {
            failureReasonSet.add("缺少引用的工具类型：'" + autoexecCombopVo.getTypeName() + "'");
        } else {
            autoexecCombopVo.setTypeId(typeId);
        }
//        if (autoexecCombopVo.getNotifyPolicyName() != null) {
//            NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyByName(autoexecCombopVo.getNotifyPolicyName());
//            if (notifyPolicyVo == null) {
//                failureReasonSet.add("缺少引用的通知策略：'" + autoexecCombopVo.getNotifyPolicyName() + "'");
//            } else {
//                autoexecCombopVo.setNotifyPolicyId(notifyPolicyVo.getId());
//            }
//        }
        int index = 0;
        //如果导入的流程名称已存在就重命名
        while (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            index++;
            autoexecCombopVo.setName(oldName + "_" + index);
        }
        String userUuid = UserContext.get().getUserUuid(true);
        autoexecCombopVo.setOwner(userUuid);
        autoexecCombopVo.setFcu(userUuid);
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        InvokeNotifyPolicyConfigVo notifyPolicyConfigVo = config.getInvokeNotifyPolicyConfig();
        if (notifyPolicyConfigVo != null) {
            if (notifyPolicyConfigVo.getPolicyName() != null) {
                NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyByName(notifyPolicyConfigVo.getPolicyName());
                if (notifyPolicyVo == null) {
                    failureReasonSet.add("缺少引用的通知策略：'" + notifyPolicyConfigVo.getPolicyPath() + "'");
                } else {
                    INotifyPolicyHandler notifyPolicyHandler = NotifyPolicyHandlerFactory.getHandler(notifyPolicyVo.getHandler());
                    if (notifyPolicyHandler == null) {
                        throw new NotifyPolicyHandlerNotFoundException(notifyPolicyVo.getHandler());
                    }
                    String moduleGroupName = "";
                    List<NotifyPolicyHandlerVo> notifyPolicyHandlerList = NotifyPolicyHandlerFactory.getNotifyPolicyHandlerList();
                    for (NotifyPolicyHandlerVo notifyPolicyHandlerVo : notifyPolicyHandlerList) {
                        if (Objects.equals(notifyPolicyHandlerVo.getHandler(), notifyPolicyVo.getHandler())) {
                            ModuleGroupVo moduleGroupVo = ModuleUtil.getModuleGroup(notifyPolicyHandlerVo.getModuleGroup());
                            if (moduleGroupVo != null) {
                                moduleGroupName = moduleGroupVo.getGroupName();
                            }
                        }
                    }
                    String handlerName = notifyPolicyHandler.getName();
                    notifyPolicyConfigVo.setPolicyPath(moduleGroupName + "/" + handlerName + "/" + notifyPolicyVo.getName());
                    notifyPolicyConfigVo.setPolicyId(notifyPolicyVo.getId());
                }
            }
        }
        List<Long> scenarioIdList = new ArrayList<>();
        List<String> scenarioNameList = new ArrayList<>();
        List<Long> scriptIdList = new ArrayList<>();
        List<String> scriptNameList = new ArrayList<>();
        List<Long> toolIdList = new ArrayList<>();
        List<String> toolNameList = new ArrayList<>();
        List<Long> profileIdList = new ArrayList<>();
        List<String> globalParamKeyList = new ArrayList<>();
        List<AutoexecCombopVersionVo> versionList = autoexecCombopVo.getVersionList();
        // 遍历所有版本信息，找出需要查询数据库的唯一标识
        for (AutoexecCombopVersionVo autoexecCombopVersionVo : versionList) {
            AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
            if (versionConfig == null) {
                continue;
            }
            // 检查场景列表
            List<AutoexecCombopScenarioVo> scenarioList = versionConfig.getScenarioList();
            if (CollectionUtils.isNotEmpty(scenarioList)) {
                Iterator<AutoexecCombopScenarioVo> iterator = scenarioList.iterator();
                while (iterator.hasNext()) {
                    AutoexecCombopScenarioVo combopScenarioVo = iterator.next();
                    scenarioIdList.add(combopScenarioVo.getScenarioId());
                    scenarioNameList.add(combopScenarioVo.getScenarioName());
                }
            }
            List<AutoexecCombopPhaseVo> combopPhaseList = versionConfig.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                    if (autoexecCombopPhaseVo == null) {
                        continue;
                    }
                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                    if (CollectionUtils.isEmpty(phaseOperationList)) {
                        continue;
                    }
                    for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                        if (autoexecCombopPhaseOperationVo == null) {
                            continue;
                        }
                        collectOperationIdAndName(autoexecCombopPhaseOperationVo, scriptIdList, scriptNameList, toolIdList, toolNameList);
                        AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                        if (operationConfig == null) {
                            continue;
                        }
                        if (operationConfig.getProfileId() != null) {
                            profileIdList.add(operationConfig.getProfileId());
                        }
                        collectGlobalParamKeyList(operationConfig, globalParamKeyList);
                        List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                        if (CollectionUtils.isNotEmpty(ifList)) {
                            for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                                if (operationVo == null) {
                                    continue;
                                }
                                collectOperationIdAndName(operationVo, scriptIdList, scriptNameList, toolIdList, toolNameList);
                                AutoexecCombopPhaseOperationConfigVo ifListOperationConfig = operationVo.getConfig();
                                if (ifListOperationConfig != null) {
                                    if (ifListOperationConfig.getProfileId() != null) {
                                        profileIdList.add(ifListOperationConfig.getProfileId());
                                    }
                                    collectGlobalParamKeyList(ifListOperationConfig, globalParamKeyList);
                                }
                            }
                        }
                        List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                        if (CollectionUtils.isNotEmpty(elseList)) {
                            for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                                if (operationVo == null) {
                                    continue;
                                }
                                collectOperationIdAndName(operationVo, scriptIdList, scriptNameList, toolIdList, toolNameList);
                                AutoexecCombopPhaseOperationConfigVo elseListOperationConfig = operationVo.getConfig();
                                if (elseListOperationConfig != null) {
                                    if (elseListOperationConfig.getProfileId() != null) {
                                        profileIdList.add(elseListOperationConfig.getProfileId());
                                    }
                                    collectGlobalParamKeyList(elseListOperationConfig, globalParamKeyList);
                                }
                            }
                        }
                    }
                }
            }
        }
        Map<Long, AutoexecScenarioVo> idKeyScenarioMap = new HashMap<>();
        Map<String, AutoexecScenarioVo> nameKeyScenarioMap = new HashMap<>();
        Map<Long, AutoexecScriptVo> idKeyScriptMap = new HashMap<>();
        Map<String, AutoexecScriptVo> nameKeyScriptMap = new HashMap<>();
        Map<Long, AutoexecScriptVersionVo> scriptIdKeyScriptActiveVersionMap = new HashMap<>();
        Map<Long, AutoexecToolVo> idKeyToolMap = new HashMap<>();
        Map<String, AutoexecToolVo> nameKeyToolMap = new HashMap<>();
        Map<Long, AutoexecProfileVo> idKeyProfileMap = new HashMap<>();
        Map<String, AutoexecGlobalParamVo> globalParamMap = new HashMap<>();
        // 批量查询数据库
        if (CollectionUtils.isNotEmpty(scenarioIdList)) {
            List<AutoexecScenarioVo> scenarioList = autoexecScenarioMapper.getScenarioListByIdList(scenarioIdList);
            idKeyScenarioMap = scenarioList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        }
        if (CollectionUtils.isNotEmpty(scenarioNameList)) {
            List<AutoexecScenarioVo> scenarioList = autoexecScenarioMapper.getScenarioListByNameList(scenarioNameList);
            nameKeyScenarioMap = scenarioList.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            List<AutoexecScriptVo> scriptList = autoexecScriptMapper.getAutoexecScriptByIdList(scriptIdList);
            idKeyScriptMap = scriptList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        }
        if (CollectionUtils.isNotEmpty(scriptNameList)) {
            List<AutoexecScriptVo> scriptList = autoexecScriptMapper.getAutoexecScriptByNameList(scriptNameList);
            nameKeyScriptMap = scriptList.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
            List<Long> idList = scriptList.stream().map(AutoexecScriptVo::getId).collect(Collectors.toList());
            idList.removeAll(scriptIdList);
            scriptIdList.addAll(idList);
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            List<AutoexecScriptVersionVo> scriptVersionList = autoexecScriptMapper.getActiveVersionListByScriptIdList(scriptIdList);
            scriptIdKeyScriptActiveVersionMap = scriptVersionList.stream().collect(Collectors.toMap(e -> e.getScriptId(), e -> e));
        }
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            List<AutoexecToolVo> toolList = autoexecToolMapper.getToolBaseInfoListByIdList(toolIdList);
            idKeyToolMap = toolList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        }
        if (CollectionUtils.isNotEmpty(toolNameList)) {
            List<AutoexecToolVo> toolList = autoexecToolMapper.getToolByNameList(toolNameList);
            nameKeyToolMap = toolList.stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
        }
        if (CollectionUtils.isNotEmpty(profileIdList)) {
            List<AutoexecProfileVo> profileList = autoexecProfileMapper.getProfileInfoListByIdList(profileIdList);
            idKeyProfileMap = profileList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        }
        if (CollectionUtils.isNotEmpty(globalParamKeyList)) {
            List<AutoexecGlobalParamVo> globalParamList = autoexecGlobalParamMapper.getGlobalParamByKeyList(globalParamKeyList);
            globalParamMap = globalParamList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
        }

        for (AutoexecCombopVersionVo autoexecCombopVersionVo : versionList) {
            AutoexecCombopVersionConfigVo versionConfig = autoexecCombopVersionVo.getConfig();
            if (versionConfig == null) {
                continue;
            }
            // 检查场景列表
            List<AutoexecCombopScenarioVo> scenarioList = versionConfig.getScenarioList();
            if (CollectionUtils.isNotEmpty(scenarioList)) {
                List<String> nameList = new ArrayList<>();
                Iterator<AutoexecCombopScenarioVo> iterator = scenarioList.iterator();
                while (iterator.hasNext()) {
                    AutoexecCombopScenarioVo combopScenarioVo = iterator.next();
                    AutoexecScenarioVo scenarioVo = idKeyScenarioMap.get(combopScenarioVo.getScenarioId());
                    if (scenarioVo == null) {
                        scenarioVo = nameKeyScenarioMap.get(combopScenarioVo.getScenarioName());
                        if (scenarioVo != null) {
                            combopScenarioVo.setScenarioId(scenarioVo.getId());
                        } else {
                            nameList.add(combopScenarioVo.getScenarioName());
                            iterator.remove();
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(nameList)) {
                    warnReasonSet.add("缺少场景'" + String.join("'、'", nameList) + "'场景未导入");
                }
            }
            List<AutoexecCombopPhaseVo> combopPhaseList = versionConfig.getCombopPhaseList();
            if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                    if (autoexecCombopPhaseVo != null) {
                        autoexecCombopPhaseVo.setId(null);
                        AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                        List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                        if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                                if (autoexecCombopPhaseOperationVo != null) {
                                    checkOperation(autoexecCombopPhaseOperationVo, failureReasonSet, idKeyScriptMap, nameKeyScriptMap, scriptIdKeyScriptActiveVersionMap, idKeyToolMap, nameKeyToolMap);
                                    AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                                    if (operationConfig != null) {
                                        checkOperationConfig(operationConfig, warnReasonSet, idKeyProfileMap, globalParamMap);
                                        List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                                        if (CollectionUtils.isNotEmpty(ifList)) {
                                            for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                                                if (operationVo != null) {
                                                    checkOperation(operationVo, failureReasonSet, idKeyScriptMap, nameKeyScriptMap, scriptIdKeyScriptActiveVersionMap, idKeyToolMap, nameKeyToolMap);
                                                    AutoexecCombopPhaseOperationConfigVo ifListOperationConfig = operationVo.getConfig();
                                                    if (ifListOperationConfig != null) {
                                                        checkOperationConfig(ifListOperationConfig, warnReasonSet, idKeyProfileMap, globalParamMap);
                                                    }
                                                }
                                            }
                                        }
                                        List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                                        if (CollectionUtils.isNotEmpty(elseList)) {
                                            for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                                                if (operationVo != null) {
                                                    checkOperation(operationVo, failureReasonSet, idKeyScriptMap, nameKeyScriptMap, scriptIdKeyScriptActiveVersionMap, idKeyToolMap, nameKeyToolMap);
                                                    AutoexecCombopPhaseOperationConfigVo elseListOperationConfig = operationVo.getConfig();
                                                    if (elseListOperationConfig != null) {
                                                        checkOperationConfig(elseListOperationConfig, warnReasonSet, idKeyProfileMap, globalParamMap);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(failureReasonSet)) {
            updateAutoexecCombopExecuteConfigProtocolId(config);
            autoexecCombopVo.setConfigStr(null);
            if (oldAutoexecCombopVo == null) {
                autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
            } else {
                autoexecCombopService.deleteDependency(oldAutoexecCombopVo);
                List<AutoexecCombopVersionVo> oldVersionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(oldAutoexecCombopVo.getId());
                for (AutoexecCombopVersionVo oldVersionVo : oldVersionList) {
                    autoexecCombopService.deleteDependency(oldVersionVo);
                }
                autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
                autoexecCombopVersionMapper.deleteAutoexecCombopVersionByCombopId(autoexecCombopVo.getId());
            }
            autoexecCombopService.saveDependency(autoexecCombopVo);
            for (AutoexecCombopVersionVo autoexecCombopVersionVo : versionList) {
                autoexecCombopVersionMapper.insertAutoexecCombopVersion(autoexecCombopVersionVo);
                autoexecCombopService.saveDependency(autoexecCombopVersionVo);
            }
            JSONObject resultObj = new JSONObject();
            resultObj.put("item", oldName);
            resultObj.put("isSucceed", 1);
            resultObj.put("warnList", warnReasonSet);
            return resultObj;
        } else {
            JSONObject resultObj = new JSONObject();
            resultObj.put("item", oldName);
            resultObj.put("isSucceed", 0);
            resultObj.put("list", failureReasonSet);
            return resultObj;
        }
    }

    /**
     * 收集操作对应工具的Id和Name
     * @param autoexecCombopPhaseOperationVo 指定的操作数据
     * @param scriptIdList 保存自定义工具ID
     * @param scriptNameList 保存自定义工具名称
     * @param toolIdList 保存工具ID
     * @param toolNameList 保存工具名称
     */
    private void collectOperationIdAndName(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo, List<Long> scriptIdList, List<String> scriptNameList, List<Long> toolIdList, List<String> toolNameList) {
        if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
            scriptIdList.add(autoexecCombopPhaseOperationVo.getOperationId());
            scriptNameList.add(autoexecCombopPhaseOperationVo.getOperationName());
        } else {
            toolIdList.add(autoexecCombopPhaseOperationVo.getOperationId());
            toolNameList.add(autoexecCombopPhaseOperationVo.getOperationName());
        }
    }

    /**
     * 收集操作配置中全局变量key
     * @param config 指定操作配置信息
     * @param globalParamKeyList 保存全局变量key
     */
    private void collectGlobalParamKeyList(AutoexecCombopPhaseOperationConfigVo config, List<String> globalParamKeyList) {
        List<ParamMappingVo> paramMappingList = config.getParamMappingList();
        if (CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (ParamMappingMode.GLOBAL_PARAM.getValue().equals(paramMappingVo.getMappingMode())) {
                    globalParamKeyList.add((String)paramMappingVo.getValue());
                }
            }
        }
        List<ParamMappingVo> argumentMappingList = config.getArgumentMappingList();
        if (CollectionUtils.isNotEmpty(argumentMappingList)) {
            for (ParamMappingVo paramMappingVo : argumentMappingList) {
                if (ParamMappingMode.GLOBAL_PARAM.getValue().equals(paramMappingVo.getMappingMode())) {
                    globalParamKeyList.add((String)paramMappingVo.getValue());
                }
            }
        }
    }

    /**
     * 检查操作对应的工具是否存在
     * @param autoexecCombopPhaseOperationVo 指定的操作数据
     * @param failureReasonSet 收集失败原因集合
     * @param idKeyScriptMap 自定义工具数据映射列表
     * @param nameKeyScriptMap 自定义工具数据映射列表
     * @param idKeyScriptActiveVersionMap 自定义工具激活版本数据映射列表
     * @param idKeyToolMap 工具数据映射列表
     * @param nameKeyToolMap 工具数据映射列表
     */
    private void checkOperation(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo, Set<String> failureReasonSet,
                                Map<Long, AutoexecScriptVo> idKeyScriptMap,
                                Map<String, AutoexecScriptVo> nameKeyScriptMap,
                                Map<Long, AutoexecScriptVersionVo> idKeyScriptActiveVersionMap,
                                Map<Long, AutoexecToolVo> idKeyToolMap,
                                Map<String, AutoexecToolVo> nameKeyToolMap) {
        if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
            AutoexecScriptVo autoexecScriptVo = idKeyScriptMap.get(autoexecCombopPhaseOperationVo.getOperationId());
            if (autoexecScriptVo != null) {
                return;
            }
            autoexecScriptVo = nameKeyScriptMap.get(autoexecCombopPhaseOperationVo.getOperationName());
            if (autoexecScriptVo == null) {
                failureReasonSet.add("缺少引用的自定义工具：'" + autoexecCombopPhaseOperationVo.getOperationName() + "'");
            } else {
                AutoexecScriptVersionVo autoexecScriptVersionVo = idKeyScriptActiveVersionMap.get(autoexecScriptVo.getId());
                if (autoexecScriptVersionVo == null) {
                    failureReasonSet.add("自定义工具：'" + autoexecScriptVo.getName() + "'没有激活版本");
                }
                autoexecCombopPhaseOperationVo.setOperationId(autoexecScriptVo.getId());
            }
        } else {
            AutoexecToolVo autoexecToolVo = idKeyToolMap.get(autoexecCombopPhaseOperationVo.getOperationId());
            if (autoexecToolVo != null) {
                return;
            }
            autoexecToolVo = nameKeyToolMap.get(autoexecCombopPhaseOperationVo.getOperationName());
            if (autoexecToolVo == null) {
                failureReasonSet.add("缺少引用的工具：'" + autoexecCombopPhaseOperationVo.getOperationName() + "'");
            } else if (Objects.equals(autoexecToolVo.getIsActive(), 0)) {
                failureReasonSet.add("工具：'" + autoexecToolVo.getName() + "'未启用");
            } else {
                autoexecCombopPhaseOperationVo.setOperationId(autoexecToolVo.getId());
            }
        }
    }

    /**
     * 检查引用的预置参数集是否存在，引用的全局参数是否存在
     * @param config 指定操作配置信息
     * @param warnReasonSet 收集警告原因集合
     * @param idKeyProfileMap 阈值参数集数据映射列表
     * @param globalParamMap 全局参数数据映射列表
     */
    private void checkOperationConfig(AutoexecCombopPhaseOperationConfigVo config, Set<String> warnReasonSet,
                                      Map<Long, AutoexecProfileVo> idKeyProfileMap,
                                      Map<String, AutoexecGlobalParamVo> globalParamMap) {
        boolean profileExist = false;
        Long profileId = config.getProfileId();
        if (profileId != null) {
            profileExist = true;
            if (idKeyProfileMap.get(profileId) == null) {
                config.setProfileId(null);
                config.setProfileName(null);
                profileExist = false;
            }
        }
        List<ParamMappingVo> paramMappingList = config.getParamMappingList();
        if (CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (ParamMappingMode.PROFILE.getValue().equals(paramMappingVo.getMappingMode()) && !profileExist) {
                    paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                    paramMappingVo.setValue(null);
                } else if (ParamMappingMode.GLOBAL_PARAM.getValue().equals(paramMappingVo.getMappingMode())) {
                    AutoexecGlobalParamVo autoexecGlobalParamVo = globalParamMap.get((String)paramMappingVo.getValue());
                    if (autoexecGlobalParamVo == null) {
                        warnReasonSet.add("缺少全局参数：'" + paramMappingVo.getValue() + "'");
                        paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                        paramMappingVo.setValue(null);
                    }
                }
            }
        }
        List<ParamMappingVo> argumentMappingList = config.getArgumentMappingList();
        if (CollectionUtils.isNotEmpty(argumentMappingList)) {
            for (ParamMappingVo paramMappingVo : argumentMappingList) {
                if (ParamMappingMode.PROFILE.getValue().equals(paramMappingVo.getMappingMode()) && !profileExist) {
                    paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                    paramMappingVo.setValue(null);
                } else if (ParamMappingMode.GLOBAL_PARAM.getValue().equals(paramMappingVo.getMappingMode())) {
                    AutoexecGlobalParamVo autoexecGlobalParamVo = globalParamMap.get((String)paramMappingVo.getValue());
                    if (autoexecGlobalParamVo == null) {
                        warnReasonSet.add("缺少全局参数：'" + paramMappingVo.getValue() + "'");
                        paramMappingVo.setMappingMode(ParamMappingMode.CONSTANT.getValue());
                        paramMappingVo.setValue(null);
                    }
                }
            }
        }
    }

    /**
     * 根据protocol字段和protocolPort字段值更新protocolId字段值
     * @param config 组合工具config
     */
    private void updateAutoexecCombopExecuteConfigProtocolId(AutoexecCombopConfigVo config){
        if (config == null) {
            return;
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo combopPhaseVo : combopPhaseList) {
                if (combopPhaseVo == null) {
                    continue;
                }
                AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
                if (phaseConfig == null) {
                    continue;
                }
                AutoexecCombopExecuteConfigVo executeConfig = phaseConfig.getExecuteConfig();
                if (executeConfig != null) {
                    updateAutoexecCombopExecuteConfigProtocolId(executeConfig);
                }
            }
        }
        List<AutoexecCombopGroupVo> combopGroupList = config.getCombopGroupList();
        if (CollectionUtils.isNotEmpty(combopGroupList)) {
            for (AutoexecCombopGroupVo combopGroupVo : combopGroupList) {
                if (combopGroupVo == null) {
                    continue;
                }
                AutoexecCombopGroupConfigVo groupConfig = combopGroupVo.getConfig();
                if (groupConfig == null) {
                    continue;
                }
                AutoexecCombopExecuteConfigVo executeConfig = groupConfig.getExecuteConfig();
                if (executeConfig != null) {
                    updateAutoexecCombopExecuteConfigProtocolId(executeConfig);
                }
            }
        }
        AutoexecCombopExecuteConfigVo executeConfigVo = config.getExecuteConfig();
        if (executeConfigVo != null) {
            updateAutoexecCombopExecuteConfigProtocolId(executeConfigVo);
        }
    }

    /**
     * 根据protocol字段和protocolPort字段值更新protocolId字段值
     * @param config 执行目标config
     */
    private void updateAutoexecCombopExecuteConfigProtocolId(AutoexecCombopExecuteConfigVo config) {
        String name = config.getProtocol();
        Integer port = config.getProtocolPort();
        if (StringUtils.isBlank(name) || port == null) {
            config.setProtocolId(null);
            return;
        }
        IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
        AccountProtocolVo protocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByNameAndPort(name, port);
        if (protocolVo == null) {
            config.setProtocolId(null);
            return;
        }
        config.setProtocolId(protocolVo.getId());
    }

    private boolean equals(AutoexecCombopVo obj1, AutoexecCombopVo obj2){
        if (!Objects.equals(obj1.getName(), obj2.getName())) {
            return false;
        }
        if (!Objects.equals(obj1.getDescription(), obj2.getDescription())) {
            return false;
        }
        if (!Objects.equals(obj1.getTypeId(), obj2.getTypeId())) {
            return false;
        }
        if (!Objects.equals(obj1.getIsActive(), obj2.getIsActive())) {
            return false;
        }
        if (!Objects.equals(obj1.getOperationType(), obj2.getOperationType())) {
            return false;
        }
        if (!Objects.equals(obj1.getOwner(), obj2.getOwner())) {
            return false;
        }
        if (!Objects.equals(obj1.getConfigStr(), obj2.getConfigStr())) {
            return false;
        }
        return true;
    }
}
