package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dao.mapper.AutoexecProfileMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileToolVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
@Service
public class AutoexecProfileServiceImpl implements AutoexecProfileService{

    @Resource
    AutoexecProfileMapper autoexecProfileMapper;

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;
    /**
     * 获取profile参数
     *
     * @param id
     * @return
     */
    @Override
    public List<AutoexecParamVo> getProfileParamById(Long id) {
        AutoexecProfileVo profileVo = autoexecProfileMapper.getProfileVoById(id);
        JSONObject config = profileVo.getConfig();

        //获取关联的工具
        List<AutoexecProfileToolVo> profileToolVoList = autoexecProfileMapper.getProfileToolListByProfileId(id);
        List<Long> toolIdList = null;
        if (CollectionUtils.isNotEmpty(profileToolVoList)) {
            toolIdList = profileToolVoList.stream().map(AutoexecProfileToolVo::getToolId).collect(Collectors.toList());
        }

        //获取关联的脚本
        List<AutoexecProfileToolVo> profileScriptVoList = autoexecProfileMapper.getProfileScriptListByProfileId(id);
        List<Long> scriptIdList = null;
        if (CollectionUtils.isNotEmpty(profileScriptVoList)) {
            scriptIdList = profileScriptVoList.stream().map(AutoexecProfileToolVo::getToolId).collect(Collectors.toList());
        }
        profileVo.setInputParamList(getProfileConfig(toolIdList, scriptIdList, config));
        return null;
    }

    @Override
    public List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList, JSONObject config) {
        List<AutoexecToolAndScriptVo> allAutoexecToolAndScriptVo = new ArrayList<>();
        allAutoexecToolAndScriptVo.addAll(autoexecToolMapper.getToolListByIdList(toolIdList));
        autoexecScriptMapper.getScriptListByIdList(scriptIdList);



        return null;
    }

}
