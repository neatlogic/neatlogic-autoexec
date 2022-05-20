package codedriver.module.autoexec.service;


import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.autoexec.constvalue.AutoexecProfileParamInvokeType;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamValueVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
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

    @Override
    public List<AutoexecProfileParamVo> getProfileParamListById(Long id) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(id);
        }
        return getLatestParamList(profileVo);
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
        if (CollectionUtils.isNotEmpty(profileParamVoList)) {
            for (AutoexecProfileParamVo paramVo : profileParamVoList) {
                if (StringUtils.equals(AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue(), paramVo.getValueInvokeType())) {
                    List<AutoexecProfileParamValueVo> insertParamValueVoList = new ArrayList<>();
                    for (AutoexecProfileParamValueVo valueVo : paramVo.getValueInvokeVoList()) {
                        valueVo.setProfileParamId(paramVo.getId());
                        valueVo.setInvokeType(paramVo.getValueInvokeType());
                        insertParamValueVoList.add(valueVo);
                    }
                    if (CollectionUtils.isNotEmpty(insertParamValueVoList)) {
                        //保存profile参数引用全局参数的关系
                        autoexecProfileMapper.insertProfileParamValueInvokeList(insertParamValueVoList);
                    }
                }
            }
            Date nowLcd = new Date();
            //保存profile参数
            autoexecProfileMapper.insertAutoexecProfileParamList(profileParamVoList, profileVo.getId(), nowLcd);
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
    public Map<String, List<Object>> getAutoexecProfileParamListByKeyListAndProfileId(List<String> keyList, Long profileId) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(profileId);
        if (profileVo == null) {
            throw new AutoexecProfileIsNotFoundException(profileId);
        }
        if (CollectionUtils.isEmpty(keyList)) {
            return null;
        }
        List<AutoexecProfileParamVo> profileParamList = getLatestParamList(profileVo);
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

            if (StringUtils.equals(paramVo.getValueInvokeType(), AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue())) {
                //获取引用的全局参数值
                valueList.add(paramVo.getValueInvokeVoList().stream().map(AutoexecProfileParamValueVo::getInvokeValue).collect(Collectors.toList()));
            } else if (StringUtils.equals(paramVo.getValueInvokeType(), AutoexecProfileParamInvokeType.CONSTANT.getValue())) {
                //获取profile的默认值
                valueList.add(paramVo.getDefaultValue());
            }
            returnMap.put(paramVo.getKey(), valueList);
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
    public  List<AutoexecProfileVo> getProfileVoListByIdList(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return null;
        }
        List<AutoexecProfileVo> returnList = autoexecProfileMapper.getProfileInfoListByIdList(idList);
        if (CollectionUtils.isEmpty(returnList)) {
            return null;
        }
        for (AutoexecProfileVo profileVo : returnList) {
            profileVo.setProfileParamVoList(getLatestParamList(profileVo));
        }
        return returnList;
    }

    /**
     * 获取最新的profile参数列表
     *
     * @param profileVo profile
     * @return
     */
    public List<AutoexecProfileParamVo> getLatestParamList(AutoexecProfileVo profileVo) {
        List<AutoexecProfileParamVo> returnList = new ArrayList<>();

        List<AutoexecParamVo> newOperationParamList = autoexecService.getAutoexecOperationParamVoList(profileVo.getAutoexecOperationVoList());
        //根据key（唯一键）去重
        if (CollectionUtils.isEmpty(newOperationParamList)) {
            return returnList;
        }

        //实时的参数信息
        List<AutoexecProfileParamVo> newProfileParamList = new ArrayList<>();
        for (AutoexecParamVo paramVo : newOperationParamList) {
            newProfileParamList.add(new AutoexecProfileParamVo(paramVo));
        }
        Map<String, AutoexecProfileParamVo> newProfileParamMap = newProfileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));

        //旧的参数信息
        List<AutoexecProfileParamVo> oldProfileParamList = autoexecProfileMapper.getProfileParamListByProfileId(profileVo.getId());
        Map<String, AutoexecProfileParamVo> oldProfileParamMap = null;
        if (CollectionUtils.isNotEmpty(oldProfileParamList)) {
            oldProfileParamMap = oldProfileParamList.stream().collect(Collectors.toMap(AutoexecProfileParamVo::getKey, e -> e));
        }

        //根据参数key替换对应的值：
        //新旧参数的key和type都相同时，才会进行替换值和查询参数引用全局参数的关系
        if (MapUtils.isNotEmpty(newProfileParamMap) && MapUtils.isNotEmpty(oldProfileParamMap)) {
            for (String newParamKey : newProfileParamMap.keySet()) {

                AutoexecProfileParamVo newParamVo = newProfileParamMap.get(newParamKey);
                AutoexecProfileParamVo oldParamVo = oldProfileParamMap.get(newParamKey);

                if (oldParamVo != null && StringUtils.equals(oldParamVo.getType(), newParamVo.getType())) {
                    if (StringUtils.equals(AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue(), oldParamVo.getValueInvokeType())) {
                        newParamVo.setValueInvokeVoList(oldParamVo.getValueInvokeVoList());
                    } else if (StringUtils.equals(AutoexecProfileParamInvokeType.CONSTANT.getValue(), oldParamVo.getValueInvokeType())) {
                        newParamVo.setDefaultValue(oldParamVo.getDefaultValue());
                    }
                    newParamVo.setValueInvokeType(oldParamVo.getValueInvokeType());
                } else {
                    newParamVo.setValueInvokeType(AutoexecProfileParamInvokeType.CONSTANT.getValue());
                }
            }
        }
        for (String key : newProfileParamMap.keySet()) {
            returnList.add(newProfileParamMap.get(key));
        }

        return returnList;
    }
}
