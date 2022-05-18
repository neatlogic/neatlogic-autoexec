package codedriver.module.autoexec.service;


import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamValueVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.exception.AutoexecProfileHasBeenReferredException;
import codedriver.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
@Service
public class AutoexecProfileServiceImpl implements AutoexecProfileService {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecService autoexecService;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public List<AutoexecProfileParamVo> getProfileParamListById(Long id) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }

        List<AutoexecProfileParamVo> oldProfileParamList = autoexecProfileMapper.getProfileParamListByProfileId(id);
        List<AutoexecProfileParamVo> returnList = new ArrayList<>();
        List<AutoexecProfileParamVo> newProfileParamList = new ArrayList<>();
        List<AutoexecOperationVo> autoexecOperationVoList = autoexecService.getAutoexecOperationByScriptIdAndToolIdList(profileVo.getAutoexecOperationVoList().stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList()), profileVo.getAutoexecOperationVoList().stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList()));

        if (CollectionUtils.isNotEmpty(autoexecOperationVoList)) {
            for (AutoexecOperationVo operationVo : autoexecOperationVoList) {
                if (CollectionUtils.isNotEmpty(operationVo.getInputParamList())) {
                    for (AutoexecParamVo operationParamVo : operationVo.getInputParamList()) {
                        AutoexecProfileParamVo profileParamVo = new AutoexecProfileParamVo(operationParamVo);
                        profileParamVo.setOperationId(operationVo.getId());
                        profileParamVo.setOperationType(operationVo.getType());
                        newProfileParamList.add(profileParamVo);
                    }
                }
            }
            //根据key（唯一键）去重
            newProfileParamList = newProfileParamList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(AutoexecProfileParamVo::getKey))), ArrayList::new));
            //实时的参数信息
            Map<String, AutoexecProfileParamVo> newProfileParamMap = newProfileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));

            //旧的参数信息
            Map<String, AutoexecProfileParamVo> oldProfileParamMap = null;
            if (CollectionUtils.isNotEmpty(oldProfileParamList)) {
                oldProfileParamMap = oldProfileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));
            }

            //根据参数key替换对应的值
            if (MapUtils.isNotEmpty(newProfileParamMap) && MapUtils.isNotEmpty(oldProfileParamMap)) {
                for (String newParamKey : newProfileParamMap.keySet()) {
                    if (oldProfileParamMap.containsKey(newParamKey) && StringUtils.equals(oldProfileParamMap.get(newParamKey).getType(), newProfileParamMap.get(newParamKey).getType())) {
                        newProfileParamMap.get(newParamKey).setDefaultValue(oldProfileParamMap.get(newParamKey).getDefaultValue());
                        newProfileParamMap.get(newParamKey).setValueInvokeVoList(oldProfileParamMap.get(newParamKey).getValueInvokeVoList());
                    }
                }
            }
            for (String key : newProfileParamMap.keySet()) {
                returnList.add(newProfileParamMap.get(key));
            }
        }
        return returnList;
    }

    /**
     * 保存profile和tool、script的关系
     *
     * @param profileId               profile id
     * @param autoexecOperationVoList 自动化工具list
     */
    @Override
    public void saveProfileOperation(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList) {
        List<Long> toolIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        List<Long> scriptIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        //tool
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            autoexecProfileMapper.insertAutoexecProfileOperation(profileId, toolIdList, ToolType.TOOL.getValue());
        }
        //script
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            autoexecProfileMapper.insertAutoexecProfileOperation(profileId, scriptIdList, ToolType.SCRIPT.getValue());
        }
    }

    /**
     * 保存profile、profile参数、profile参数值引用全局参数的关系、profile和tool、script的关系
     *
     * @param profileVo profile
     */
    @Override
    public void saveProfile(AutoexecProfileVo profileVo) {
        //保存profile
        autoexecProfileMapper.insertProfile(profileVo);

        //保存profile参数、profile参数值引用全局参数的关系
        List<AutoexecProfileParamVo> profileParamVoList = profileVo.getProfileParamVoList();
        List<AutoexecProfileParamValueVo> insertParamValueVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(profileParamVoList)) {
            for (AutoexecProfileParamVo paramVo : profileParamVoList) {
                autoexecProfileMapper.deleteProfileParamByProfileParamId(paramVo.getId());
                if (CollectionUtils.isEmpty(paramVo.getValueInvokeVoList())) {
                    continue;
                }
                for (AutoexecProfileParamValueVo valueVo : paramVo.getValueInvokeVoList()) {
                    valueVo.setProfileParamId(paramVo.getId());
                    insertParamValueVoList.add(valueVo);
                }
            }
            Date nowLcd = new Date();
            //保存profile参数
            autoexecProfileMapper.insertAutoexecProfileParamList(profileParamVoList, profileVo.getId(), nowLcd);
            if (CollectionUtils.isNotEmpty(insertParamValueVoList)) {
                //保存profile参数引用全局参数的关系
                autoexecProfileMapper.insertProfileParamValueInvokeList(insertParamValueVoList);
            }

            //删除多余的profile参数 和 参数引用全局参数的关系
            List<Long> needDeleteParamIdList = autoexecProfileMapper.getNeedDeleteProfileParamIdListByProfileIdAndLcd(profileVo.getId(), nowLcd);
            if (CollectionUtils.isNotEmpty(needDeleteParamIdList)) {
                autoexecProfileMapper.deleteProfileParamByIdList(needDeleteParamIdList);
                autoexecProfileMapper.deleteProfileParamValueInvokeByParamIdList(needDeleteParamIdList);
            }
        }
        //保存profile和tool、script的关系
        saveProfileOperation(profileVo.getId(), profileVo.getAutoexecOperationVoList());
    }

    /**
     * 通过id 删除 profile
     *
     * @param id id
     */
    @Override
    public void deleteProfileById(Long id) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        //查询是否被引用(产品确认：无需判断所属系统和关联工具，只需要考虑是否被系统、模块、环境使用)
        if (DependencyManager.getDependencyCount(AutoexecFromType.AUTOEXEC_PROFILE_CIENTITY, id) > 0) {
            throw new AutoexecProfileHasBeenReferredException(profileVo.getName());
        }
        //删除profile的参数值引用关系（引用关系暂时只有 引用全局参数的关系）
        autoexecProfileMapper.deleteProfileParamValueInvokeByProfileId(id);
        //删除profile的参数
        autoexecProfileMapper.deleteProfileParamByProfileId(id);
        //删除profile的关联工具关系
        autoexecProfileMapper.deleteProfileOperationByProfileId(id);
        //删除profile
        autoexecProfileMapper.deleteProfileById(id);
    }

    @Override
    public Map<String, List<Object>> getAutoexecProfileParamListByKeyListAndProfileId(List<String> keyList, Long profileId) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(profileId);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(profileId);
        }
        if (CollectionUtils.isEmpty(keyList)) {
            return null;
        }
        List<AutoexecProfileParamVo> profileParamList = autoexecProfileMapper.getProfileParamListByProfileId(profileId);
        if (CollectionUtils.isEmpty(profileParamList)) {
            return null;
        }
        Map<String, AutoexecProfileParamVo> nowParamMap = profileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));

        Map<String, List<Object>> returnMap = new HashMap<>();
        for (String key : keyList) {
            if (!nowParamMap.containsKey(key)) {
                continue;
            }
            List<Object> valueList = new ArrayList<>();
            AutoexecProfileParamVo paramVo = nowParamMap.get(key);
            if (CollectionUtils.isNotEmpty(paramVo.getValueInvokeVoList())) {
                //获取引用的全局参数值
                valueList.add(paramVo.getValueInvokeVoList().stream().map(AutoexecProfileParamValueVo::getInvokeValue).collect(Collectors.toList()));
            } else if (!Objects.isNull(paramVo.getDefaultValue())) {
                //获取profile的默认值
                valueList.add(paramVo.getDefaultValue());
            } else {
                if (StringUtils.equals(paramVo.getOperationType(), ToolType.SCRIPT.getValue())) {
                    //  取最新版本的script
                    AutoexecScriptVersionVo activeScriptVersion = autoexecScriptMapper.getActiveVersionByScriptId(paramVo.getOperationId());
                    if (activeScriptVersion == null) {
                        continue;
                    }
                    List<AutoexecScriptVersionParamVo> scriptInputParamList = autoexecScriptMapper.getParamListByVersionId(activeScriptVersion.getId());
                    if (CollectionUtils.isEmpty(scriptInputParamList)) {
                        continue;
                    }
                    valueList.add(scriptInputParamList.stream().collect(Collectors.toMap(AutoexecScriptVersionParamVo::getKey, AutoexecScriptVersionParamVo::getDefaultValue)).get(key));
                } else if (StringUtils.equals(paramVo.getOperationType(), ToolType.TOOL.getValue())) {
                    //  取tool
                    List<AutoexecParamVo> toolInputParamList = autoexecToolMapper.getToolById(paramVo.getOperationId()).getInputParamList();
                    if (CollectionUtils.isNotEmpty(toolInputParamList)) {
                        valueList.add(toolInputParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getKey, AutoexecParamVo::getDefaultValue)).get(key));
                    }
                }
            }
            returnMap.put(paramVo.getKey(), valueList);
        }
        return returnMap;
    }
}
