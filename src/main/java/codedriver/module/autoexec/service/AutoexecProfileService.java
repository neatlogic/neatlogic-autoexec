package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;

import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
public interface AutoexecProfileService {


    /**
     * 根据profileId获取profile参数
     *
     * @param id id
     * @return profile参数列表
     */
    List<AutoexecProfileParamVo> getProfileParamListById(Long id);

    /**
     * 保存profile和tool、script的关系
     * 在删除profile时会删除此关系，在删除script的时候也会删除此关系
     *
     * @param profileId               profile id
     * @param autoexecOperationVoList 自动化工具列表
     */
    void saveProfileOperation(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList);

    /**
     * 保存profile、profile参数、profile参数值引用全局参数的关系、profile和tool、script的关系
     *
     * @param profileVo profile
     */
    void saveProfile(AutoexecProfileVo profileVo);

    /**
     * 通过id 删除 profile
     *
     * @param id id
     */
    void deleteProfileById(Long id);

    /**
     * 通过key列表和profileId获取对应的值列表
     *
     * @param keyList   key列表
     * @param profileId profile id
     * @return profile的key、value的map
     */
    Map<String, List<Object>> getAutoexecProfileParamListByKeyListAndProfileId(List<String> keyList, Long profileId);


    /**
     * 批量根据profileId列表获取对应的profile列表
     *
     * @param idList profile id列表
     * @return profile列表
     */
     List<AutoexecProfileVo> getProfileVoListByIdList(List<Long> idList);
}
