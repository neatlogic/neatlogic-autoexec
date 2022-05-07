package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
public interface AutoexecProfileService {


    /**
     * 根据profileId获取profile参数
     *
     * @param id
     * @return
     */
    List<AutoexecParamVo> getProfileParamById(Long id);

    /**
     * 保存profile和tool、script的关系
     * 在删除profile时会删除此关系，在删除script的时候也会删除此关系
     *
     * @param profileId               profile id
     * @param autoexecOperationVoList 自动化工具list
     */
    void saveProfileOperation(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList);

    /**
     * 保存profile、profile参数、profile参数引用全局参数的关系
     *
     * @param profileVo profile
     */
    void insertProfile(AutoexecProfileVo profileVo);
}
