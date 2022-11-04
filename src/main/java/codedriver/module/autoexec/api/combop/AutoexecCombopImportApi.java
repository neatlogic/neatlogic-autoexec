/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.constvalue.ParamMappingMode;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.notify.dto.NotifyPolicyVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScenarioMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
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
                logger.debug(e.getMessage(), e);
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureCount);
        resultObj.put("failureReasonList", failureReasonList);
        return resultObj;
    }

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
                List<AutoexecParamVo> autoexecParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
                AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
                if (Objects.equals(JSONObject.toJSONString(autoexecParamVoList), JSONObject.toJSONString(config.getRuntimeParamList()))) {
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
        if (autoexecCombopVo.getNotifyPolicyName() != null) {
            NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyByName(autoexecCombopVo.getNotifyPolicyName());
            if (notifyPolicyVo == null) {
                failureReasonSet.add("缺少引用的通知策略：'" + autoexecCombopVo.getNotifyPolicyName() + "'");
            } else {
                autoexecCombopVo.setNotifyPolicyId(notifyPolicyVo.getId());
            }
        }
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
        // 检查场景列表
        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            List<String> nameList = new ArrayList<>();
            Iterator<AutoexecCombopScenarioVo> iterator = scenarioList.iterator();
            while (iterator.hasNext()) {
                AutoexecCombopScenarioVo scenarioVo = iterator.next();
                if (autoexecScenarioMapper.checkScenarioIsExistsById(scenarioVo.getScenarioId()) == 0) {
                    nameList.add(scenarioVo.getScenarioName());
                    iterator.remove();
                }
            }
            if (CollectionUtils.isNotEmpty(nameList)) {
                warnReasonSet.add("缺少场景'" + String.join("'、'", nameList) + "'场景未导入");
            }
        }
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo != null) {
                    autoexecCombopPhaseVo.setId(null);
                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                            if (autoexecCombopPhaseOperationVo != null) {
                                checkOperation(autoexecCombopPhaseOperationVo, failureReasonSet);
                                AutoexecCombopPhaseOperationConfigVo operationConfig = autoexecCombopPhaseOperationVo.getConfig();
                                if (operationConfig != null) {
                                    checkOperationConfig(operationConfig, warnReasonSet);
                                    List<AutoexecCombopPhaseOperationVo> ifList = operationConfig.getIfList();
                                    if (CollectionUtils.isNotEmpty(ifList)) {
                                        for (AutoexecCombopPhaseOperationVo operationVo : ifList) {
                                            if (operationVo != null) {
                                                checkOperation(operationVo, failureReasonSet);
                                                AutoexecCombopPhaseOperationConfigVo ifListOperationConfig = operationVo.getConfig();
                                                if (ifListOperationConfig != null) {
                                                    checkOperationConfig(ifListOperationConfig, warnReasonSet);
                                                }
                                            }
                                        }
                                    }
                                    List<AutoexecCombopPhaseOperationVo> elseList = operationConfig.getElseList();
                                    if (CollectionUtils.isNotEmpty(elseList)) {
                                        for (AutoexecCombopPhaseOperationVo operationVo : elseList) {
                                            if (operationVo != null) {
                                                checkOperation(operationVo, failureReasonSet);
                                                AutoexecCombopPhaseOperationConfigVo elseListOperationConfig = operationVo.getConfig();
                                                if (elseListOperationConfig != null) {
                                                    checkOperationConfig(elseListOperationConfig, warnReasonSet);
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
                autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(id);
                autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
            }
            List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                List<AutoexecCombopParamVo> autoexecCombopParamList = new ArrayList<>();
                for(AutoexecParamVo paramVo : runtimeParamList){
                    AutoexecCombopParamVo autoexecCombopParamVo = new AutoexecCombopParamVo(paramVo);
                    autoexecCombopParamVo.setCombopId(id);
                    autoexecCombopParamList.add(autoexecCombopParamVo);
                }
                autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamList);
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
     * 检查操作对应的工具是否存在
     * @param autoexecCombopPhaseOperationVo
     * @param failureReasonSet
     */
    private void checkOperation(AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo, Set<String> failureReasonSet) {
        if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
            AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(autoexecCombopPhaseOperationVo.getOperationId());
            if (autoexecScriptVo != null) {
                return;
            }
            autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoByName(autoexecCombopPhaseOperationVo.getOperationName());
            if (autoexecScriptVo == null) {
                failureReasonSet.add("缺少引用的自定义工具：'" + autoexecCombopPhaseOperationVo.getOperationName() + "'");
            } else {
                AutoexecScriptVersionVo autoexecScriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(autoexecScriptVo.getId());
                if (autoexecScriptVersionVo == null) {
                    failureReasonSet.add("自定义工具：'" + autoexecScriptVo.getName() + "'未启用");
                }
                autoexecCombopPhaseOperationVo.setOperationId(autoexecScriptVo.getId());
            }
        } else {
            AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(autoexecCombopPhaseOperationVo.getOperationId());
            if (autoexecToolVo != null) {
                return;
            }
            autoexecToolVo = autoexecToolMapper.getToolByName(autoexecCombopPhaseOperationVo.getOperationName());
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
     * @param config
     * @param warnReasonSet
     */
    private void checkOperationConfig(AutoexecCombopPhaseOperationConfigVo config, Set<String> warnReasonSet) {
        boolean profileExist = false;
        Long profileId = config.getProfileId();
        if (profileId != null) {
            profileExist = true;
            if (autoexecProfileMapper.checkProfileIsExists(profileId) == 0) {
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
                    AutoexecGlobalParamVo autoexecGlobalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey((String)paramMappingVo.getValue());
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
                    AutoexecGlobalParamVo autoexecGlobalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey((String)paramMappingVo.getValue());
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
        if (!Objects.equals(obj1.getNotifyPolicyId(), obj2.getNotifyPolicyId())) {
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
