package codedriver.module.autoexec.service;


import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.constvalue.AutoexecGlobalParamType;
import codedriver.framework.autoexec.constvalue.AutoexecProfileParamInvokeType;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.crossover.IAutoexecProfileCrossoverService;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.exception.AutoexecProfileHasBeenReferredException;
import codedriver.framework.autoexec.exception.AutoexecProfileIsNotFoundException;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.module.autoexec.dependency.AutoexecGlobalParamProfileDependencyHandler;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
@Service
public class AutoexecProfileServiceImpl implements AutoexecProfileService, IAutoexecProfileCrossoverService {

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Resource
    AutoexecService autoexecService;

    @Override
    public List<AutoexecProfileParamVo> getProfileParamListById(Long id) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        return getLatestParamList(profileVo.getId(), profileVo.getAutoexecOperationVoList());
    }

    /**
     * 保存profile和tool、script的关系
     *
     * @param profileId               profile id
     * @param autoexecOperationVoList 自动化工具list
     */
    @Override
    public void saveProfileOperation(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList) {
        Long updateTag = System.currentTimeMillis();
        List<Long> toolIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        List<Long> scriptIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        //tool
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            autoexecProfileMapper.insertAutoexecProfileOperation(profileId, toolIdList, ToolType.TOOL.getValue(), updateTag);
        }
        //script
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            autoexecProfileMapper.insertAutoexecProfileOperation(profileId, scriptIdList, ToolType.SCRIPT.getValue(), updateTag);
        }
        autoexecProfileMapper.deleteProfileOperationByProfileIdAndLcd(profileId, updateTag);
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

        //保存profile参数值引用全局参数的关系
        List<AutoexecProfileParamVo> profileParamVoList = profileVo.getProfileParamVoList();
        List<Long> needDeleteParamIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(profileParamVoList)) {
            Long updateTag = System.currentTimeMillis();
            for (AutoexecProfileParamVo paramVo : profileParamVoList) {
                //删除当前profile参数与全局参数的关系
                DependencyManager.delete(AutoexecGlobalParamProfileDependencyHandler.class, paramVo.getId());
                //保存profile参数和globalParam的关系
                if (StringUtils.equals(AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue(), paramVo.getMappingMode())) {
                    String globalParamKey = paramVo.getDefaultValueStr();
                    if (StringUtils.isNotEmpty(globalParamKey)) {
                        JSONObject dependencyConfig = new JSONObject();
                        dependencyConfig.put("profileId", profileVo.getId());
                        DependencyManager.insert(AutoexecGlobalParamProfileDependencyHandler.class, globalParamKey, paramVo.getId(), dependencyConfig);

                    }
                }
            }

            //保存profile参数
            autoexecProfileMapper.insertAutoexecProfileParamList(profileParamVoList, profileVo.getId(), updateTag);
            needDeleteParamIdList.addAll(autoexecProfileMapper.getNeedDeleteProfileParamIdListByProfileIdAndLcd(profileVo.getId(), updateTag));
        }

        //删除多余的profile参数
        if (CollectionUtils.isNotEmpty(needDeleteParamIdList)) {
            autoexecProfileMapper.deleteProfileParamByIdList(needDeleteParamIdList);
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
        //查询是否被引用(产品确认：无需判断所属系统和关联工具，只需要考虑是否被组合工具使用)
        if (DependencyManager.getDependencyCount(AutoexecFromType.PROFILE, id) > 0) {
            throw new AutoexecProfileHasBeenReferredException(profileVo.getName());
        }

        //删除profile的参数
        autoexecProfileMapper.deleteProfileParamByProfileId(id);
        //删除profile的关联工具关系
        autoexecProfileMapper.deleteProfileOperationByProfileId(id);
        //删除profile
        autoexecProfileMapper.deleteProfileById(id);
    }

    /**
     * 获取key对应的值：
     * 1、根据参数引用类型，分为引用全局参数 和 常量
     * 2、若是全局参数，直接赋值
     * 3、若是常量，直接赋值
     *
     * @param keyList   key列表
     * @param profileId profile id
     * @return
     */
    @Override
    public Map<String, Object> getAutoexecProfileParamListByKeyListAndProfileId(List<String> keyList, Long profileId) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(profileId);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(profileId);
        }
        if (CollectionUtils.isEmpty(keyList)) {
            return null;
        }
        List<AutoexecProfileParamVo> profileParamList = getLatestParamList(profileVo.getId(), profileVo.getAutoexecOperationVoList());
        if (CollectionUtils.isEmpty(profileParamList)) {
            return null;
        }
        Map<String, AutoexecProfileParamVo> nowParamMap = profileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));

        Map<String, Object> returnMap = new HashMap<>();
        for (String key : keyList) {
            if (!nowParamMap.containsKey(key)) {
                continue;
            }
            AutoexecProfileParamVo paramVo = nowParamMap.get(key);

            if (StringUtils.equals(paramVo.getMappingMode(), AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue())) {
                //获取引用的全局参数值
                AutoexecGlobalParamVo globalParamVo = autoexecGlobalParamMapper.getGlobalParamByKey(paramVo.getDefaultValueStr());
                if (StringUtils.equals(AutoexecGlobalParamType.PASSWORD.getValue(), globalParamVo.getType()) && !Objects.isNull(globalParamVo.getDefaultValue())) {
                    String pwd = RC4Util.encrypt(globalParamVo.getDefaultValueStr());
                    paramVo.setDefaultValue(pwd);
                }else{
                    paramVo.setDefaultValue(globalParamVo.getDefaultValue());
                }
            }
            returnMap.put(paramVo.getKey(), paramVo.getDefaultValue());
        }
        return returnMap;
    }

    /**
     * 通过profileId列表获取对应的profile列表
     *
     * @param idList profile id列表
     * @return
     */
    @Override
    public List<AutoexecProfileVo> getProfileVoListByIdList(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return null;
        }
        List<AutoexecProfileVo> returnList = autoexecProfileMapper.getProfileInfoListByIdList(idList);
        if (CollectionUtils.isEmpty(returnList)) {
            return null;
        }
        for (AutoexecProfileVo profileVo : returnList) {
            profileVo.setProfileParamVoList(getLatestParamList(profileVo.getId(), profileVo.getAutoexecOperationVoList()));
        }
        return returnList;
    }

    /**
     * 获取最新的profile参数列表
     *
     * @param profileId       profile id
     * @param operationVoList 工具列表
     * @return 最新的profile参数列表
     */
    public List<AutoexecProfileParamVo> getLatestParamList(Long profileId, List<AutoexecOperationVo> operationVoList) {

        //根据profile与工具的关系，获取最新参数
        List<AutoexecParamVo> newOperationParamList = autoexecService.getAutoexecOperationParamVoList(operationVoList);
        if (CollectionUtils.isEmpty(newOperationParamList)) {
            return new ArrayList<>();
        }
        List<AutoexecProfileParamVo> newProfileParamList = new ArrayList<>();
        for (AutoexecParamVo paramVo : newOperationParamList) {
            newProfileParamList.add(new AutoexecProfileParamVo(paramVo));
        }

        //profile旧的参数
        List<AutoexecProfileParamVo> oldProfileParamList = autoexecProfileMapper.getProfileParamListByProfileId(profileId);
        if (CollectionUtils.isEmpty(oldProfileParamList)) {
            return newProfileParamList;
        }

        Map<String, AutoexecProfileParamVo> oldProfileParamMap = oldProfileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));
        Map<String, AutoexecProfileParamVo> newProfileParamMap = newProfileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));

        //根据参数key替换对应的值：
        //新旧参数的key和type都相同时，才会进行替换值和查询参数引用全局参数的关系
        for (String newParamKey : newProfileParamMap.keySet()) {

            AutoexecProfileParamVo newParamVo = newProfileParamMap.get(newParamKey);
            AutoexecProfileParamVo oldParamVo = oldProfileParamMap.get(newParamKey);

            if (oldParamVo != null && StringUtils.equals(oldParamVo.getType(), newParamVo.getType())) {
                newParamVo.setId(oldParamVo.getId());
                if (StringUtils.equals(AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue(), oldParamVo.getMappingMode())) {
                    newParamVo.setMappingMode(AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue());
                    newParamVo.setDefaultValue(oldParamVo.getDefaultValue());
                } else if (StringUtils.equals(AutoexecProfileParamInvokeType.CONSTANT.getValue(), oldParamVo.getMappingMode())) {
                    if (StringUtils.equals(AutoexecGlobalParamType.PASSWORD.getValue(), oldParamVo.getType()) && StringUtils.isNotBlank(oldParamVo.getDefaultValueStr())) {
                        newParamVo.setDefaultValue(RC4Util.decrypt(oldParamVo.getDefaultValueStr()));
                    } else {
                        newParamVo.setDefaultValue(oldParamVo.getDefaultValue());
                    }
                }
            }
        }
        return newProfileParamList;
    }
}
